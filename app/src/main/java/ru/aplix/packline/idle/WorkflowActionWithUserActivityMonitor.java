package ru.aplix.packline.idle;

import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.InputEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.conf.ActivityMonitorConfiguration;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.workflow.StandardWorkflowAction;
import ru.aplix.packline.workflow.StandardWorkflowController;
import ru.aplix.packline.workflow.WorkflowContext;

@SuppressWarnings("rawtypes")
public abstract class WorkflowActionWithUserActivityMonitor<Controller extends StandardWorkflowController> extends StandardWorkflowAction<Controller> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ActivityMonitorConfiguration config;
	private IdleTimeout idleTimeout;

	@Override
	public void execute(WorkflowContext context) {
		super.execute(context);

		try {
			if (idleTimeout != null) {
				if (config == null) {
					config = Configuration.getInstance().getActivityMonitorConfiguration();
				}

				switch (idleTimeout) {
				case SHORT:
					UserActivityMonitor.setTreshold(config.getIdleShortTresholdInMillis());
					break;
				case LONG:
					UserActivityMonitor.setTreshold(config.getIdleLongTresholdInMillis());
					break;
				}
			}
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

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

	public IdleTimeout getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(IdleTimeout idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
}
