package ru.aplix.packline.controller;

import javafx.event.ActionEvent;
import ru.aplix.packline.action.GenStickAction;
import ru.aplix.packline.workflow.WorkflowContext;

public class GenStickController extends StandardController<GenStickAction> {

	@Override
	public void prepare(WorkflowContext context) {
		super.prepare(context);
	}

	@Override
	public void terminate() {
	}

	public void completeClick(ActionEvent event) {
		done();
	}
}
