package ru.aplix.packline.workflow;

public abstract class StandardWorkflowController<Action extends WorkflowAction> implements WorkflowController {

	private DoneListener doneListener;
	private Action action;

	public DoneListener getDoneListener() {
		return doneListener;
	}

	public void setDoneListener(DoneListener doneListener) {
		this.doneListener = doneListener;
	}

	public void setAction(Action value) {
		this.action = value;
	}

	public Action getAction() {
		return this.action;
	}

	protected void done() {
		if (doneListener != null) {
			doneListener.done();
		}
	}
}
