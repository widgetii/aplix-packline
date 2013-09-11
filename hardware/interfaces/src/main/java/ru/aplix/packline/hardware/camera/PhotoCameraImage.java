package ru.aplix.packline.hardware.camera;

import java.awt.Image;

public class PhotoCameraImage {

	private String imageId;
	private Image source;

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public Image getSource() {
		return source;
	}

	public void setSource(Image source) {
		this.source = source;
	}
}
