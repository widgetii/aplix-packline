package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.PackTypeAction;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.workflow.WorkflowContext;

public class PackTypeController extends StandardController<PackTypeAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private Task<Boolean> task;

	public PackTypeController() {
		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Order order = (Order) context.getAttribute(Const.ORDER);
		if (order != null) {
			clientLabel.setText(order.getClientName());
			deliveryLabel.setText(order.getDeliveryMethod());
			customerLabel.setText(order.getCustomerName());
		}

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

	public void boxClick(ActionEvent event) {
		selectPacketType(PackingType.BOX);
	}

	public void packetClick(ActionEvent event) {
		selectPacketType(PackingType.PACKET);
	}

	public void paperClick(ActionEvent event) {
		selectPacketType(PackingType.PAPER);
	}

	private void selectPacketType(PackingType value) {
		try {
			getAction().selectPacketType(value);

			done();
		} catch (DatatypeConfigurationException e) {
			LOG.error(null, e);
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

				progressVisibleProperty.set(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);

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

				progressVisibleProperty.set(false);

				if (this.getValue() != null) {
					PackTypeController.this.done();
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
						errorStr = PackTypeController.this.getResources().getString("error.barcode.scanner");
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
