package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.PackLineException;
import ru.aplix.packline.controller.GenStickCustomerController;
import ru.aplix.packline.post.Customer;
import ru.aplix.packline.post.PackingLinePortType;
import ru.aplix.packline.post.TagList;

public class GenStickCustomerAction extends CommonAction<GenStickCustomerController> {

	@Override
	protected String getFormName() {
		return "sticking-customer";
	}

	public Customer processBarcode(String code) throws PackLineException {
		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		Customer result = postServicePort.getCustomer(code);
		if (result == null || result.getId() == null) {
			throw new PackLineException(getResources().getString("error.barcode.invalid.code"));
		}

		return result;
	}

	public void generateAndPrint(int count, String customerCode) throws PackLineException {
		if (customerCode == null) {
			throw new PackLineException(getResources().getString("error.customer.not.selected"));
		}

		PackingLinePortType postServicePort = (PackingLinePortType) getContext().getAttribute(Const.POST_SERVICE_PORT);

		TagList tagList = postServicePort.generateTagsForIncomings(customerCode, count);
		if (tagList == null) {
			throw new PackLineException(getResources().getString("error.post.generate.tags"));
		}

		for (int i = 0; i < count; i++) {
			// TODO: print stickers
		}
	}
}
