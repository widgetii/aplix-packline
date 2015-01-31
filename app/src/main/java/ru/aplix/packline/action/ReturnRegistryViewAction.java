package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

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
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		Incoming existing = null;
		switch (registry.getActionType()) {
		case ADD:
			final Incoming incoming = postServicePort.findIncoming2(code);
			if (incoming == null || incoming.getId() == null || incoming.getId().length() == 0) {
				throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
			}

			// Now we should add a new incoming to our registry.
			// Check that it hasn't been added yet.
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object item) {
					return incoming.getId().equals(((Tag) item).getId());
				}
			});
			if (existing != null) {
				throw new PackLineException(getResources().getString("error.post.incoming.registered"));
			} else {
				existing = incoming;
			}

			break;
		case DELETE:
			// Now we should delete selected incoming from our registry.
			// Check that incoming is present in registry first.
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = code.equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), code);
					}
					return result;
				}
			});
			if (existing == null) {
				throw new PackLineException(getResources().getString("error.post.incoming.other.registy"));
			}
			break;
		}

		getContext().setAttribute(Const.ORDER, null);
		getContext().setAttribute(Const.TAG, existing);

		return true;
	}

	public void carryOutRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		String res = postServicePort.carryOutRegistry2(registry.getId());
		if (res != null && res.length() > 0) {
			throw new PackLineException(res);
		}
	}

	public void deleteRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteRegistry2(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.delete"));
		}
	}
}
