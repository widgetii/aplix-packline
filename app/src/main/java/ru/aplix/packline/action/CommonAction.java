package ru.aplix.packline.action;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import ru.aplix.packline.Const;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.dialog.ManualInputDialog;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.StandardWorkflowController;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CommonAction<Controller extends StandardWorkflowController<?>> extends WorkflowActionWithUserActivityMonitor<Controller> {

	protected final Log LOG = LogFactory.getLog(getClass());

	private Timeline logoImageMousePressedTimeline;
	private int logoImageMousePressedCount;
	private Parent rootNode;
	protected Label postsCountLabel;
	private ContextMenu contextMenu = null;
	private boolean skipClick;

	private Task<?> task;

	public CommonAction() {
		logoImageMousePressedTimeline = new Timeline();
		logoImageMousePressedTimeline.setCycleCount(2);
		logoImageMousePressedTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), event -> {
            logoImageMousePressedCount--;
            if (logoImageMousePressedCount == 0) {
                skipClick = true;
                startFromBeginning();
            }
        }));
	}

	@Override
	public void execute(WorkflowContext context) {
		super.execute(context);

		updatePostsCount();
	}

	@Override
	public void done() {
		if (task != null) {
			task.cancel(false);
		}

		super.done();
	}

	@Override
	protected void onFormLoaded(Parent rootNode, final ResourceBundle resources) {
		super.onFormLoaded(rootNode, resources);
		this.rootNode = rootNode;

		// Add mouse handlers for logoImage
		final ImageView logoImage = (ImageView) rootNode.lookup("#logoImage");
		if (logoImage != null) {
			logoImage.setOnMouseClicked(mouseEvent -> {
                if (skipClick) {
                    return;
                }

                try {
                    if (contextMenu == null) {
                        contextMenu = createContextMenu(resources);
                    }
                    contextMenu.show(logoImage, Side.BOTTOM, 0, 0);
                } catch (FileNotFoundException | MalformedURLException | JAXBException e) {
                    LOG.error(null, e);
                }
            });
			logoImage.setOnMousePressed(mouseEvent -> {
                skipClick = false;
                logoImageMousePressedCount = 2;
                logoImageMousePressedTimeline.playFromStart();
            });
			logoImage.setOnMouseReleased(mouseEvent -> logoImageMousePressedTimeline.stop());
			logoImage.setOnMouseExited(mouseEvent -> logoImageMousePressedTimeline.stop());
		}

		try {
			if (Configuration.getInstance().getRoles().getLabeling()) {
				postsCountLabel = (Label) rootNode.lookup("#postsCountLabel");
				if (postsCountLabel != null) {
					postsCountLabel.setVisible(true);
					postsCountLabel.setText(null);
					postsCountLabel.setOnMouseClicked(mouseEvent -> showActivePosts());
				}
			}
		} catch (FileNotFoundException | MalformedURLException | JAXBException e) {
			LOG.error(null, e);
		}
	}

	protected void updatePostsCount() {
		if (postsCountLabel == null || (task != null && task.isRunning())) {
			return;
		}

		task = new Task<Integer>() {
			@Override
			public Integer call() throws Exception {
				try {
					PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
					return postServicePort.getActivePostsCount();
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				updatePostsCount("" + getValue());
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	public void updatePostsCount(String value) {
		postsCountLabel.setText(value);
	}

	private ContextMenu createContextMenu(ResourceBundle resources) throws FileNotFoundException, MalformedURLException, JAXBException {
		String allMenuStyles = null;
		try {
			allMenuStyles = IOUtils.toString(getClass().getResourceAsStream("/resources/styles/menu.css"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		String menuItemStyle = extractStyle(allMenuStyles, "default-menu-item");
		String menuItemStyle2 = extractStyle(allMenuStyles, "menu-item");

		ContextMenu result = new ContextMenu();

		MenuItem itemBegin = new MenuItem(resources.getString("menu.begin"));
		itemBegin.setStyle(menuItemStyle);
		itemBegin.setOnAction(e -> startFromBeginning());

		MenuItem itemBarcode = null;
		if (getController() instanceof BarcodeListener) {
			itemBarcode = new MenuItem(resources.getString("menu.input.barcode"));
			itemBarcode.setStyle(menuItemStyle2);
			itemBarcode.setOnAction(e -> manualBarcodeInput());
		}

		MenuItem itemWeight = null;
		if (getController() instanceof MeasurementListener) {
			itemWeight = new MenuItem(resources.getString("menu.input.weight"));
			itemWeight.setStyle(menuItemStyle2);
			itemWeight.setOnAction(e -> manualWeightInput());
		}

		MenuItem reconnectBarcode = null;
		if (getController() instanceof BarcodeListener) {
			reconnectBarcode = new MenuItem(resources.getString("menu.barcode.reconnect"));
			reconnectBarcode.setStyle(menuItemStyle2);
			reconnectBarcode.setOnAction(e -> reconnectBarcodeScanner());
		}

		MenuItem reconnectScales = null;
		if (getController() instanceof MeasurementListener) {
			reconnectScales = new MenuItem(resources.getString("menu.scales.reconnect"));
			reconnectScales.setStyle(menuItemStyle2);
			reconnectScales.setOnAction(e -> reconnectScales());
		}

		MenuItem itemMarkersForContainers = null;
		MenuItem itemMarkersForCustomer = null;
		if (Configuration.getInstance().getRoles().getGluing()) {
			itemMarkersForContainers = new MenuItem(resources.getString("menu.markers.container"));
			itemMarkersForContainers.setStyle(menuItemStyle2);
			itemMarkersForContainers.setOnAction(e -> generateStickersForContainer());

			itemMarkersForCustomer = new MenuItem(resources.getString("menu.markers.customer"));
			itemMarkersForCustomer.setStyle(menuItemStyle2);
			itemMarkersForCustomer.setOnAction(e -> generateStickersForCustomer());
		}

		MenuItem itemWarrantyCard = null;
		if (Configuration.getInstance().getRoles().getWarranty()) {
			itemWarrantyCard = new MenuItem(resources.getString("menu.warranty.card"));
			itemWarrantyCard.setStyle(menuItemStyle2);
			itemWarrantyCard.setOnAction(e -> fillWarrantyCard());
		}

		MenuItem itemRemarking = null;
		if (Configuration.getInstance().getRoles().getLabeling()) {
			itemRemarking = new MenuItem(resources.getString("menu.remarking"));
			itemRemarking.setStyle(menuItemStyle2);
			itemRemarking.setOnAction(e -> remarking());
		}

		MenuItem itemZebraTest = null;
		if (Configuration.getInstance().getRoles().getLabeling()) {
			itemZebraTest = new MenuItem(resources.getString("menu.zebra.test"));
			itemZebraTest.setStyle(menuItemStyle2);
			itemZebraTest.setOnAction(e -> testZebraPrinter());
		}

		MenuItem itemControlReturns = null;
		if (Configuration.getInstance().getRoles().getReturns()) {
			itemControlReturns = new MenuItem(resources.getString("menu.control.returns"));
			itemControlReturns.setStyle(menuItemStyle2);
			itemControlReturns.setOnAction(e -> controlReturns());
		}

		MenuItem itemCloseRegistry = null;
		if (Configuration.getInstance().getRoles().getLabeling()) {
			itemCloseRegistry = new MenuItem(resources.getString("menu.registry.close"));
			itemCloseRegistry.setStyle(menuItemStyle2);
			itemCloseRegistry.setOnAction(e -> closeRegistry());
		}

		MenuItem itemActivePosts = null;
		if (Configuration.getInstance().getRoles().getLabeling()) {
			itemActivePosts = new MenuItem(resources.getString("menu.active.posts"));
			itemActivePosts.setStyle(menuItemStyle2);
			itemActivePosts.setOnAction(e -> showActivePosts());
		}

		MenuItem itemPrintBarcode = null;
		if (Configuration.getInstance().getRoles().getLabeling()) {
			itemPrintBarcode = new MenuItem(resources.getString("menu.print.barcode"));
			itemPrintBarcode.setStyle(menuItemStyle2);
			itemPrintBarcode.setOnAction(e -> showPrintBarcode());
		}

		Menu subMenu = new Menu(resources.getString("menu.sub.operations"));
		subMenu.setStyle(menuItemStyle2);
		if (itemMarkersForContainers != null) {
			subMenu.getItems().add(itemMarkersForContainers);
		}
		if (itemMarkersForCustomer != null) {
			subMenu.getItems().add(itemMarkersForCustomer);
		}
		if (itemWarrantyCard != null) {
			subMenu.getItems().add(itemWarrantyCard);
		}
		if (itemRemarking != null) {
			subMenu.getItems().add(itemRemarking);
		}
		if (itemZebraTest != null) {
			subMenu.getItems().add(itemZebraTest);
		}
		if (itemControlReturns != null) {
			subMenu.getItems().add(itemControlReturns);
		}
		if (itemCloseRegistry != null) {
			subMenu.getItems().add(itemCloseRegistry);
		}
		if (itemActivePosts != null) {
			subMenu.getItems().add(itemActivePosts);
		}
		if (itemPrintBarcode != null) {
			subMenu.getItems().add(itemPrintBarcode);
		}

		result.getItems().add(itemBegin);
		if (itemBarcode != null) {
			result.getItems().add(itemBarcode);
		}
		if (itemWeight != null) {
			result.getItems().add(itemWeight);
		}
		if (reconnectBarcode != null) {
			result.getItems().add(reconnectBarcode);
		}
		if (reconnectScales != null) {
			result.getItems().add(reconnectScales);
		}
		if (subMenu.getItems().size() > 0) {
			result.getItems().add(subMenu);
		}
		return result;
	}

	private String extractStyle(String allStyles, String styleName) {
		Pattern pattern = Pattern.compile(String.format(".%s\\s*\\{([^\\{\\}]+)\\}", styleName));
		Matcher matcher = pattern.matcher(allStyles);
		if (matcher.find() && matcher.groupCount() == 1) {
			return matcher.group(1);
		}
		return null;
	}

	private void startFromBeginning() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.START_WORKFLOW_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void manualBarcodeInput() {
		Window owner = rootNode.getScene().getWindow();
		ManualInputDialog mid = new ManualInputDialog(owner, value -> {
            if (getController() instanceof BarcodeListener) {
                ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
                @SuppressWarnings({ "unchecked" })
                List<BarcodeListener> list = (List<BarcodeListener>) applicationContext.getBean(Const.BARCODE_LISTENERS);
                for (BarcodeListener bl : list) {
                    bl.onCatchBarcode(value);
                }
            }
        });
		mid.centerOnScreen();
		mid.show();
	}

	private void manualWeightInput() {
		Window owner = rootNode.getScene().getWindow();
		ManualInputDialog mid = new ManualInputDialog(owner, value -> {
            if (getController() instanceof MeasurementListener) {
                try {
                    Float floatValue = Float.valueOf(value);
                    ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
                    @SuppressWarnings({ "unchecked" })
                    List<MeasurementListener> list = (List<MeasurementListener>) applicationContext.getBean(Const.MEASUREMENT_LISTENERS);
                    for (MeasurementListener ml : list) {
                        ml.onWeightStabled(floatValue);
                    }
                } catch (NumberFormatException nfe) {
                }
            }
        });
		mid.centerOnScreen();
		mid.show();
	}

	private void generateStickersForContainer() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.GEN_STICK_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void generateStickersForCustomer() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.GEN_STICK_CUSTOMER_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void fillWarrantyCard() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.WARRANTY_CARD_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void remarking() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.REMARKING_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void testZebraPrinter() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.ZEBRA_TEST_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void controlReturns() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.CONTROL_RETURNS_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void closeRegistry() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.CLOSE_REGISTRY_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void showActivePosts() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.ACTIVE_POSTS_ACTION_BEAN_NAME);
		WorkflowAction currentWA = (WorkflowAction) getContext().getAttribute(Const.CURRENT_WORKFLOW_ACTION);
		if (wa == currentWA) {
			((CommonAction<?>) wa).done();
		} else {
			wa.execute(getContext());
		}
	}

	private void showPrintBarcode() {
		ApplicationContext applicationContext = (ApplicationContext) getContext().getAttribute(Const.APPLICATION_CONTEXT);
		WorkflowAction wa = (WorkflowAction) applicationContext.getBean(Const.PRINT_BARCODE_ACTION_BEAN_NAME);
		wa.execute(getContext());
	}

	private void reconnectBarcodeScanner() {
		BarcodeScanner<?> barcodeScanner = (BarcodeScanner<?>) getContext().getAttribute(Const.BARCODE_SCANNER);
		if (barcodeScanner != null) {
			barcodeScanner.disconnect();
		}
	}

	private void reconnectScales() {
		Scales<?> scales = (Scales<?>) getContext().getAttribute(Const.SCALES);
		if (scales != null) {
			scales.disconnect();
		}
	}
}