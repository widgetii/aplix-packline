package ru.aplix.packline.action;

import ru.aplix.packline.controller.SelectPrintFormsController;

public class SelectPrintFormsAction extends CommonAction<SelectPrintFormsController> {

	@Override
	protected String getFormName() {
		return "print-forms-list";
	}
}
