package ru.aplix.packline.action;

import ru.aplix.packline.controller.WeightingOrderController;
import ru.aplix.packline.model.Order;

public class WeightingOrderAction extends CommonAction<WeightingOrderController> {

	@Override
	protected String getFormName() {
		return "weighting-order";
	}

	public void processMeasure(Float value, Order order) {
		order.setWeight(value);
	}
}
