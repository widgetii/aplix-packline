package ru.aplix.packline.idle;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class UserActivityMonitor extends Service<Void> {

	private final Log LOG = LogFactory.getLog(getClass());

	private static UserActivityMonitor instance = null;

	private volatile long idleTime;

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
								idleListener.idle();
							}
						});
					}
				}
				return null;
			}
		};
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
}
