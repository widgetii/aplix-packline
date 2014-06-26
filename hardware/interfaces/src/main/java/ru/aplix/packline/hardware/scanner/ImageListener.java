package ru.aplix.packline.hardware.scanner;

import java.io.File;

public interface ImageListener {

	void onImageAcquired(File imageFile);

	void onImageAcquisitionCompleted();

	void onImageAcquisitionFailed();
}
