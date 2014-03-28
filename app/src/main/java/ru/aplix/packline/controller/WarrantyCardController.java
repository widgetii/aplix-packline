package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.WarrantyCardAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.workflow.WorkflowContext;

public class WarrantyCardController extends StandardController<WarrantyCardAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	enum OperationState {
		WAITING_ORDER_BARCODE, WAITING_BARCODE, WCARD_FILLING
	}

	@FXML
	private Label infoLabel;
	@FXML
	private Parent orderBarcodeContainer;
	@FXML
	private Parent barcodeContainer;
	@FXML
	private Parent wcardContainer;
	@FXML
	private Button withoutBarcodeBtn;
	@FXML
	private Button wcardFilledBtn;
	@FXML
	private Button wcardAbsentBtn;

	private Tag tag;
	private String barcode;
	private OperationState operationState;
	private BarcodeScanner<?> barcodeScanner = null;
	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		tag = null;
		barcode = null;
		setOperationState(OperationState.WAITING_ORDER_BARCODE);
		setProgress(false);

		// Initialize bar-code scanner
		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		if (task != null) {
			task.cancel(false);
		}
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				processBarcode(value);
			}
		});
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		switch (operationState) {
		case WAITING_ORDER_BARCODE:
			findIncoming(value);
			break;
		case WAITING_BARCODE:
			if (tag.getId().equals(value)) {
				tag = null;
				barcode = null;
				setOperationState(OperationState.WAITING_ORDER_BARCODE);
			} else {
				barcode = value;
				startFillingWarrantyCard();
			}
			break;
		case WCARD_FILLING:
			break;
		}
	}

	public void nobarcodeClick(ActionEvent event) {
		processBarcode(null);
	}

	public void wcardFilledClick(ActionEvent event) {
		stopFillingWarrantyCard(true);
	}

	public void wcardAbsentClick(ActionEvent event) {
		stopFillingWarrantyCard(false);
	}

	private void findIncoming(final String value) {
		task = new Task<Tag>() {
			@Override
			public Tag call() throws Exception {
				try {
					return getAction().findTag(value);
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				setProgress(true);
			}

			@Override
			protected void failed() {
				super.failed();

				setProgress(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				Tag result = getValue();
				if (result != null) {
					tag = result;
					setOperationState(OperationState.WAITING_BARCODE);
				} else {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void startFillingWarrantyCard() {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().startFillingWarrantyCard(tag, barcode);
					return null;
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				setProgress(true);
			}

			@Override
			protected void failed() {
				super.failed();

				setProgress(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				setOperationState(OperationState.WCARD_FILLING);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void stopFillingWarrantyCard(final boolean filled) {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().stopFillingWarrantyCard(tag, barcode, filled);
					return null;
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				setProgress(true);
			}

			@Override
			protected void failed() {
				super.failed();

				setProgress(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				setOperationState(OperationState.WAITING_BARCODE);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		withoutBarcodeBtn.setDisable(value);
		wcardFilledBtn.setDisable(value);
		wcardAbsentBtn.setDisable(value);
	}

	private void setOperationState(OperationState newOperationState) {
		if (operationState == null || !operationState.equals(newOperationState)) {
			operationState = newOperationState;

			switch (operationState) {
			case WAITING_ORDER_BARCODE:
				orderBarcodeContainer.setVisible(true);
				barcodeContainer.setVisible(false);
				wcardContainer.setVisible(false);
				infoLabel.setText(getResources().getString("warranty.card.info1"));
				break;
			case WAITING_BARCODE:
				orderBarcodeContainer.setVisible(false);
				barcodeContainer.setVisible(true);
				wcardContainer.setVisible(false);
				infoLabel.setText(String.format(getResources().getString("warranty.card.info2"), tag.getId()));
				break;
			case WCARD_FILLING:
				orderBarcodeContainer.setVisible(false);
				barcodeContainer.setVisible(false);
				wcardContainer.setVisible(true);
				infoLabel.setText(String.format(getResources().getString("warranty.card.info3"), barcode != null && barcode.length() > 0 ? barcode
						: getResources().getString("nobarcode")));
				break;
			}
		}
	}

	@Override
	protected boolean checkNoError() {
		if ((barcodeScanner == null) || barcodeScanner.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
