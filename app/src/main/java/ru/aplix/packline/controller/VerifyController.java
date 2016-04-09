package ru.aplix.packline.controller;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Window;
import javafx.util.Callback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.VerifyAction;
import ru.aplix.packline.dialog.ConfirmationImageDialog;
import ru.aplix.packline.dialog.ConfirmationListener;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.post.*;
import ru.aplix.packline.utils.CacheOrders;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;

public class VerifyController extends StandardController<VerifyAction> implements BarcodeListener {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	public Insets x1;
	@FXML
	public StackPane contentPane;
	@FXML
	public Label clientLabel;
	@FXML
	public Label deliveryLabel;
	@FXML
	public Label customerLabel;
	@FXML
	public Button cancelButton;
	@FXML
	public Button verifyCloseButton;
	@FXML
	private AnchorPane actInfoContainer;

	@SuppressWarnings("rawtypes")
	@FXML
	private TableView ordersTableView;

	private BarcodeScanner<?> barcodeScanner = null;

	private ConfirmationImageDialog confirmationImageDialog = null;

	private List<Task<?>> tasks;

	private ObservableList<Product> dataList;

	private Map<Product, BooleanProperty> confirmedMap;

	public VerifyController() {
		super();

		tasks = new ArrayList<>();
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		initTable();
	}

	private List<Product> getProducts(final String orderId)
	{
		ApplicationContext	applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		CacheOrders cacheOrders = (CacheOrders) applicationContext.getBean(Const.CACHE_ORDERS_BEAN_NAME);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		try {
			Order order = cacheOrders.findOrder(postServicePort, orderId, false, true);
			if (order != null) {
				return order.getProducts();
			}
		} catch (PackLineException e) {
			LOG.error(null, e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		/**
		 * TableView refresh items
		 * https://bugs.openjdk.java.net/browse/JDK-8098085
 		 */
		if (dataList != null) {
			dataList.removeAll(dataList);
			dataList = null;
		}

		setProgress(false);

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order != null) {

			if (order.isVerifyPacking() == null || !order.isVerifyPacking()) {
				getAction().setNextAction(getAction().getVerifyCloseAction());
				throw new SkipActionException();
			}

			clientLabel.setText(getResources().getString("order.info.client") + order.getClientName());
			deliveryLabel.setText(getResources().getString("order.info.delivery") + order.getDeliveryMethod());
			customerLabel.setText(getResources().getString("order.info.customer") + order.getCustomer().getName());

			List<Product> products = getProducts(order.getId());
			if (products != null) {
				dataList = FXCollections.observableArrayList(products);

				confirmedMap = new HashMap<>();
				for (Product product : products) {
					confirmedMap.put(product, new SimpleBooleanProperty(false));
				}
				checkСonfirmation();

				ordersTableView.setItems(dataList);
			}
		}

		barcodeScanner = (BarcodeScanner<?>) context.getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.addBarcodeListener(this);
		}
	}

