package ru.aplix.packline.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import ru.aplix.packline.Const;
import ru.aplix.packline.model.Operator;
import ru.aplix.packline.workflow.StandardWorkflowController;
import ru.aplix.packline.workflow.WorkflowAction;
import ru.aplix.packline.workflow.WorkflowContext;

public abstract class StandardController<Action extends WorkflowAction> extends StandardWorkflowController<Action> implements Initializable {

	@FXML
	protected Parent rootNode;

	protected StringProperty titleProperty = new SimpleStringProperty();
	protected StringProperty errorMessageProperty = new SimpleStringProperty();
	protected BooleanProperty errorVisibleProperty = new SimpleBooleanProperty();

	private ResourceBundle resources;
	private WorkflowContext context;

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
		Label errorLabel = (Label) rootNode.lookup("#errorLabel");
		if (errorLabel != null) {
			errorLabel.textProperty().bind(errorMessageProperty);
		}

		// Add key event filter
		rootNode.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent keyEvent) {
				switch (keyEvent.getCode()) {
				/*case N:
					if (KeyEvent.KEY_RELEASED.equals(keyEvent.getEventType())) {
						done();
					}
					break;*/
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
}