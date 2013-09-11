package ru.aplix.packline.action;

import ru.aplix.packline.controller.DimentionsController;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.model.PackingSize;

public class DimentionsAction extends CommonAction<DimentionsController> {

	@Override
	protected String getFormName() {
		return "dimentions";
	}

	public boolean processBarcode(String code, Order order, float length, float height, float width) {
		// TODO: place processing code here
		order.getPacking().setPackingCode(code);
		order.getPacking().setPackingSize(new PackingSize(length, height, width));

		return true;
	}
}
