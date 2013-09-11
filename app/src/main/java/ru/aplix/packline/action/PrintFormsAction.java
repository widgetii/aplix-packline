package ru.aplix.packline.action;

import ru.aplix.packline.controller.PrintFormsController;

public class PrintFormsAction extends CommonAction<PrintFormsController> {

	@Override
	protected String getFormName() {
		return "printing";
	}
}
