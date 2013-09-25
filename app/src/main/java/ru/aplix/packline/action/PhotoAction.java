package ru.aplix.packline.action;

import ru.aplix.packline.controller.PhotoController;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;

public class PhotoAction extends CommonAction<PhotoController> {

	@Override
	protected String getFormName() {
		return "photo";
	}

	public void imageAcquired(PhotoCameraImage result) {
		// TODO: place image processing logic here
	}
}
