package ru.aplix.packline.action;

import javax.xml.datatype.DatatypeConfigurationException;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.TrolleyController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.Order;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.utils.Utils;

public class TrolleyAction extends CommonAction<TrolleyController> {

	@Override
	protected String getFormName() {
		return "trolley";
	}

	public TrolleyType getTrolleyMessage() {
		Order order = (Order) getContext().getAttribute(Const.ORDER);
		int current = order.getIncoming().size() + 1;
		int total = Math.max(1, order.getTotalIncomings());

		if (total == 1) {
			return TrolleyType.PACK;
		} else if (current < total) {
			return TrolleyType.KEEP;
		} else {
			return TrolleyType.JOIN;
		}
	}

	public void process() throws PackLineException, DatatypeConfigurationException {
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setDate(Utils.now());

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		order.getIncoming().add(incoming);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		int res = postServicePort.addIncomingToOrder(incoming.getOrderId(), incoming);
		if (res <= -1) {
			throw new PackLineException(getResources().getString("error.post.incoming.order"));
		}
	}

	public static enum TrolleyType {
		PACK, KEEP, JOIN
	}
}
