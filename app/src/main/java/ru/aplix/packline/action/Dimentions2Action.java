package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.Dimentions2Controller;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.PackingSize;
import ru.aplix.packline.post.UpdateContainerResponse2;

public class Dimentions2Action extends CommonAction<Dimentions2Controller> {

	@Override
	protected String getFormName() {
		return "dimentions2";
	}

	public void process(float length, float height, float width) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);
		Container container = (Container) getContext().getAttribute(Const.TAG);

		PackingSize ps = new PackingSize();
		ps.setLength(length);
		ps.setWidth(width);
		ps.setHeight(height);
		container.setPackingSize(ps);

		UpdateContainerResponse2 response = postServicePort.updateContainer2(container);
		if (response == null) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		} else if (response.getError() != null && response.getError().length() > 0) {
			throw new PackLineException(response.getError());
		} else if (response.getContainer() == null) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		}

		// Update container in our context
		container = response.getContainer();
		getContext().setAttribute(Const.TAG, container);
	}
}
