package ru.aplix.packline.action;

import javax.xml.datatype.DatatypeConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.TrolleyController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.utils.Utils;

public class TrolleyAction extends CommonAction<TrolleyController> {

	@Override
	protected String getFormName() {
		return "trolley";
	}

	public TrolleyType getTrolleyMessage() {
		int current = 0;
		int total = 0;

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		if (order != null) {
			current = order.getIncoming().size() + 1;
			total = Math.max(1, order.getTotalIncomings());
		}

		if (total == 1) {
			return TrolleyType.PACK;
		} else if (current < total) {
			return TrolleyType.KEEP;
		} else {
			return TrolleyType.JOIN;
		}
	}

	public void process() throws PackLineException, DatatypeConfigurationException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		final Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setDate(Utils.now());

		switch (registry.getActionType()) {
		case ADD:
			int res = postServicePort.addIncomingToRegistry(registry.getId(), incoming);
			if (res <= -1) {
				throw new PackLineException(getResources().getString("error.post.incoming.registry.add"));
			}

			order.getIncoming().add(incoming);
			registry.getIncoming().add(incoming);
			break;
		case DELETE:
			// Delete incoming from registry
			if (!postServicePort.deleteIncomingFromRegistry(registry.getId(), incoming)) {
				throw new PackLineException(getResources().getString("error.post.incoming.registry.delete"));
			}

			registry.getIncoming().remove((Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = incoming.getId().equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
					}
					return result;
				}
			}));
			order.getIncoming().remove((Incoming) CollectionUtils.find(registry.getIncoming(), new Predicate() {
				@Override
				public boolean evaluate(Object o) {
					Incoming item = (Incoming) o;
					boolean result = incoming.getId().equals(item.getId());
					if (!result && item.getBarcodes() != null) {
						result = ArrayUtils.contains(item.getBarcodes().toArray(), incoming.getId());
					}
					return result;
				}
			}));
			break;
		}
	}

	/**
	 *
	 */
	public static enum TrolleyType {
		PACK, KEEP, JOIN
	}
}
