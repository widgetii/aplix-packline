package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.WarrantyCardController;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.Tag;
import ru.aplix.packline.post.TagType;

public class WarrantyCardAction extends CommonAction<WarrantyCardController> {

	@Override
	protected String getFormName() {
		return "warranty-card";
	}

	public Tag findTag(String code) throws PackLineException {
		Tag result;

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		TagType tagType = postServicePort.findTag(code);

		if (TagType.INCOMING.equals(tagType)) {
			result = postServicePort.findIncoming(code);
		} else if (TagType.POST.equals(tagType)) {
			result = postServicePort.findPost(code);
		} else {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		if (result == null || result.getId() == null || result.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		return result;
	}

	public void startFillingWarrantyCard(Tag tag, String barcode) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.startFillingWarrantyCard(tag.getId(), barcode != null ? barcode : "")) {
			throw new PackLineException(getResources().getString("error.post.warranty.filling.start"));
		}
	}

	public void stopFillingWarrantyCard(Tag tag, String barcode, boolean filled) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		if (!postServicePort.stopFillingWarrantyCard(tag.getId(), barcode != null ? barcode : "", filled)) {
			throw new PackLineException(getResources().getString("error.post.warranty.filling.stop"));
		}
	}
}
