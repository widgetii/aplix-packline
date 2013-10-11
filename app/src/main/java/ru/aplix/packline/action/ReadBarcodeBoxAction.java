package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingType;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.Tag;

public class ReadBarcodeBoxAction extends CommonAction<ReadBarcodeBoxController> {

	@Override
	protected String getFormName() {
		return "barcode-box";
	}

	public void processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Tag tag = postServicePort.findTag(code);
		if (tag == null || !code.equals(tag.getId())) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		if (!(tag instanceof Container)) {
			throw new PackLineException(getResources().getString("error.post.not.box.container"));
		}
		Container emptyBox = (Container) tag;
		if (!PackingType.BOX.equals(emptyBox.getPackingType())) {
			throw new PackLineException(getResources().getString("error.post.not.box.container"));
		}
		if (emptyBox.getPostId() != null) {
			throw new PackLineException(getResources().getString("error.post.container.incorrect.post"));
		}

		Post post = (Post) getContext().getAttribute(Const.TAG);
		Container container = post.getContainer();

		container.setId(code);
		container.setPackingSize(emptyBox.getPackingSize());
		container.setBoxTypeId(emptyBox.getBoxTypeId());

		if (!postServicePort.addContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.add"));
		}
	}
}
