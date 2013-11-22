package ru.aplix.packline.controller;

import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Callback;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.OrderActAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.workflow.WorkflowContext;

public class OrderActController extends StandardController<OrderActAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView ordersTableView;
	@FXML
	private Label actInfoLabel;
	@FXML
	private Label totalOrdersLabel;
	@FXML
	private AnchorPane actInfoContainer;
	@FXML
	private Button closeActButton;
	@FXML
	private Button saveActButton;
	@FXML
	private Button deleteActButton;

	private DateFormat dateFormat;
	private DateFormat timeFormat;
	private Registry registry;

	private BarcodeScanner<?> barcodeScanner = null;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	private ConfirmationDialog confirmationDialog = null;
	private List<Task<?>> tasks;

	public OrderActController() {
		tasks = new ArrayList<Task<?>>();

		dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
		timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

		barcodeCheckerEventHandler = new BarcodeCheckerEventHandler();

		barcodeChecker = new Timeline();
		barcodeChecker.setCycleCount(Timeline.INDEFINITE);
		barcodeChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), barcodeCheckerEventHandler));
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		initTable();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		registry = (Registry) context.getAttribute(Const.REGISTRY);
		if (registry != null) {
			ObservableList<Incoming> data = FXCollections.observableArrayList(registry.getIncoming());
			ordersTableView.setItems(data);

			Date date = registry.getDate() != null ? registry.getDate().toGregorianCalendar().getTime() : null;
			actInfoLabel.setText(String.format(getResources().getString("act.info"), registry.getId(), date != null ? dateFormat.format(date) : "",
					date != null ? timeFormat.format(date) : "", registry.getCustomer().getName()));

			totalOrdersLabel.setText(String.format(getResources().getString("act.incomings.total"), ordersTableView.getItems().size(),
					registry.getTotalIncomings()));
		}

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
			barcodeChecker.playFromStart();
		}
	}

	@SuppressWarnings("unchecked")
	private void initTable() {
		TableColumn<Incoming, String> firstColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.number"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(false);
		firstColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.20));
		firstColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getId());
			}
		});

		TableColumn<Incoming, String> secondColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.description"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.38));
		secondColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getContentDescription());
			}
		});

		TableColumn<Incoming, String> thirdColumn = new TableColumn<Incoming, String>(getResources().getString("act.incoming.datetime"));
		thirdColumn.setSortable(false);
		thirdColumn.setResizable(false);
		thirdColumn.setEditable(false);
		thirdColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.15));
		thirdColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Incoming, String> p) {
				String s = "";
				if (p.getValue().getDate() != null) {
					Date d = p.getValue().getDate().toGregorianCalendar().getTime();
					s = String.format("%s\n%s", dateFormat.format(d), timeFormat.format(d));
				}
				return new ReadOnlyObjectWrapper<String>(s);
			}
		});

		TableColumn<Incoming, Float> fourthColumn = new TableColumn<Incoming, Float>(getResources().getString("act.incoming.weight"));
		fourthColumn.setSortable(false);
		fourthColumn.setResizable(false);
		fourthColumn.setEditable(false);
		fourthColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.10));
		fourthColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, Float>, ObservableValue<Float>>() {
			public ObservableValue<Float> call(CellDataFeatures<Incoming, Float> p) {
				return new ReadOnlyObjectWrapper<Float>(p.getValue().getWeight());
			}
		});

		TableColumn<Incoming, Incoming> fifthColumn = new TableColumn<Incoming, Incoming>(getResources().getString("act.incoming.delete"));
		fifthColumn.setSortable(false);
		fifthColumn.setResizable(false);
		fifthColumn.setEditable(false);
		fifthColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.10));
		fifthColumn.setCellFactory(new Callback<TableColumn<Incoming, Incoming>, TableCell<Incoming, Incoming>>() {
			public TableCell<Incoming, Incoming> call(TableColumn<Incoming, Incoming> p) {
				return new ButtonTableCell();
			}
		});
		fifthColumn.setCellValueFactory(new Callback<CellDataFeatures<Incoming, Incoming>, ObservableValue<Incoming>>() {
			@Override
			public ObservableValue<Incoming> call(CellDataFeatures<Incoming, Incoming> p) {
				return new ReadOnlyObjectWrapper<Incoming>(p.getValue());
			}
		});

		ordersTableView.getColumns().add(fifthColumn);
		ordersTableView.getColumns().add(firstColumn);
		ordersTableView.getColumns().add(secondColumn);
		ordersTableView.getColumns().add(thirdColumn);
		ordersTableView.getColumns().add(fourthColumn);

		ordersTableView.setPlaceholder(new Text(getResources().getString("act.noincomings")));

		actInfoContainer.prefWidthProperty().bind(((AnchorPane) rootNode).widthProperty().multiply(0.25));
	}

	@Override
	public void terminate() {
		if (barcodeScanner != null) {
			barcodeChecker.stop();
			barcodeScanner.removeBarcodeListener(this);
		}

		for (Task<?> task : tasks) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		closeActButton.setDisable(value);
		saveActButton.setDisable(value);
		deleteActButton.setDisable(value);
	}

	public void closeActClick(ActionEvent event) {
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
		if (registry.getDate() != null) {
			Date orderDate = registry.getDate().toGregorianCalendar().getTime();
			orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
		}

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.act.carryOutAndClose", registry.getId(), orderDateStr);
		confirmationDialog.show();
	}

	private void doCloseAct() {

		Task<?> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().carryOutRegistry();
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

				OrderActController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
	}

	public void saveActClick(ActionEvent event) {
		done();
	}

	private void doDeleteAct() {
		Task<?> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().deleteRegistry();
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

				OrderActController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
	}

	public void deleteActClick(ActionEvent event) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				doDeleteAct();
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		String orderDateStr = "";
		if (registry.getDate() != null) {
			Date orderDate = registry.getDate().toGregorianCalendar().getTime();
			orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
		}

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage("confirmation.act.delete", registry.getId(), orderDateStr);
		confirmationDialog.show();
	}

	private void askDeleteIncoming(final Incoming incoming, String message, Object... params) {
		Window owner = rootNode.getScene().getWindow();
		confirmationDialog = new ConfirmationDialog(owner, "dialog.delete", null, new ConfirmationListener() {

			@Override
			public void onAccept() {
				confirmationDialog = null;
				deleteIncoming(incoming);
			}

			@Override
			public void onDecline() {
				confirmationDialog = null;
			}
		});

		confirmationDialog.centerOnScreen();
		confirmationDialog.setMessage(message, params);
		confirmationDialog.show();
	}

	public void deleteIncoming(final Incoming incoming) {
		Task<?> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().deleteIncoming(incoming);
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

				ordersTableView.getItems().remove(incoming);
				totalOrdersLabel.setText(String.format(getResources().getString("act.incomings.total"), ordersTableView.getItems().size(),
						registry.getTotalIncomings()));
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
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

		Task<?> task = new Task<Boolean>() {
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

				Boolean result = getValue();
				if (result != null && result.booleanValue()) {
					getContext().setAttribute(Const.JUST_SCANNED_BARCODE, value);

					OrderActController.this.done();
				} else {
					barcodeCheckerEventHandler.reset();

					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
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
						errorStr = OrderActController.this.getResources().getString("error.barcode.scanner");
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
	private class ButtonTableCell extends TableCell<Incoming, Incoming> {

		private final Button button;

		public ButtonTableCell() {
			this.button = new Button();
			this.button.setText(null);
			this.button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			this.button.setPrefWidth(70d);
			this.button.setPrefHeight(70d);
			this.button.setId("deleteButton");
			this.button.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (progressVisibleProperty.get()) {
						return;
					}

					ObservableValue<Incoming> ov = getTableColumn().getCellObservableValue(getIndex());
					final Incoming incoming = ov.getValue();

					String orderDateStr = "";
					if (incoming.getDate() != null) {
						Date orderDate = incoming.getDate().toGregorianCalendar().getTime();
						orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));
					}

					askDeleteIncoming(incoming, "confirmation.act.incoming.delete", incoming.getId(), incoming.getContentDescription(), orderDateStr);
				}
			});

			setAlignment(Pos.CENTER);
			setGraphic(button);
		}

		@Override
		public void updateItem(Incoming item, boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				setGraphic(button);
			}
		}
	}
}
