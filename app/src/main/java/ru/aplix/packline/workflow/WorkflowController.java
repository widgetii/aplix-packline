package ru.aplix.packline.workflow;

public interface WorkflowController {

	public void prepare(WorkflowContext context);

	public void terminate(boolean appIsStopping);
}
