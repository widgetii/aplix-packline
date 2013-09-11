package ru.aplix.packline.action;

import ru.aplix.packline.controller.OrderActController;

public class OrderActAction extends CommonAction<OrderActController> {

	@Override
	protected String getFormName() {
		return "order-act";
	}

	public boolean processBarcode(String code) {
		if ("0123456789012".equals(code)) {
			return true;
		} else
			return false;
	}

	public void closeAct() {

	}

	public void saveAct() {

	}
}
