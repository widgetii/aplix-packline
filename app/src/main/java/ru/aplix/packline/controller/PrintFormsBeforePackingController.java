package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.PrintFormsBeforePackingAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class PrintFormsBeforePackingController extends StandardController<PrintFormsBeforePackingAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
		getAction().prepare();

		task = new PrintTask();
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

	/**
	 * 
	 */
	private class PrintTask extends Task<Integer> {

		@Override
		public Integer call() throws Exception {
			long t = System.currentTimeMillis();

			int[] printed = new int[] { 0 };
			try {
				getAction().downloadAndPrintDocuments(printed);
			} finally {
				if (printed[0] > 0) {
					t = System.currentTimeMillis() - t;
					LOG.info(String.format("Printing time: %.1f sec", (float) t / 1000f));
				}
			}

			return Integer.valueOf(printed[0]);
		}

		@Override
		protected void running() {
			super.running();

			progressVisibleProperty.set(true);
		}

		@Override
		protected void failed() {
			super.failed();

			progressVisibleProperty.set(false);

			LOG.error(null, getException());

			String error = getException().getMessage() != null ? getException().getMessage() : getException().getClass().getSimpleName();
			errorMessageProperty.set(error);
			errorVisibleProperty.set(true);

			errorVisibleProperty.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (!newValue) {
						errorVisibleProperty.removeListener(this);
						PrintFormsBeforePackingController.this.done();
					}
				}
			});
		}

		@Override
		protected void succeeded() {
			super.succeeded();

			progressVisibleProperty.set(false);

			if (getValue() > 0) {
				showWarningMessage(getResources().getString("printing.info3"));

				errorVisibleProperty.addListener(new ChangeListener<Boolean>() {
					@Override
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (!newValue) {
							errorVisibleProperty.removeListener(this);
							PrintFormsBeforePackingController.this.done();
						}
					}
				});
			} else {
				PrintFormsBeforePackingController.this.done();
			}
		}
	};
}
