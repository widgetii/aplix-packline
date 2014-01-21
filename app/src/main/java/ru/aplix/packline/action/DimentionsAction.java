package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.DimentionsController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingSize;
import ru.aplix.packline.post.Post;
import ru.aplix.packline.post.TagType;

public class DimentionsAction extends NotificationAction<DimentionsController> {

	@Override
	protected String getFormName() {
		return "dimentions";
	}

	public void processBarcode(String code, float length, float height, float width) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagType tagType = postServicePort.findTag(code);
		if (!TagType.CONTAINER.equals(tagType)) {
			throw new PackLineException(getResources().getString("error.post.not.container"));
		}
		Container emptyContainer = postServicePort.findContainer(code);
		if (emptyContainer == null || emptyContainer.getId() == null || emptyContainer.getId().length() == 0) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}
		if (emptyContainer.getPostId() != null && emptyContainer.getPostId().length() > 0) {
			throw new PackLineException(getResources().getString("error.post.container.incorrect.post"));
		}
		if (emptyContainer.isShipped()) {
			throw new PackLineException(getResources().getString("error.post.container.shipped"));
		}

		Post post = (Post) getContext().getAttribute(Const.TAG);
		Container container = post.getContainer();

		container.setId(code);
		PackingSize ps = new PackingSize();
		ps.setLength(length);
		ps.setWidth(width);
		ps.setHeight(height);
		container.setPackingSize(ps);

		if (!postServicePort.addContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.add"));
		}
		
		notifyAboutIncomingParcel(container.getId());
	}
}
