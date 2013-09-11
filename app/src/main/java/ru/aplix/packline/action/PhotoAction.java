package ru.aplix.packline.action;

import ru.aplix.packline.controller.PhotoController;

public class PhotoAction extends CommonAction<PhotoController> {

	@Override
	protected String getFormName() {
		return "photo";
	}
}
