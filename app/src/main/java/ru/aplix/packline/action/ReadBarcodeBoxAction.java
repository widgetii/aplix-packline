package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.TagType;
import ru.aplix.packline.utils.Utils;

public class ReadBarcodeBoxAction extends NotificationAction<ReadBarcodeBoxController> {

	@Override
	protected String getFormName() {
		return "barcode-box";
	}

	public int processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.CONTAINER.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.container"));
		}
		Container emptyBox = postServicePort.findContainer(code);
		if (emptyBox == null || emptyBox.getId() == null || emptyBox.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		if (!PackingType.BOX.equals(emptyBox.getPackingType())) {
			throw new PackLineException(getResources().getString("error.post.not.box.container"));
		}
		if (Utils.isPackingSizeEmpty(emptyBox.getPackingSize())) {
			throw new PackLineException(String.format(getResources().getString("error.post.box.invalid.size"), code));
		}
		if (emptyBox.getPostId() != null && emptyBox.getPostId().length() > 0) {
			throw new PackLineException(getResources().getString("error.post.container.incorrect.post"));
		}
		if (emptyBox.isShipped()) {
			throw new PackLineException(getResources().getString("error.post.container.shipped"));
		}

		Post post = (Post) getContext().getAttribute(Const.TAG);
		Container container = post.getContainer();

		container.setId(code);
		container.setPackingSize(emptyBox.getPackingSize());
		container.setBoxTypeId(emptyBox.getBoxTypeId());

		if (!postServicePort.addContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.add"));
		}

		notifyAboutIncomingParcel(container.getId());
		
		return postServicePort.getBoxCount(container.getBoxTypeId());
	}
}
