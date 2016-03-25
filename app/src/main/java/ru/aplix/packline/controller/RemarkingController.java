package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.RemarkingAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.workflow.WorkflowContext;

public class RemarkingController extends StandardController<RemarkingAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private Button completeButton;

	private BarcodeScanner<?> barcodeScanner = null;

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
		getAction().prepare();

		infoLabel.setText(getResources().getString("remarking.info"));

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		completeButton.setVisible(!value);

		if (value) {
			infoLabel.setText(getResources().getString("printing.info1"));
		} else {
			infoLabel.setText(getResources().getString("remarking.info"));
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

		task = new Task<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					return getAction().processBarcode(value);
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

				if (!getValue()) {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
				else
				if (getAction().attachDocuments) {
					showWarningMessage(getResources().getString("printing.info4"), true);
				}

			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
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

	public void completeClick(ActionEvent event) {
		done();
	}
}
