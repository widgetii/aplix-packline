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
import ru.aplix.packline.action.UnderweightAction;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowContext;

public class UnderweightController extends StandardController<UnderweightAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private Button nextButton;
	@FXML
	private Button cancelButton;
	@FXML
	private Button weightingButton;

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);
		cancelButton.setDisable(false);
		weightingButton.setDisable(false);

		try {
			Container container = (Container) getContext().getAttribute(Const.TAG);
			Order order = (Order) getContext().getAttribute(Const.ORDER);

			Float diff = 0f;
			if (order.getIncoming() != null && order.getIncoming().size() == 1) {
				diff = order.getIncoming().get(0).getWeight() - container.getTotalWeight();
			}

			infoLabel.setText(String.format(getResources().getString("underweight.info"), String.format("%.3f", diff)));

			Utils.playSound(Utils.SOUND_WARNING);
		} catch (Exception e) {
			LOG.error(null, e);
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}

	public void nextClick(ActionEvent event) {
		getAction().setNextAction(getAction().getNormalAction());
		done();
	}
	
	public void backClick(ActionEvent event) {
		doAction();
	}

	public void weightingClick(ActionEvent event) {
		getAction().setNextAction(getAction().getWeightingAction());
		done();
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
				weightingButton.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);
				cancelButton.setDisable(false);
				weightingButton.setDisable(false);

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
				weightingButton.setDisable(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				UnderweightController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}
}
