package ru.aplix.packline.action;

import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.SelectPickupRequestsController;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PickupRequest;
import ru.aplix.packline.post.PickupRequestList;
import ru.aplix.packline.post.Registry;
import ru.aplix.packline.utils.Utils;

public class SelectPickupRequestsAction extends CommonAction<SelectPickupRequestsController> {

	@Override
	protected String getFormName() {
		return "pickup-requests-list";
	}

	public List<PickupRequest> loadPickupRequests() throws DatatypeConfigurationException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		PickupRequestList list = postServicePort.getPickupRequests(registry.getCustomer().getId(), Utils.now());
		return list != null ? list.getItems() : null;
	}

	public void bindRegistryWithPickupRequest(PickupRequest pickupRequest) throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		String result = postServicePort.bindRegistryWithPickupRequest(registry.getId(), pickupRequest.getId());
		if (result != null && result.length() > 0) {
			throw new PackLineException(result);
		}
	}
}
