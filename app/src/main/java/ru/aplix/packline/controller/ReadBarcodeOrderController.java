package ru.aplix.packline.controller;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.stage.Window;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ReadBarcodeOrderAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.RouteList;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReadBarcodeOrderController extends StandardController<ReadBarcodeOrderAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label routeListInfoLabel;
	@FXML
	private Label totalRegistriesLabel;
	@FXML
	private Pane routeListContainer;
	@FXML
	private Button closeRouteListButton;
	@FXML
	private Button saveRouteListButton;

	private BarcodeScanner<?> barcodeScanner = null;

	private ConfirmationDialog confirmationDialog = null;
	private Task<?> task;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	private RouteList routeList;

	public ReadBarcodeOrderController() {
		super();

		dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		// Display route list info
		routeList = (RouteList) getContext().getAttribute(Const.ROUTE_LIST);
		routeListContainer.setVisible(routeList != null);
		routeListContainer.prefWidthProperty().bind(((AnchorPane) rootNode).widthProperty().multiply(routeListContainer.isVisible() ? 0.25f : 0f));
		if (routeList != null) {
			Date date = routeList.getDate() != null ? routeList.getDate().toGregorianCalendar().getTime() : null;
			routeListInfoLabel.setText(String.format(getResources().getString("routeList.info"), routeList.getId(),
					date != null ? dateFormat.format(date) : "", date != null ? timeFormat.format(date) : "", routeList.getDriverName()));

			int closedCound = 0;
			for (Registry registry : routeList.getRegistry()) {
				if (registry.isCarriedOutAndClosed()) {
					closedCound++;
				}
			}
			totalRegistriesLabel.setText(String.format(getResources().getString("routeList.registries.total"), closedCound, routeList.getRegistry().size()));
		}

		// Initialize bar-code scanner
		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}

		// If bar-code has been read already, then process it
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
		super.terminate();

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
		if (progressVisibleProperty.get() || confirmationDialog != null) {
			return;
		}

		task = new Task<Object>() {
			@Override
			public Object call() throws Exception {
				try {
					Operator operator = getAction().checkOperatorWorkComplete(value);
					if (operator != null) {
						return operator;
					}

					Tag tag = getAction().processBarcode(value);
					return tag;
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

				Object result = getValue();
				if (result != null) {
					ReadBarcodeOrderController.this.done();
				} else {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	public void saveRouteListClick(ActionEvent event) {
		getAction().saveRouteList();
		done();
	}

	public void closeRouteListClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.carryOutAndClose", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				doCloseAct();
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		String orderDateStr = "";
		if (routeList.getDate() != null) {
			Date orderDate = routeList.getDate().toGregorianCalendar().getTime();
			orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
		}

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.routeList.carryOutAndClose", routeList.getId(), orderDateStr);
		confirmationDialog.show();
	}

	private void doCloseAct() {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().carryOutRouteList();
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

				ReadBarcodeOrderController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		closeRouteListButton.setDisable(value);
		saveRouteListButton.setDisable(value);
	}

	@Override
	protected boolean checkNoError() {
		if ((barcodeScanner != null) && barcodeScanner.isConnected()) {
			return true;
		} else {
			errorMessageProperty.set(getResources().getString("error.barcode.scanner"));
			errorVisibleProperty.set(true);

			return false;
		}
	}
}
