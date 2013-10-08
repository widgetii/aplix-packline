package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.action.TrolleyAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class TrolleyController extends StandardController<TrolleyAction> {

	private final Log LOG = LogFactory.getLog(getClass());

	@FXML
	private Label infoLabel;
	@FXML
	private ImageView imageView;
	@FXML
	private Button nextButton;

	private ResourceBundle resources;

	private Task<?> task;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		super.initialize(location, resources);

		this.resources = resources;
	}

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

		nextButton.setDisable(false);

		switch (getAction().getTrolleyMessage()) {
		case PACK:
			infoLabel.setText(resources.getString("trolley.pack.info"));
			Image image = new Image(getClass().getResource("/resources/images/img-trolley-red.png").toExternalForm());
			imageView.setImage(image);
			break;
		case KEEP:
			infoLabel.setText(resources.getString("trolley.keep.info"));
			image = new Image(getClass().getResource("/resources/images/img-trolley-green.png").toExternalForm());
			imageView.setImage(image);
			break;
		case JOIN:
			infoLabel.setText(resources.getString("trolley.join.info"));
			image = new Image(getClass().getResource("/resources/images/img-trolley-blue.png").toExternalForm());
			imageView.setImage(image);
			break;
		}

		doAction();
	}

	@Override
	public void terminate() {
		if (task != null) {
			task.cancel(false);
			task = null;
		}
	}

	public void nextClick(ActionEvent event) {
		done();
	}

	private void doAction() {
		task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				try {
					getAction().process();
				} catch (Exception e) {
					LOG.error(e);
					throw e;
				}
				return null;
			}

			@Override
			protected void running() {
				super.running();

				progressVisibleProperty.set(true);
				nextButton.setDisable(true);
			}

			@Override
			protected void failed() {
				super.failed();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);

				String errorStr;
				if (getException() instanceof PackLineException) {
					errorStr = getException().getMessage();
				} else {
					errorStr = resources.getString("error.post.service");
				}

				errorMessageProperty.set(errorStr);
				errorVisibleProperty.set(true);
			}

			@Override
			protected void succeeded() {
				super.succeeded();

				progressVisibleProperty.set(false);
				nextButton.setDisable(false);
			}
		};

		ExecutorService executor = (ExecutorService) getContext().getAttribute(Const.EXECUTOR);
		executor.submit(task);
	}
}
