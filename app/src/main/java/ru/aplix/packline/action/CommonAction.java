package ru.aplix.packline.action;

import java.io.IOException;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;

import ru.aplix.packline.Const;
import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.idle.WorkflowActionWithUserActivityMonitor;
import ru.aplix.packline.workflow.StandardWorkflowController;
import ru.aplix.packline.workflow.WorkflowAction;

public abstract class CommonAction<Controller extends StandardWorkflowController<?>> extends WorkflowActionWithUserActivityMonitor<Controller> {

	private Timeline logoImageMousePressedTimeline;
	private int logoImageMousePressedCount;

	private ContextMenu contextMenu = null;
	private boolean skipClick;

	public CommonAction() {
		logoImageMousePressedTimeline = new Timeline();
		logoImageMousePressedTimeline.setCycleCount(2);
		logoImageMousePressedTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(1), new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				logoImageMousePressedCount--;
				if (logoImageMousePressedCount == 0) {
					skipClick = true;
					startFromBeginning();
				}
			}
		}));
	}

	@Override
	protected void onFormLoaded(Parent rootNode, final ResourceBundle resources) {
		super.onFormLoaded(rootNode, resources);

		// Add mouse handlers for logoImage
		final ImageView logoImage = (ImageView) rootNode.lookup("#logoImage");
		if (logoImage != null) {
			logoImage.setOnMouseClicked(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					if (skipClick) {
						return;
					}

					if (contextMenu == null) {
						contextMenu = createContextMenu(resources);
					}
					contextMenu.show(logoImage, Side.BOTTOM, 0, 0);
				}
			});
			logoImage.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					skipClick = false;
					logoImageMousePressedCount = 2;
					logoImageMousePressedTimeline.playFromStart();
				}
			});
			logoImage.setOnMouseReleased(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					logoImageMousePressedTimeline.stop();
				}
			});
			logoImage.setOnMouseExited(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent mouseEvent) {
					logoImageMousePressedTimeline.stop();
				}
			});
		}
	}

	private ContextMenu createContextMenu(ResourceBundle resources) {
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
		itemBegin.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				startFromBeginning();
			}
		});

		MenuItem itemBarcode = null;
		if (getController() instanceof BarcodeListener) {
			itemBarcode = new MenuItem(resources.getString("menu.input.barcode"));
			itemBarcode.setStyle(menuItemStyle);
			itemBarcode.setDisable(true);
			itemBarcode.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) {
				}
			});
		}

		MenuItem itemMarkers = new MenuItem(resources.getString("menu.markers"));
		itemMarkers.setStyle(menuItemStyle2);
		itemMarkers.setDisable(true);
		itemMarkers.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
			}
		});

		Menu subMenu = new Menu(resources.getString("menu.sub.operations"));
		subMenu.setStyle(menuItemStyle2);
		subMenu.getItems().add(itemMarkers);

		result.getItems().add(itemBegin);
		if (itemBarcode != null) {
			result.getItems().add(itemBarcode);
		}
		result.getItems().add(subMenu);
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
}