package ru.aplix.packline.hardware.scanner;

public interface ImageScannerConnectionListener {

	void onConnected();

	void onDisconnected();

	void onConnectionFailed();
}
