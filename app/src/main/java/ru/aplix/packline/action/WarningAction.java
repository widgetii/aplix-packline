package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.WarningController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
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

	public void process() throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);
		String code = (String) getContext().getAttribute(Const.WARNING_CODE);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.markAsProblem(container.getId(), code)) {
			throw new PackLineException(getResources().getString("error.post.mark.problem"));
		}

		setNextAction(getBackAction());
	}
}
