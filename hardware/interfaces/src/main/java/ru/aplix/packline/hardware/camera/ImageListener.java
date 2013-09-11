package ru.aplix.packline.hardware.camera;

public interface ImageListener {

	void onImageAcquired(PhotoCameraImage value);

	void onImageAcquisitionFailed();
}
