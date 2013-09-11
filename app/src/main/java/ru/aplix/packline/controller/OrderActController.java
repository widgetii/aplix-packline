package ru.aplix.packline.controller;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import ru.aplix.packline.Const;
import ru.aplix.packline.action.OrderActAction;
import ru.aplix.packline.dialog.ConfirmationDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.workflow.WorkflowContext;

public class OrderActController extends StandardController<OrderActAction> implements BarcodeListener {

	// @formatter:off
    final ObservableList<Order> data = FXCollections.observableArrayList(
            new Order(1L, "123456789", new Date(), 1.5f),
            new Order(2L, "23424565", new Date(), 4.5f),
            new Order(3L, "346456", new Date(), 2.3f),
            new Order(4L, "07890879087", new Date(), 8.7f),
            new Order(5L, "3465678", new Date(), 9.1f),
            new Order(6L, "324535345", new Date(), 2.4f),
            new Order(7L, "45664356", new Date(), 5.4f),
            new Order(8L, "56786786", new Date(), 8.7f),
            new Order(9L, "879079808", new Date(), 1.0f),
            new Order(10L, "046264564564", new Date(), 0.6f),
            new Order(11L, "123456789", new Date(), 1.5f),
            new Order(12L, "23424565", new Date(), 4.5f),
            new Order(13L, "346456", new Date(), 2.3f),
            new Order(14L, "07890879087", new Date(), 8.7f),
            new Order(15L, "3465678", new Date(), 9.1f),
            new Order(16L, "324535345", new Date(), 2.4f),
            new Order(17L, "45664356", new Date(), 5.4f),
            new Order(18L, "56786786", new Date(), 8.7f),
            new Order(19L, "879079808", new Date(), 1.0f),
            new Order(20L, "046264564564", new Date(), 0.6f));
    // @formatter:on

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView ordersTableView;
	@FXML
	private Label actInfoLabel;
	@FXML
	private Label totalOrdersLabel;
	@FXML
	private AnchorPane actInfoContainer;

	private DateFormat dateFormat;
	private DateFormat timeFormat;

	private BarcodeScanner<?> barcodeScanner;
	private Timeline barcodeChecker;
	private BarcodeCheckerEventHandler barcodeCheckerEventHandler;

	public OrderActController() {
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

		ordersTableView.setItems(data);

		Date now = new Date();
		actInfoLabel.setText(String.format(getResources().getString("act.info"), "123456788", dateFormat.format(now), timeFormat.format(now),
				"\"ЮниЭкспресс\"\nг.Москва, Озерковская набережная, д.48-50, стр.2, подъезд 7А"));

		totalOrdersLabel.setText(String.format(getResources().getString("act.orders.total"), 50));

		errorMessageProperty.set(null);
		errorVisibleProperty.set(false);

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		barcodeScanner.addBarcodeListener(this);

		barcodeChecker.playFromStart();
	}

