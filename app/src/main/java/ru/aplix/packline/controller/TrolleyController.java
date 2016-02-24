package ru.aplix.packline.controller;

import java.util.concurrent.ExecutorService;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.TrolleyAction;
import ru.aplix.packline.action.TrolleyAction.TrolleyType;
import ru.aplix.packline.conf.Configuration;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.WorkflowContext;

public class TrolleyController extends StandardController<TrolleyAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private ImageView imageView;
	@FXML
	private Button nextButton;
	@FXML
	private Button photoButton;

	private TrolleyType trolleyType;
	private Timeline autoFirer;
	private Task<?> task;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);
		photoButton.setDisable(false);
		photoButton.setVisible(true);

		trolleyType = getAction().getTrolleyMessage();
		switch (trolleyType) {
		case SKIP:
			infoLabel.setText(getResources().getString("trolley.skip.info"));
			Image image = new Image(getClass().getResource("/resources/images/img-trolley-skip.png").toExternalForm());
			imageView.setImage(image);
			if (trolleyPackAutoClose()) {
				// autoClose is used more often and operator's attention may be dissipated
				// so we awake him by sound
				Utils.playSound(Utils.SOUND_WARNING);
			}
			break;
		case PACK:
			infoLabel.setText(getResources().getString("trolley.pack.info"));
			image = new Image(getClass().getResource("/resources/images/img-trolley-red.png").toExternalForm());
			imageView.setImage(image);

			if (trolleyPackAutoClose()) {
				// Give operator 5 seconds to make more photos
				autoFirer = new Timeline(new KeyFrame(Duration.seconds(5), new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						nextButton.fire();
					}
				}));
				autoFirer.playFromStart();
			}
			break;
		case KEEP:
			infoLabel.setText(getResources().getString("trolley.keep.info"));
			image = new Image(getClass().getResource("/resources/images/img-trolley-green.png").toExternalForm());
			imageView.setImage(image);
			if (trolleyPackAutoClose()) {
				// autoClose is used more often and operator's attention may be dissipated
				// so we awake him by sound
				Utils.playSound(Utils.SOUND_WARNING);
			}
			break;
		case JOIN:
			infoLabel.setText(getResources().getString("trolley.join.info"));
			image = new Image(getClass().getResource("/resources/images/img-trolley-blue.png").toExternalForm());
			imageView.setImage(image);
			if (trolleyPackAutoClose()) {
				// autoClose is used more often and operator's attention may be dissipated
				// so we awake him by sound
				Utils.playSound(Utils.SOUND_WARNING);
			}
			break;
		}
	}

	@Override
	public void terminate(boolean appIsStopping) {
		super.terminate(appIsStopping);

		stopAutoFirer();

		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}

	public void stopAutoFirer() {
		if (autoFirer != null) {
			autoFirer.stop();
		}

	}

	public void nextClick(ActionEvent event) {
		if (progressVisibleProperty.get()) {
			return;
		}

		stopAutoFirer();

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
				photoButton.setDisable(true);
				photoButton.setVisible(false);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(true); // We can't go further if action failed
				photoButton.setDisable(false);
				photoButton.setVisible(getContext().getAttribute(Const.TAG) instanceof Incoming);

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
				photoButton.setDisable(false);
				photoButton.setVisible(getContext().getAttribute(Const.TAG) instanceof Incoming);

				errorMessageProperty.set(null);
				errorVisibleProperty.set(false);

				TrolleyController.this.done();
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}

	public void makePhotoClick(ActionEvent event) {
		getAction().setNextAction(getAction().getPhotoAction());
		done();
	}

	private boolean trolleyPackAutoClose() {
		try {
			return Configuration.getInstance().getTrolleyPackAutoClose();
		} catch (Exception e) {
			LOG.error(null, e);
		}
		return false;
	}
}
