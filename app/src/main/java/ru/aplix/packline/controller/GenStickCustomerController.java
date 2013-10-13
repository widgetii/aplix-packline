package ru.aplix.packline.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.GenStickCustomerAction;
import ru.aplix.packline.conf.Configuration;
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
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Task<?> task;

	public GenStickCustomerController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		countGroup = new PersistentButtonToggleGroup();
		countButton1.setToggleGroup(countGroup);
		countButton2.setToggleGroup(countGroup);
		countButton3.setToggleGroup(countGroup);
		countButton4.setToggleGroup(countGroup);
		countGroup.selectToggle(countButton2);

		setCount(countButton1, 1);
		setCount(countButton2, 2);
		setCount(countButton3, 3);
		setCount(countButton4, 4);
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		customerCode = null;
		customerInfoLabel.setText(getResources().getString("sticking.no.customer.selected"));

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeChecker.playFromStart();
		}
	}

	@Override
	public void terminate() {
		if (barcodeScanner != null) {
			barcodeChecker.stop();
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

	private void setCount(ToggleButton toggleButton, int index) {
		try {
			List<Integer> quantity = Configuration.getInstance().getStickersQuantity();
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
		final Integer count = (Integer) countGroup.getSelectedToggle().getUserData();

		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().generateAndPrint(count != null ? count : 0, customerCode);
				} catch (Exception e) {
					LOG.error(e);
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

				barcodeCheckerEventHandler.reset();

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

	public void completeClick(ActionEvent event) {
		done();
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<Customer>() {
			@Override
			public Customer call() throws Exception {
				try {
					Customer customer = getAction().processBarcode(value);
					return customer;
				} catch (Exception e) {
					LOG.error(e);
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

				barcodeCheckerEventHandler.reset();

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
					barcodeCheckerEventHandler.reset();

					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	/**
	 *
	 */
	private class BarcodeCheckerEventHandler implements EventHandler<ActionEvent> {

		private int delayCount;
		private String errorStr;

		public BarcodeCheckerEventHandler() {
			reset();
		}

		@Override
		public void handle(ActionEvent event) {
			if (delayCount <= 1) {
				if ((barcodeScanner != null) && barcodeScanner.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = GenStickCustomerController.this.getResources().getString("error.barcode.scanner");
					}

					errorMessageProperty.set(errorStr);
					errorVisibleProperty.set(true);
				}
			} else {
				delayCount--;
			}
		}

		public void reset() {
			delayCount = Const.ERROR_DISPLAY_DELAY;
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
