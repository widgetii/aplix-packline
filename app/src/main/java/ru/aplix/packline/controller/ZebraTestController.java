package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.action.ZebraTestAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class ZebraTestController extends StandardController<ZebraTestAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	public Button buttonComplete;
	@FXML
	public Button buttonPrintTest;

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		setProgress(false);
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
		}
	}

	public void generateClick(ActionEvent event) {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().test();
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;
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

				String error = getException().getMessage() != null ? getException().getMessage() : getException().getClass().getSimpleName();
				errorMessageProperty.set(error);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				setProgress(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	public void completeClick(ActionEvent event) {
		done();
	}

	private void setProgress(boolean value) {
		progressVisibleProperty.set(value);
		buttonComplete.setDisable(value);
		buttonPrintTest.setDisable(value);
	}
}
