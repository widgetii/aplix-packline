package ru.aplix.packline.hardware.camera;

public interface PhotoCameraConnectionListener {

	void onConnected();

	void onDisconnected();

	void onConnectionFailed();
}