	@SuppressWarnings("unchecked")
	private void initTable() {
		TableColumn<Product, String> firstColumn = new TableColumn<>(getResources().getString("act.incoming.description"));
		firstColumn.setSortable(false);
		firstColumn.setResizable(false);
		firstColumn.setEditable(false);
		firstColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.45));
		firstColumn.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getDescription()));

		TableColumn<Product, Integer> secondColumn = new TableColumn<>(getResources().getString("activeposts.count"));
		secondColumn.setSortable(false);
		secondColumn.setResizable(false);
		secondColumn.setEditable(false);
		secondColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.10));
		secondColumn.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(p.getValue().getCount()));

		TableColumn<Product, Product> thirdColumn = new TableColumn<>(getResources().getString("pictures"));
		thirdColumn.setSortable(false);
		thirdColumn.setResizable(false);
		thirdColumn.setEditable(false);
		thirdColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.30));
		thirdColumn.setCellValueFactory(item -> new ReadOnlyObjectWrapper<>(item.getValue()));
		thirdColumn.setCellFactory(param -> new TableCell<Product, Product>() {
					@Override
					public void updateItem(Product item, boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							HBox hBox = new HBox();

							hBox.setAlignment(Pos.CENTER);
							hBox.setSpacing(5);

							Platform.runLater(() -> {
								for (int i = 0; i < item.getPhotos().size(); i++) {
									Image image = new Image(item.getPhotos().get(i));
									ImageView imageView = new ImageView(image);
									imageView.setFitHeight(100);
									imageView.setFitWidth(100);
									imageView.setPreserveRatio(true);

									imageView.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
										ColorAdjust colorAdjust = new ColorAdjust();
										colorAdjust.setBrightness(-0.5);
										imageView.setEffect(colorAdjust);
										imageView.setCursor(Cursor.HAND);
									});

									imageView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
										imageView.setEffect(null);
										imageView.setCursor(Cursor.DEFAULT);
									});

									final int currentPageIndex = i;
									imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, event ->
											confirmationProduct(item, false, currentPageIndex)
									);

									hBox.getChildren().add(imageView);
								}
							});

							setGraphic(hBox);
						} else {
							setGraphic(null);
						}
					}
				});

		TableColumn<Product, Boolean> fourthColumn = new TableColumn<>(getResources().getString("confirmed"));
		fourthColumn.setSortable(false);
		fourthColumn.setResizable(false);
		fourthColumn.setEditable(false);
		fourthColumn.prefWidthProperty().bind(ordersTableView.widthProperty().multiply(0.15));
		fourthColumn.setCellValueFactory(param -> confirmedMap.get(param.getValue()) );
		fourthColumn.setCellFactory(new ImageClickCellFactory(
				new CreateFavoriteClickMouseEventHandler()));
		
		ordersTableView.getColumns().clear();
		ordersTableView.getColumns().add(firstColumn);
		ordersTableView.getColumns().add(secondColumn);
		ordersTableView.getColumns().add(thirdColumn);
		ordersTableView.getColumns().add(fourthColumn);

		ordersTableView.setPlaceholder(new Text(getResources().getString("verify.noData")));
		
		ordersTableView.addEventFilter(KeyEvent.KEY_RELEASED, keyEvent -> {
			if (keyEvent.getCode() == KeyCode.SPACE) {
				int index = ordersTableView.getSelectionModel().getSelectedIndex();
				if (index > -1) {
					confirmationProduct(dataList.get(index), false);
				}
			}
		});

		actInfoContainer.prefWidthProperty().bind(((AnchorPane) rootNode).widthProperty().multiply(0.25));
	}

	private class ImageClickCellFactory implements Callback<TableColumn<Product, Boolean>, TableCell<Product, Boolean>> {

		private final EventHandler<MouseEvent> click;

		public ImageClickCellFactory(EventHandler<MouseEvent> click) {
			this.click = click;
		}

		@Override
		public TableCell<Product, Boolean> call(TableColumn<Product, Boolean> p) {

			TableCell<Product, Boolean> cell
					= new TableCell<Product, Boolean>() {

				private final String imageName = "/resources/images/confirmed.png";
				private final VBox vBox;
				private final ImageView imageView;
				private final Button button;
				// Constructor
				{
					vBox = new VBox();
					vBox.setAlignment(Pos.CENTER);

					Image image = new Image(getClass().getResourceAsStream(imageName));

					imageView = new ImageView();
					imageView.setVisible(true);
					imageView.setCache(true);
					imageView.setImage(image);

					button = new Button("Подтвердить");
					button.getStyleClass().add("custom-verify-button");
					button.setPrefSize(160, 64);
					button.setOnAction(event ->
							{
								int index = getIndex();
								if (index > -1) {
									confirmationProduct(dataList.get(index), false);
								}
							}
					);

					vBox.getChildren().addAll(imageView);
					setGraphic(vBox);
				}

				@Override
				protected void updateItem(Boolean item,
										  boolean empty) {
					super.updateItem(item, empty);

					if (item != null) {
						if (item) {
							setGraphic(vBox);
						}
						else {
							setGraphic(button);
						}
					} else {
						setGraphic(null);
					}
				}
			};

			// Double click
			if (click != null) {
				cell.setOnMouseClicked(click);
			}

			cell.setCursor(Cursor.HAND);
			return cell;
		}
	}

	private class CreateFavoriteClickMouseEventHandler
			implements EventHandler<MouseEvent> {

		@Override
		public void handle(MouseEvent event) {
			if (event.getClickCount() == 1) {

				try {
					int index =((TableCell<?,?>) event.getSource()).getIndex();
					if (index > -1) {
						confirmationProduct(dataList.get(index), false);
					}

				} catch (IndexOutOfBoundsException ignored) {
				}
			}
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (barcodeScanner != null) {
			barcodeScanner.removeBarcodeListener(this);
		}

		for (Task<?> task : tasks) {
			task.cancel(false);
		}
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		cancelButton.setDisable(value);
	}

	public void verifyCloseClick() {
		getAction().setNextAction(getAction().getVerifyCloseAction());
		VerifyController.this.done();
	}

	public void cancelClick() {
		getAction().setNextAction(getAction().getCancelAction());
		VerifyController.this.done();
	}

	private void confirmationProduct(Product product, boolean silent) {
		confirmationProduct(product, silent, 0);
	}

	private void confirmationProduct(Product product, boolean silent, int currentPageIndex) {
		if (product != null) {
			if (silent && product.getCount() == 1 )
			{
				BooleanProperty confirmedProperty = confirmedMap.get(product);
				confirmedProperty.set(true);

				checkСonfirmation();
			}
			else
			{
				Window owner = rootNode.getScene().getWindow();
				confirmationImageDialog = new ConfirmationImageDialog(owner, "dialog.confirm", "confirmation.product", product.getPhotos(), currentPageIndex, new ConfirmationListener() {

					@Override
					public void onAccept() {
						BooleanProperty confirmedProperty = confirmedMap.get(product);
						if (confirmedProperty != null) {
							confirmedProperty.set(true);

							checkСonfirmation();
						}

						confirmationImageDialog = null;
					}

					@Override
					public void onDecline() {
						BooleanProperty confirmedProperty = confirmedMap.get(product);
						if (confirmedProperty != null) {
							confirmedProperty.set(false);

							checkСonfirmation();
						}

						confirmationImageDialog = null;
					}
				});

				confirmationImageDialog.centerOnScreen();
				confirmationImageDialog.setDescription(product.getDescription());
				confirmationImageDialog.setCount("confirmation.count", product.getCount());
				confirmationImageDialog.show();
			}
		}
	}

	@Override
	public void onCatchBarcode(final String value) {
		Platform.runLater(() -> processBarcode(value));
	}

	private void processBarcode(final String value) {
		if (progressVisibleProperty.get() || confirmationImageDialog != null) {
			return;
		}

		Task<?> task = new Task<Product>() {
			@Override
			public Product call() throws Exception {
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

				Product result = getValue();
				if (result != null) {
					confirmationProduct(result, true);

				} else {
					errorMessageProperty.set(getResources().getString("error.barcode.invalid.code"));
					errorVisibleProperty.set(true);
				}
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
		tasks.add(task);
	}

	private void checkСonfirmation()
	{
		long count = confirmedMap.entrySet().stream().filter(p -> p.getValue().get()).count();
		verifyCloseButton.setDisable(confirmedMap.size() != count);
	}
}