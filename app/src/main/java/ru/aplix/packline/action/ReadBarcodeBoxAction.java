package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.ReadBarcodeBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingSize;
import ru.aplix.packline.post.Post;

public class ReadBarcodeBoxAction extends CommonAction<ReadBarcodeBoxController> {

	@Override
	protected String getFormName() {
		return "barcode-box";
	}

	public void processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		PackingSize ps = postServicePort.getBoxSize(code);
		if (ps == null) {
			throw new PackLineException(getResources().getString("error.post.container.size"));
		}

		Post post = (Post) getContext().getAttribute(Const.TAG);
		Container container = post.getContainer();

		container.setId(code);
		container.setPackingSize(ps);

		if (!postServicePort.addContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.add"));
		}
	}
}
