package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.ReadBarcodeBoxAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReadBarcodeBoxController extends StandardController<ReadBarcodeBoxAction> implements BarcodeListener {

	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;

	private BarcodeScanner<?> barcodeScanner;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	public ReadBarcodeBoxController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Order order = (Order) context.getAttribute(Const.ORDER);

		clientLabel.setText(order.getClient());
		deliveryLabel.setText(order.getDeliveryMethod());
		customerLabel.setText(order.getCustomer());

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		barcodeScanner.addBarcodeListener(this);

		barcodeChecker.playFromStart();
	}

	@Override
	public void terminate() {
		barcodeChecker.stop();
		barcodeScanner.removeBarcodeListener(this);
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

	private void processBarcode(String value) {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (getAction().processBarcode(value, order)) {
			done();
		} else {
			barcodeCheckerEventHandler.reset();

			errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
			errorVisibleProperty.set(true);
		}
	}

	private class BarcodeCheckerEventHandler implements EventHandler<ActionEvent> {

		private int delayCount;
		private String errorStr;

		public BarcodeCheckerEventHandler() {
			reset();
		}

		@Override
		public void handle(ActionEvent event) {
			if (delayCount <= 1) {
				if (barcodeScanner.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = ReadBarcodeBoxController.this.getResources().getString("error.barcode.scanner");
					}

					errorMessageProperty.set(errorStr);
					errorVisibleProperty.set(true);
				}
			} else {
				delayCount--;
			}
		}

		public void reset() {
			delayCount = 5;
		}
	}
}
