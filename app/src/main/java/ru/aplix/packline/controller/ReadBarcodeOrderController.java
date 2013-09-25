package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.ReadBarcodeOrderAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReadBarcodeOrderController extends StandardController<ReadBarcodeOrderAction> implements BarcodeListener {

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	public ReadBarcodeOrderController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeChecker.playFromStart();
		}

		final String barcode = (String) getContext().getAttribute(Const.JUST_SCANNED_BARCODE);
		if (barcode != null) {
			getContext().setAttribute(Const.JUST_SCANNED_BARCODE, null);
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					processBarcode(barcode);
				}
			});
		}
	}

	@Override
	public void terminate() {
		if (barcodeScanner != null) {
			barcodeChecker.stop();
			barcodeScanner.removeBarcodeListener(this);
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

	private void processBarcode(String value) {
		Order order = getAction().processBarcode(value);
		if (order != null) {
			getContext().setAttribute(Const.ORDER, order);

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
				if ((barcodeScanner != null) && barcodeScanner.isConnected()) {
					errorMessageProperty.set(null);
					errorVisibleProperty.set(false);
				} else {
					if (errorStr == null) {
						errorStr = ReadBarcodeOrderController.this.getResources().getString("error.barcode.scanner");
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
}
