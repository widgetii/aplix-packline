package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReturnRegistryViewController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.workflow.WorkflowAction;

public class ReturnRegistryViewAction extends CommonAction<ReturnRegistryViewController> {

	private WorkflowAction weightingAction;
	private WorkflowAction backAction;

	public WorkflowAction getWeightingAction() {
		return weightingAction;
	}

	public void setWeightingAction(WorkflowAction weightingAction) {
		this.weightingAction = weightingAction;
	}

	public WorkflowAction getBackAction() {
		return backAction;
	}

	public void setBackAction(WorkflowAction backAction) {
		this.backAction = backAction;
	}

	@Override
	protected String getFormName() {
		return "return-registry-view";
	}

	public boolean processBarcode(final String code) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		Incoming existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return code.equals(((Tag) item).getId());
			}
		});
		if (existing == null) {
			throw new PackLineException(getResources().getString("error.post.incoming.other.registy"));
		}

		getContext().setAttribute(Const.ORDER, null);
		getContext().setAttribute(Const.TAG, existing);

		return true;
	}

	public void carryOutRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.carryOutRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.carryout"));
		}
	}

	public void deleteRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.delete"));
		}
	}
}
