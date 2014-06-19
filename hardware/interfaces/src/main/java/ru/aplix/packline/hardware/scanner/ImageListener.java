package ru.aplix.packline.hardware.scanner;

import java.awt.Image;

public interface ImageListener {

	void onImageAcquired(Image value);
	
	void onImageAcquisitionCompleted();

	void onImageAcquisitionFailed();
}
