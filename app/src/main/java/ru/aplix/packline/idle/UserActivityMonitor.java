package ru.aplix.packline.idle;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.WorkflowContext;

public class UserActivityMonitor extends Service<Void> {

	private final Log LOG = LogFactory.getLog(getClass());

	private static UserActivityMonitor instance = null;

	private WorkflowContext workflowContext;

	private volatile long idleTime;
	private long lastActivityLogTime;
	private boolean previousState = false;

	private static long treshold = DateUtils.MILLIS_PER_MINUTE * 10;

	private IdleListener idleListener;

	private UserActivityMonitor() {
		reset();
	}

	public static UserActivityMonitor getInstance() {
		if (instance == null) {
			instance = new UserActivityMonitor();
		}
		return instance;
	}

	@Override
	protected Task<Void> createTask() {
		return new Task<Void>() {
			@Override
			public Void call() {
				while (!isCancelled()) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException ie) {
					}

					long diff = System.currentTimeMillis() - idleTime;
					if (diff > treshold) {
						LOG.debug(String.format("User is idle about %d seconds", diff / DateUtils.MILLIS_PER_SECOND));

						reset();

						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								if (idleListener != null) {
									idleListener.idle();
								}
							}
						});
					}

					logOperatorActivity(false);
				}

				logOperatorActivity(true);

				return null;
			}
		};
	}

	private void logOperatorActivity(boolean endOfLoop) {
		if (endOfLoop && previousState) {
			logStateChange(false);
			return;
		}

		Operator operator = null;
		if (workflowContext != null) {
			operator = (Operator) workflowContext.getAttribute(Const.OPERATOR);
		}

		boolean state = (operator != null);
		if (state) {
			long diff = System.currentTimeMillis() - lastActivityLogTime;
			if ((previousState != state) || (diff > DateUtils.MILLIS_PER_MINUTE)) {
				logStateChange(true);
			}
		} else {
			if (previousState) {
				logStateChange(false);
			}
		}

		previousState = state;
	}

	private void logStateChange(boolean value) {
		try {
			LOG.debug(String.format("Logging operator activity: %s", value ? "active" : "inactive"));
			lastActivityLogTime = System.currentTimeMillis();
			
			PackingLinePortType postServicePort = (PackingLinePortType) workflowContext.getAttribute(Const.POST_SERVICE_PORT);
			if (postServicePort != null) {
				postServicePort.setOperatorActivity(value);
			}
		} catch (Throwable e) {
			LOG.error(null, e);
		}
	}

	public void reset() {
		idleTime = System.currentTimeMillis();
	}

	public static long getTreshold() {
		return treshold;
	}

	public static void setTreshold(long value) {
		treshold = value;
	}

	public IdleListener getIdleListener() {
		return idleListener;
	}

	public void setIdleListener(IdleListener idleListener) {
		this.idleListener = idleListener;
	}

	public WorkflowContext getWorkflowContext() {
		return workflowContext;
	}

	public void setWorkflowContext(WorkflowContext workflowContext) {
		this.workflowContext = workflowContext;
	}
}
