package ru.aplix.packline.action;

import javax.xml.datatype.DatatypeConfigurationException;

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
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setDate(Utils.now());

		Order order = (Order) getContext().getAttribute(Const.ORDER);
		order.getIncoming().add(incoming);

		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		registry.getIncoming().add(incoming);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		int res = postServicePort.addIncomingToRegistry(registry.getId(), incoming);
		if (res <= -1) {
			throw new PackLineException(getResources().getString("error.post.incoming.registry"));
		}
	}

	public static enum TrolleyType {
		PACK, KEEP, JOIN
	}
}
