package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import ru.aplix.packline.Const;
import ru.aplix.packline.post.Operator;
import ru.aplix.packline.utils.Utils;
import ru.aplix.packline.workflow.StandardWorkflowController;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;

public abstract class StandardController<Action extends WorkflowAction> extends StandardWorkflowController<Action> implements Initializable {

	@FXML
	protected Parent rootNode;

	protected StringProperty titleProperty = new SimpleStringProperty();
	protected StringProperty errorMessageProperty = new SimpleStringProperty();
	protected StringProperty warningMessageProperty = new SimpleStringProperty();
	protected BooleanProperty errorVisibleProperty = new SimpleBooleanProperty();
	protected BooleanProperty progressVisibleProperty = new SimpleBooleanProperty();

	private ResourceBundle resources;
	private WorkflowContext context;

	private Timeline errorChecker;
	private ErrorCheckerEventHandler errorCheckerEventHandler;

	public StandardController() {
		errorCheckerEventHandler = new ErrorCheckerEventHandler();

		errorChecker = new Timeline();
		errorChecker.setCycleCount(Timeline.INDEFINITE);
		errorChecker.getKeyFrames().add(new KeyFrame(Duration.seconds(1), errorCheckerEventHandler));
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.resources = resources;

		// Set title label
		Label titleLabel = (Label) rootNode.lookup("#titleLabel");
		if (titleLabel != null) {
			titleLabel.textProperty().bind(titleProperty);
		}

		// Bind error properties
		Pane errorPane = (Pane) rootNode.lookup("#errorPane");
		if (errorPane != null) {
			errorPane.visibleProperty().bind(errorVisibleProperty);
		}
		final Label errorLabel = (Label) rootNode.lookup("#errorLabel");
		if (errorLabel != null) {
			errorLabel.textProperty().bind(errorMessageProperty);
		}
		final Label warningLabel = (Label) rootNode.lookup("#warningLabel");
		if (warningLabel != null) {
			warningLabel.textProperty().bind(warningMessageProperty);
		}

		errorMessageProperty.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				errorLabel.setVisible(newValue != null);
				warningLabel.setVisible(newValue == null);

				if (newValue != null) {
					Utils.playSound(Utils.SOUND_ERROR);

					errorCheckerEventHandler.reset();
				}
			}
		});
		warningMessageProperty.addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				warningLabel.setVisible(newValue != null);
				errorLabel.setVisible(newValue == null);

				if (newValue != null) {
					Utils.playSound(Utils.SOUND_WARNING);

					errorCheckerEventHandler.reset();
				}
			}
		});

		// Bind progress properties
		ProgressIndicator progressIndicator = (ProgressIndicator) rootNode.lookup("#progressIndicator");
		if (progressIndicator != null) {
			progressIndicator.visibleProperty().bind(progressVisibleProperty);
		}

		// Add key event filter
		rootNode.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				switch (keyEvent.getCode()) {
				case X:
					if (keyEvent.isAltDown()) {
						Platform.exit();
					}
					break;
				default:
					break;
				}
			}
		});
	}

	@Override
	public void prepare(WorkflowContext context) {
		this.context = context;

		Operator operator = (Operator) context.getAttribute(Const.OPERATOR);
		if (operator != null) {
			titleProperty.set(String.format("%s / %s", resources.getString(getTitleResourceName()), operator.getName()));
		} else {
			titleProperty.set(resources.getString(getTitleResourceName()));
		}

		errorMessageProperty.set(null);
		warningMessageProperty.set(null);
		errorVisibleProperty.set(false);
		progressVisibleProperty.set(false);

		errorCheckerEventHandler.reset();
		errorChecker.playFromStart();
	}

	@Override
	public void terminate() {
		errorChecker.stop();
	}

	protected String getTitleResourceName() {
		return "app.title";
	}

	protected ResourceBundle getResources() {
		return resources;
	}

	protected WorkflowContext getContext() {
		return context;
	}

	protected boolean checkNoError() {
		return true;
	}

	/**
	 *
	 */
	private class ErrorCheckerEventHandler implements EventHandler<ActionEvent> {

		private int delayCount;

		public ErrorCheckerEventHandler() {
			reset();
		}

		@Override
		public void handle(ActionEvent event) {
			if (delayCount <= 0) {
				if (checkNoError()) {
					errorMessageProperty.set(null);
					warningMessageProperty.set(null);
					errorVisibleProperty.set(false);
				}
			} else {
				delayCount--;
			}
		}

		public void reset() {
			delayCount = Const.ERROR_DISPLAY_DELAY;
		}
	}
}