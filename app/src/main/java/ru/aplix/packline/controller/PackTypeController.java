package ru.aplix.packline.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.PackTypeAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.workflow.WorkflowContext;

import javax.xml.datatype.DatatypeConfigurationException;
import java.util.concurrent.ExecutorService;

public class PackTypeController extends StandardController<PackTypeAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());
	@FXML
	public StackPane contentPane;
	@FXML
	public Button boxPackButton;
	@FXML
	public Button packetPackButton;
	@FXML
	public Button paperPackButton;
	@FXML
	public Button roofBoardingPackButton;
	@FXML
	public AnchorPane orderInfoContainer;
	@FXML
	private Label clientLabel;
	@FXML
	private Label deliveryLabel;
	@FXML
	private Label customerLabel;

	private BarcodeScanner<?> barcodeScanner = null;

	private Task<Boolean> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		Order order = (Order) context.getAttribute(Const.ORDER);
		if (order != null) {
			clientLabel.setText(order.getClientName());
			deliveryLabel.setText(order.getDeliveryMethod());
			customerLabel.setText(order.getCustomer().getName());
		}

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

	public void boxClick(ActionEvent event) {
		selectPacketType(PackingType.BOX);
	}

	public void packetClick(ActionEvent event) {
		selectPacketType(PackingType.PACKET);
	}

	public void paperClick(ActionEvent event) {
		selectPacketType(PackingType.PAPER);
	}

	public void roofBoardingClick(ActionEvent event) {
		selectPacketType(PackingType.ROOF_BOARDING);
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
		Platform.runLater(() -> processBarcode(value));
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get()) {
			return;
		}

		task = new Task<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					final Integer emptyBoxCount = getAction().processBarcode(value);
					Integer emptyBoxThreshold = Configuration.getInstance().getEmptyBoxThreshold();
					if ((emptyBoxCount != null) && (emptyBoxThreshold != null) && (Integer.compare(emptyBoxCount, emptyBoxThreshold) < 0)) {
						Platform.runLater(() -> showWarningMessage(String.format(getResources().getString("message.replenish.box"), emptyBoxCount)));

						Thread.sleep(Const.ERROR_DISPLAY_DELAY * DateUtils.MILLIS_PER_SECOND);
					}
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);

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

				PackTypeController.this.done();
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
}
