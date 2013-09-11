package ru.aplix.packline.action;

import ru.aplix.packline.controller.WeightingBoxController;
import ru.aplix.packline.model.Order;

public class WeightingBoxAction extends CommonAction<WeightingBoxController> {

	@Override
	protected String getFormName() {
		return "weighting-box";
	}

	public void processMeasure(Float value, Order order) {
		order.getPacking().setTotalWeight(value);
	}
}
