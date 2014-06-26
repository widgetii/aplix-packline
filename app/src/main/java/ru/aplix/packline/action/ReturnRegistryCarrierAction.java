package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.controller.ReturnRegistryCarrierController;

public class ReturnRegistryCarrierAction extends CommonAction<ReturnRegistryCarrierController> {

	@Override
	protected String getFormName() {
		return "return-registry-carrier";
	}

	public void select(String value) {
		getContext().setAttribute(Const.SELECTED_CARRIER, value);
	}
}
