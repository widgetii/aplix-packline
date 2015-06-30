package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.OrderActController;
import ru.aplix.packline.post.ActionType;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.workflow.WorkflowAction;

public class OrderActAction extends CommonAction<OrderActController> {

	private WorkflowAction saveActAction;
	private WorkflowAction deleteActAction;
	private WorkflowAction closeActAction;
	private WorkflowAction scanActAction;

	public WorkflowAction getSaveActAction() {
		return saveActAction;
	}

	public void setSaveActAction(WorkflowAction saveActAction) {
		this.saveActAction = saveActAction;
	}

	public WorkflowAction getDeleteActAction() {
		return deleteActAction;
	}

	public void setDeleteActAction(WorkflowAction deleteActAction) {
		this.deleteActAction = deleteActAction;
	}

	public WorkflowAction getCloseActAction() {
		return closeActAction;
	}

	public void setCloseActAction(WorkflowAction closeActAction) {
		this.closeActAction = closeActAction;
	}

	public WorkflowAction getScanActAction() {
		return scanActAction;
	}

	public void setScanActAction(WorkflowAction scanActAction) {
		this.scanActAction = scanActAction;
	}

	@Override
	protected String getFormName() {
		return "order-act";
	}

	public boolean processBarcode(String code) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.INCOMING.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.incoming"));
		}
		final Incoming incoming = postServicePort.findIncoming(code);
		if (incoming == null || incoming.getId() == null || incoming.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		Order order = postServicePort.getOrder(incoming.getOrderId());
		if (order != null && (order.getId() == null || order.getId().length() == 0)) {
			throw new PackLineException(getResources().getString("error.post.invalid.nested.tag"));
		}
		if (registry.getCustomer() == null
				|| (order != null && (order.getCustomer() == null || !registry.getCustomer().getId().equals(order.getCustomer().getId())))) {
			throw new PackLineException(getResources().getString("error.post.incoming.incorrect.customer"));
		}

		Incoming existing;
		switch (registry.getActionType()) {
		case ADD:
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
			}

			break;
		case DELETE:
			// Now we should delete selected incoming from our registry.
			// Check that incoming is present in registry first.
			existing = (Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = incoming.getId().equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
					}
					return result;
				}
			});
			if (existing == null) {
				throw new PackLineException(getResources().getString("error.post.incoming.other.registy"));
			}
			break;
		}

		return true;
	}

	public void carryOutRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (registry.getActionType() == ActionType.DELETE) {
			setNextAction(getScanActAction());
		} else {
			setNextAction(getCloseActAction());
		}

		if (!postServicePort.carryOutRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.carryout"));
		}
	}

	public void saveAct() {
		setNextAction(getSaveActAction());
	}

	public void deleteRegistry() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		setNextAction(getDeleteActAction());

		if (!postServicePort.deleteRegistry(registry.getId())) {
			throw new PackLineException(getResources().getString("error.post.registry.delete"));
		}
	}

	public void deleteIncoming(final Incoming incoming) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteIncomingFromRegistry(registry.getId(), incoming)) {
			throw new PackLineException(getResources().getString("error.post.registry.incoming.delete"));
		}

		// Delete incoming from registry
		registry.getIncoming().remove(incoming);

		// Delete incoming from order as well
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order != null) {
			Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object object) {
					return incoming.getId().equals(((Incoming) object).getId());
				}
			});
			if (existing != null) {
				order.getIncoming().remove(existing);
			}
		}
	}
}
