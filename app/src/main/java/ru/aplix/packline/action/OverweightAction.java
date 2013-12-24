package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.OverweightController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.WorkflowAction;

public class OverweightAction extends CommonAction<OverweightController> {

	private WorkflowAction weightingAction;
	private WorkflowAction normalAction;

	public WorkflowAction getWeightingAction() {
		return weightingAction;
	}

	public void setWeightingAction(WorkflowAction weightingAction) {
		this.weightingAction = weightingAction;
	}

	public WorkflowAction getNormalAction() {
		return normalAction;
	}

	public void setNormalAction(WorkflowAction normalAction) {
		this.normalAction = normalAction;
	}

	@Override
	protected String getFormName() {
		return "overweight";
	}

	public void process() throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.markAsProblem(container.getId(), "Overweight")) {
			throw new PackLineException(getResources().getString("error.post.mark.problem"));
		}

		setNextAction(getNormalAction());
	}
}
