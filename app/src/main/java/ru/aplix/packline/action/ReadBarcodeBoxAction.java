package ru.aplix.packline.action;

import ru.aplix.packline.controller.ReadBarcodeBoxController;
import ru.aplix.packline.model.Order;
import ru.aplix.packline.model.PackingSize;

public class ReadBarcodeBoxAction extends CommonAction<ReadBarcodeBoxController> {

	@Override
	protected String getFormName() {
		return "barcode-box";
	}

	public boolean processBarcode(String code, Order order) {
		// TODO: place processing code here
		order.getPacking().setPackingCode(code);
		order.getPacking().setPackingSize(new PackingSize(120f, 100f, 90f));

		return true;
	}
}
