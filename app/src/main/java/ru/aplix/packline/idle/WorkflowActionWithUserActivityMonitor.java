package ru.aplix.packline.idle;

import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.InputEvent;
import ru.aplix.packline.workflow.StandardWorkflowAction;
import ru.aplix.packline.workflow.StandardWorkflowController;

@SuppressWarnings("rawtypes")
public abstract class WorkflowActionWithUserActivityMonitor<Controller extends StandardWorkflowController> extends StandardWorkflowAction<Controller> {

	@Override
	protected void onFormLoaded(Parent rootNode, ResourceBundle resources) {
		super.onFormLoaded(rootNode, resources);

		// Add user activity event filter
		rootNode.addEventFilter(InputEvent.ANY, new EventHandler<InputEvent>() {
			@Override
			public void handle(InputEvent inputEvent) {
				UserActivityMonitor.getInstance().reset();
			}
		});
	}

	@Override
	public void done() {
		UserActivityMonitor.getInstance().reset();

		super.done();
	}
}
