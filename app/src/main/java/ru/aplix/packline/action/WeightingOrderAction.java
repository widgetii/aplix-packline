package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.controller.WeightingOrderController;
import ru.aplix.packline.post.Incoming;

public class WeightingOrderAction extends CommonAction<WeightingOrderController> {

	@Override
	protected String getFormName() {
		return "weighting-order";
	}

	public void processMeasure(Float value) {
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setWeight(value);
	}
}
