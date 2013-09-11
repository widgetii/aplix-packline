package ru.aplix.packline.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.AuthAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.model.Operator;
import ru.aplix.packline.workflow.WorkflowContext;

public class AuthController extends StandardController<AuthAction> implements BarcodeListener {

	private BarcodeScanner<?> barcodeScanner;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	public AuthController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		context.setAttribute(Const.OPERATOR, null);
		super.prepare(context);

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
				authenticateOperator(value);
			}
		});
	}

	private void authenticateOperator(String value) {
		Operator operator = getAction().authenticateOperator(value);
		if (operator != null) {
			getContext().setAttribute(Const.OPERATOR, operator);

			done();
		} else {
			barcodeCheckerEventHandler.reset();

			errorMessageProperty.set(getResources().getString("error.auth.invalid.code"));
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
						errorStr = AuthController.this.getResources().getString("error.barcode.scanner");
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
