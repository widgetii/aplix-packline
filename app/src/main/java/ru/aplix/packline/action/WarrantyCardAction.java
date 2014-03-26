package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.WarrantyCardController;
import ru.aplix.packline.post.Incoming;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.TagType;

public class WarrantyCardAction extends CommonAction<WarrantyCardController> {

	@Override
	protected String getFormName() {
		return "warranty-card";
	}

	public Incoming findIncoming(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		TagType tagType = postServicePort.findTag(code);
		if (!TagType.INCOMING.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		Incoming result = postServicePort.findIncoming(code);
		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		return result;
	}

	public void startFillingWarrantyCard(Incoming incoming, String barcode) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.startFillingWarrantyCard(incoming.getId(), barcode)) {
			throw new PackLineException(getResources().getString("error.post.warranty.filling.start"));
		}
	}

	public void stopFillingWarrantyCard(Incoming incoming, String barcode, boolean filled) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.stopFillingWarrantyCard(incoming.getId(), barcode, filled)) {
			throw new PackLineException(getResources().getString("error.post.warranty.filling.stop"));
		}
	}
}
