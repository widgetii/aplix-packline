package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.AuthAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.workflow.WorkflowContext;

public class AuthController extends StandardController<AuthAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private ResourceBundle resources;

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Task<Operator> task;

	public AuthController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		this.resources = resources;
	}

	@Override
	public void prepare(WorkflowContext context) {
		context.setAttribute(Const.OPERATOR, null);
		super.prepare(context);
		
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
				authenticateOperator(value);
			}
		});
	}

	private void authenticateOperator(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<Operator>() {
			@Override
			public Operator call() throws Exception {
				try {
					Operator operator = getAction().authenticateOperator(value);
					return operator;
				} catch (Exception e) {
					LOG.error(e);
					throw e;
				}
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);

				barcodeCheckerEventHandler.reset();

				errorMessageProperty.set(resources.getString("error.post.service"));
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);

				Object result = getValue();
				if (result != null) {
					AuthController.this.done();
				} else {
					barcodeCheckerEventHandler.reset();

					errorMessageProperty.set(getResources().getString("error.auth.invalid.code"));
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
			delayCount = Const.ERROR_DISPLAY_DELAY;
		}
	}
}
