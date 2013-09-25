package ru.aplix.packline.action;

import ru.aplix.packline.controller.GenStickController;

public class GenStickAction extends CommonAction<GenStickController> {

	@Override
	protected String getFormName() {
		return "sticking";
	}
}