	@SuppressWarnings("unchecked")
	private void initTable() {
		TableColumn<Order, Long> firstColumn = new TableColumn<Order, Long>(getResources().getString("act.order.number"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(false);
		firstColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.10));
		firstColumn.setCellValueFactory(new Callback<CellDataFeatures<Order, Long>, ObservableValue<Long>>() {
			@Override
			public ObservableValue<Long> call(CellDataFeatures<Order, Long> p) {
				return new ReadOnlyObjectWrapper<Long>(p.getValue().getId());
			}
		});

		TableColumn<Order, String> secondColumn = new TableColumn<Order, String>(getResources().getString("act.order.description"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.38));
		secondColumn.setCellValueFactory(new Callback<CellDataFeatures<Order, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Order, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getCode());
			}
		});

		TableColumn<Order, String> thirdColumn = new TableColumn<Order, String>(getResources().getString("act.order.datetime"));
		thirdColumn.setSortable(false);
		thirdColumn.setResizable(false);
		thirdColumn.setEditable(false);
		thirdColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.20));
		thirdColumn.setCellValueFactory(new Callback<CellDataFeatures<Order, String>, ObservableValue<String>>() {
			@Override
			public ObservableValue<String> call(CellDataFeatures<Order, String> p) {
				Date d = p.getValue().getDate();
				String s = String.format("%s\n%s", dateFormat.format(d), timeFormat.format(d));
				return new ReadOnlyObjectWrapper<String>(s);
			}
		});

		TableColumn<Order, String> fourthColumn = new TableColumn<Order, String>(getResources().getString("act.order.weight"));
		fourthColumn.setSortable(false);
		fourthColumn.setResizable(false);
		fourthColumn.setEditable(false);
		fourthColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.15));
		fourthColumn.setCellValueFactory(new Callback<CellDataFeatures<Order, String>, ObservableValue<String>>() {
			public ObservableValue<String> call(CellDataFeatures<Order, String> p) {
				return new ReadOnlyObjectWrapper<String>(p.getValue().getWeight().toString());
			}
		});

		TableColumn<Order, Order> fifthColumn = new TableColumn<Order, Order>(getResources().getString("act.order.delete"));
		fifthColumn.setSortable(false);
		fifthColumn.setResizable(false);
		fifthColumn.setEditable(false);
		fifthColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.10));
		fifthColumn.setCellFactory(new Callback<TableColumn<Order, Order>, TableCell<Order, Order>>() {
			public TableCell<Order, Order> call(TableColumn<Order, Order> p) {
				return new ButtonTableCell();
			}
		});
		fifthColumn.setCellValueFactory(new Callback<CellDataFeatures<Order, Order>, ObservableValue<Order>>() {
			@Override
			public ObservableValue<Order> call(CellDataFeatures<Order, Order> p) {
				return new ReadOnlyObjectWrapper<Order>(p.getValue());
			}
		});

		ordersTableView.getColumns().add(fifthColumn);
		ordersTableView.getColumns().add(firstColumn);
		ordersTableView.getColumns().add(secondColumn);
		ordersTableView.getColumns().add(thirdColumn);
		ordersTableView.getColumns().add(fourthColumn);

		ordersTableView.setPlaceholder(new Text(getResources().getString("act.noorders")));

		actInfoContainer.prefWidthProperty().bind(((AnchorPane) rootNode).widthProperty().multiply(0.25));
	}

	@Override
	public void terminate() {
		barcodeChecker.stop();
		barcodeScanner.removeBarcodeListener(this);
	}

	public void closeActClick(ActionEvent event) {
		getAction().closeAct();
		done();
	}

	public void saveActClick(ActionEvent event) {
		getAction().saveAct();
		done();
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
		boolean result = getAction().processBarcode(value);
		if (result) {
			getContext().setAttribute(Const.JUST_SCANNED_BARCODE, value);

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
			delayCount = 5;
		}
	}

	/**
	 *
	 */
	private class ButtonTableCell extends TableCell<Order, Order> {

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
					ObservableValue<Order> ov = getTableColumn().getCellObservableValue(getIndex());
					final Order order = ov.getValue();

					Date orderDate = order.getDate();
					String orderDateStr = String.format("%s %s", dateFormat.format(orderDate), timeFormat.format(orderDate));

					Window owner = rootNode.getScene().getWindow();
					ConfirmationDialog cd = new ConfirmationDialog(owner, "act.order.delete", null, new ConfirmationListener() {

						@Override
						public void onAccept() {
							getTableView().getItems().remove(order);
						}

						@Override
						public void onDecline() {
						}
					});
					cd.centerOnScreen();
					cd.setMessage("confirmation.act.order.delete", order.getId(), order.getCode(), orderDateStr);
					cd.show();
				}
			});

			setAlignment(Pos.CENTER);
			setGraphic(button);
		}

		@Override
		public void updateItem(Order item, boolean empty) {
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
