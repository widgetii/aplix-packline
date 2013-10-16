package ru.aplix.packline.action;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.OrderActController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;

public class OrderActAction extends CommonAction<OrderActController> {

	@Override
	protected String getFormName() {
		return "order-act";
	}

	public boolean processBarcode(String code) throws PackLineException {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.INCOMING.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.incoming"));
		}
		final Incoming incoming = postServicePort.findIncoming(code);
		if (incoming == null || !code.equals(incoming.getId())) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		if (!order.getId().equals(incoming.getOrderId())) {
			throw new PackLineException(getResources().getString("error.post.incoming.incorrect.order"));
		}

		Incoming existing = (Incoming) CollectionUtils.find(order.getIncoming(), new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return incoming.getId().equals(((Tag) item).getId());
			}
		});
		if (existing != null) {
			throw new PackLineException(getResources().getString("error.post.incoming.registered"));
		}

		return true;
	}

	public void carryOutOrder() throws PackLineException {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.carryOutOrder(order.getId())) {
			throw new PackLineException(getResources().getString("error.post.order.carryout"));
		}
	}

	public void deleteOrder() throws PackLineException {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteOrder(order.getId())) {
			throw new PackLineException(getResources().getString("error.post.order.delete"));
		}
	}

	public void deleteIncoming(Incoming incoming) throws PackLineException {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteIncomingFromOrder(order.getId(), incoming.getId())) {
			throw new PackLineException(getResources().getString("error.post.order.incoming.delete"));
		}

		order.getIncoming().remove(incoming);
	}
}
