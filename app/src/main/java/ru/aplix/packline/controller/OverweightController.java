package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.OverweightAction;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.conf.WeightingRestriction;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowContext;

public class OverweightController extends StandardController<OverweightAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private Button nextButton;
	@FXML
	private Button weightingButton;

	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);
		weightingButton.setDisable(false);

		try {
			Container container = (Container) getContext().getAttribute(Const.TAG);
			final Post post = (Post) getContext().getAttribute(Const.POST);

			WeightingRestriction wr = (WeightingRestriction) CollectionUtils.find(Configuration.getInstance().getWeighting().getWeightingRestrictions(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return post.getPostType().equals(((WeightingRestriction) object).getPostType());
				}
			});

			infoLabel.setText(String.format(getResources().getString("overweight.info"), String.format("%.3f", container.getTotalWeight()),
					String.format("%.3f", wr.getMaxWeight())));
			
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
		doAction();
	}

	public void weightingClick(ActionEvent event) {
		getContext().setAttribute(Const.BWL_WEIGHT, null);
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
				weightingButton.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);
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
				weightingButton.setDisable(false);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				OverweightController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}
}
