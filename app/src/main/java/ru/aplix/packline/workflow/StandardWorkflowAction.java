package ru.aplix.packline.workflow;

import java.io.IOException;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.utils.Utils;

@SuppressWarnings("rawtypes")
public abstract class StandardWorkflowAction<Controller extends StandardWorkflowController> implements WorkflowAction, DoneListener {

	private final Log LOG = LogFactory.getLog(getClass());

	private WorkflowContext context;
	private WorkflowAction nextAction;
	private Scene scene = null;
	private Controller controller = null;
	private ResourceBundle resources;

	public WorkflowAction getNextAction() {
		return nextAction;
	}

	public void setNextAction(WorkflowAction nextAction) {
		this.nextAction = nextAction;
	}

	public WorkflowContext getContext() {
		return context;
	}

	protected Controller getController() {
		return controller;
	}

	protected ResourceBundle getResources() {
		return resources;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(WorkflowContext context) {
		LOG.info(String.format("Executing %s...", getClass().getSimpleName()));
		try {
			// Remember context for future use
			this.context = context;

			// Terminate current action(controller) if exists
			StandardWorkflowAction<?> currentAction = (StandardWorkflowAction<?>) context.getAttribute(Const.CURRENT_WORKFLOW_ACTION);
			if (currentAction != null && currentAction.controller != null) {
				currentAction.controller.terminate();
			}

			// Load form, controoler and create scene
			if (controller == null) {
				String fxmlName = String.format("/resources/fxml/%s.fxml", getFormName());
				resources = ResourceBundle.getBundle("resources.messages.strings");
				FXMLLoader loader = new FXMLLoader();
				loader.setLocation(getClass().getResource(fxmlName));
				loader.setResources(resources);
				Parent rootNode = (Parent) loader.load(getClass().getResourceAsStream(fxmlName));
				onFormLoaded(rootNode, resources);

				controller = (Controller) loader.getController();
				controller.setDoneListener(this);
				controller.setAction(this);

				scene = new Scene(rootNode);
			}

			// Set current controller
			context.setAttribute(Const.CURRENT_WORKFLOW_ACTION, this);
			context.setAttribute(Const.CURRENT_WORKFLOW_CONTROLLER, this.controller);

			// Prepare controller
			controller.prepare(context);

			// Set scene on stage
			Stage stage = (Stage) context.getAttribute(Const.STAGE);
			stage.setScene(scene);
			if (!stage.isShowing()) {
				stage.show();
			}
		} catch (IOException e) {
			LOG.error(null, e);
		} catch (SkipActionException sae) {
			done();
		}
	}

	@Override
	public void done() {
		if (nextAction != null) {
			nextAction.execute(context);
		}
	}

	protected abstract String getFormName();

	protected void onFormLoaded(Parent rootNode, ResourceBundle resources) {
		if (rootNode instanceof Region) {
			Region region = (Region) rootNode;
			Rectangle2D screenBounds = (Rectangle2D) getContext().getAttribute(Const.SCREEN_BOUNDS);
			region.setPrefWidth(screenBounds.getWidth());
			region.setPrefHeight(screenBounds.getHeight());
		}

		if (Utils.isJ2DPipelineUsed) {
			rootNode.getStylesheets().clear();
			rootNode.getStylesheets().add(getClass().getResource("/resources/styles-lg/styles.css").toExternalForm());
		}
	}
}
