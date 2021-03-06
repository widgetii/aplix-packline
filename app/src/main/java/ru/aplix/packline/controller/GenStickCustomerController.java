package ru.aplix.packline.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.GenStickCustomerAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Customer;
import ru.aplix.packline.workflow.WorkflowContext;

public class GenStickCustomerController extends StandardController<GenStickCustomerAction> implements BarcodeListener {

	@FXML
	public Button buttonComplete;
	@FXML
	public Button buttonGenerate;
	@FXML
	public ToggleButton countButton1;
	@FXML
	public ToggleButton countButton2;
	@FXML
	public ToggleButton countButton3;
	@FXML
	public ToggleButton countButton4;
	@FXML
	public Label customerInfoLabel;

	private ToggleGroup countGroup;
	private String customerCode = null;

	private final Log LOG = LogFactory.getLog(getClass());

	private BarcodeScanner<?> barcodeScanner = null;

	private ConfirmationDialog confirmationDialog = null;
	private Task<?> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		countGroup = new PersistentButtonToggleGroup();
		countButton1.setToggleGroup(countGroup);
		countButton2.setToggleGroup(countGroup);
		countButton3.setToggleGroup(countGroup);
		countButton4.setToggleGroup(countGroup);
		countGroup.selectToggle(countButton2);
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		setCount(countButton1, 1);
		setCount(countButton2, 2);
		setCount(countButton3, 3);
		setCount(countButton4, 4);
		countButton2.setSelected(true);

		setProgress(false);

		customerCode = null;
		customerInfoLabel.setText(getResources().getString("sticking.no.customer.selected"));

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
				if (value == null || value.length() == 0 || progressVisibleProperty.get() || confirmationDialog != null) {
					return;
				}

				String barcode;
				try {
					String[] args = value.split("#");
					if (args.length == 2) {
						barcode = args[0];
						Integer count = Integer.valueOf(args[1]);

						countButton4.setText(count.toString());
						countButton4.setUserData(count);
						countButton4.setSelected(true);
					} else {
						barcode = value;
					}
				} catch (Exception e) {
					return;
				}

				processBarcode(barcode);
			}
		});
	}

	private void setCount(ToggleButton toggleButton, int index) {
		try {
			List<Integer> quantity = Configuration.getInstance().getStickers().getForCustomers().getQuantity();
			Integer value = quantity.get(index - 1);

			toggleButton.setText(value.toString());
			toggleButton.setUserData(value);
		} catch (Exception e) {
			toggleButton.setText("-");
			toggleButton.setUserData(null);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		buttonComplete.setDisable(value);
		buttonGenerate.setDisable(value);
		countButton1.setDisable(value);
		countButton2.setDisable(value);
		countButton3.setDisable(value);
		countButton4.setDisable(value);
	}

	public void generateClick(ActionEvent event) {
		generateAndPrintStickers(-1);
	}

	public void completeClick(ActionEvent event) {
		done();
	}

	private void generateAndPrintStickers(final int offset) {
		final Integer count = (Integer) countGroup.getSelectedToggle().getUserData();

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().generateAndPrint(offset, count != null ? count : 0, customerCode);
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;
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

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void processBarcode(final String value) {
		task = new Task<Customer>() {

			private int lastPrintedTagIndex = -1;

			@Override
			public Customer call() throws Exception {
				try {
					lastPrintedTagIndex = getAction().findPrintedTag(value);
					if (lastPrintedTagIndex > -1) {
						cancel();
						return null;
					}

					Customer customer = getAction().processBarcode(value);
					return customer;
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

				Customer result = getValue();
				if (result != null) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);

					customerCode = result.getId();
					customerInfoLabel.setText(String.format(getResources().getString("customer.info"), result.getId(), result.getName()));
				} else {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}

			@Override
			protected void cancelled() {
				super.cancelled();

				setProgress(false);
				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				if (lastPrintedTagIndex > -1) {
					continuePrinting(value, lastPrintedTagIndex);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void continuePrinting(String lastPrintedTagId, final int lastPrintedTagIndex) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.confirm", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				generateAndPrintStickers(lastPrintedTagIndex);
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.stickers.continue", lastPrintedTagId);
		confirmationDialog.show();
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

	/**
	 *
	 */
	private class PersistentButtonToggleGroup extends ToggleGroup {

		public PersistentButtonToggleGroup() {
			super();
			getToggles().addListener(new ListChangeListener<Toggle>() {
				@Override
				public void onChanged(Change<? extends Toggle> c) {
					while (c.next())
						for (final Toggle addedToggle : c.getAddedSubList())
							((ToggleButton) addedToggle).addEventFilter(MouseEvent.MOUSE_RELEASED, new EventHandler<MouseEvent>() {
								@Override
								public void handle(MouseEvent mouseEvent) {
									if (addedToggle.equals(getSelectedToggle()))
										mouseEvent.consume();
								}
							});
				}
			});
		}
	}
}
