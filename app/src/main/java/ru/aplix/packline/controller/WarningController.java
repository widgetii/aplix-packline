package ru.aplix.packline.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import ru.aplix.packline.Const;
import ru.aplix.packline.action.WarningAction;
import ru.aplix.packline.workflow.SkipActionException;
import ru.aplix.packline.workflow.WorkflowContext;

public class WarningController extends StandardController<WarningAction> {

	@FXML
	private Label infoLabel;

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);

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
		getAction().setNextAction(getAction().getBackAction());
		done();
	}
}
