package ru.aplix.packline.action;

import ru.aplix.packline.controller.PrintFormsBeforeWeightingController;

public class PrintFormsBeforeWeightingAction extends BasePrintFormsAction<PrintFormsBeforeWeightingController> {

	@Override
	protected String getFormName() {
		return "printing-before-weighting";
	}
}
