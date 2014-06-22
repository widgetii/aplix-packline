package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.WarningAction;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WarningController extends StandardController<WarningAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private Button nextButton;
	@FXML
	private Button cancelButton;

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);
		cancelButton.setDisable(false);

		String message = (String) context.getAttribute(Const.WARNING_MESSAGE);
		infoLabel.setText(message);

		if (message == null || message.length() == 0) {
			getAction().setNextAction(getAction().getNormalAction());
			throw new SkipActionException();
		}
	}

	public void nextClick(ActionEvent event) {
		getAction().setNextAction(getAction().getNormalAction());
		done();
	}

	public void backClick(ActionEvent event) {
		doAction();
	}

	private void doAction() {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().process();
				} catch (Throwable e) {
					LOG.error(null, e);
					throw e;
				}
				return null;
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
				nextButton.setDisable(true);
				cancelButton.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);
				cancelButton.setDisable(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = getResources().getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);
				cancelButton.setDisable(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				WarningController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}
}
