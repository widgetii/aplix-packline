package ru.aplix.packline.controller;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.ReturnRegistryDeleteAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class ReturnRegistryDeleteController extends StandardController<ReturnRegistryDeleteAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		task = new Task<Void>() {

			private CountDownLatch terminateLatch = new CountDownLatch(1);

			@Override
			public Void call() throws Exception {
				try {
					getAction().process();
				} catch (final Throwable e) {
					LOG.error(null, e);

					if (!isCancelled()) {
						Platform.runLater(new Runnable() {

							@Override
							public void run() {
								progressVisibleProperty.set(false);

								String errorStr;
								if (e instanceof PackLineException) {
									errorStr = e.getMessage();
								} else {
									errorStr = getResources().getString("error.post.service");
								}

								errorMessageProperty.set(errorStr);
								errorVisibleProperty.set(true);
							}
						});

						terminateLatch.await(Const.ERROR_DISPLAY_DELAY, TimeUnit.SECONDS);
					}
				}
				return null;
			}

			@Override
			public boolean cancel(boolean mayInterruptIfRunning) {
				terminateLatch.countDown();
				return super.cancel(mayInterruptIfRunning);
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				ReturnRegistryDeleteController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}
}
