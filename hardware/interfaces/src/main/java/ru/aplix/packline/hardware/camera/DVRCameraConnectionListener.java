package ru.aplix.packline.hardware.camera;

public interface DVRCameraConnectionListener {

	void onConnected();

	void onDisconnected();

	void onConnectionFailed();
}
