package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.WeightingBoxController;
import ru.aplix.packline.post.Container;
import ru.aplix.packline.post.PackingLinePortType;

public class WeightingBoxAction extends CommonAction<WeightingBoxController> {

	@Override
	protected String getFormName() {
		return "weighting-box";
	}

	public void processMeasure(Float value) throws PackLineException {
		Container container = (Container) getContext().getAttribute(Const.TAG);
		container.setTotalWeight(value);

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		if (!postServicePort.updateContainer(container)) {
			throw new PackLineException(getResources().getString("error.post.container.update"));
		}
	}
}
