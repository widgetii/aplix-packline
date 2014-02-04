package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.PrintFormsController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.workflow.WorkflowAction;

public class PrintFormsAction extends BasePrintFormsAction<PrintFormsController> {

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
		return "printing";
	}

	public void markAsProblem(String code) throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.markAsProblem(container.getId(), code)) {
			throw new PackLineException(getResources().getString("error.post.mark.problem"));
		}
	}

	public boolean processBarcode(String code) throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);

		boolean match = container != null && code != null && code.equals(container.getTrackingId());
		if (match) {
			PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
			if (!postServicePort.markPostAsShipped(container.getId())) {
				throw new PackLineException(getResources().getString("error.post.container.mark.shipped"));
			}

			setNextAction(getNormalAction());
		}
		return match;
	}
}
