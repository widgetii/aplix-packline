package ru.aplix.packline.action;

import ru.aplix.packline.controller.WarningController;
import ru.aplix.packline.workflow.WorkflowAction;

public class WarningAction extends CommonAction<WarningController> {

	private WorkflowAction backAction;
	private WorkflowAction normalAction;

	@Override
	protected String getFormName() {
		return "warning";
	}

	public WorkflowAction getBackAction() {
		return backAction;
	}

	public void setBackAction(WorkflowAction backAction) {
		this.backAction = backAction;
	}

	public WorkflowAction getNormalAction() {
		return normalAction;
	}

	public void setNormalAction(WorkflowAction normalAction) {
		this.normalAction = normalAction;
	}
}
