package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReturnRegistryDeleteController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Registry;

public class ReturnRegistryDeleteAction extends CommonAction<ReturnRegistryDeleteController> {

	@Override
	protected String getFormName() {
		return "return-registry-delete";
	}

	public void process() throws PackLineException {
		Registry registry = (Registry) getContext().getAttribute(Const.REGISTRY);
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.deleteIncomingFromRegistry(registry.getId(), incoming)) {
			throw new PackLineException(getResources().getString("error.post.registry.incoming.delete"));
		}

		// Delete incoming from registry
		registry.getIncoming().remove(incoming);
	}
}
