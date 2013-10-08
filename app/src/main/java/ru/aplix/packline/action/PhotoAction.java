package ru.aplix.packline.action;

import ru.aplix.packline.Const;
import ru.aplix.packline.controller.PhotoController;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;
import ru.aplix.packline.post.Incoming;

public class PhotoAction extends CommonAction<PhotoController> {

	@Override
	protected String getFormName() {
		return "photo";
	}

	public void imageAcquired(PhotoCameraImage result) {
		Incoming incoming = (Incoming) getContext().getAttribute(Const.TAG);
		incoming.setPhotoId(result.getImageId());
	}
}
