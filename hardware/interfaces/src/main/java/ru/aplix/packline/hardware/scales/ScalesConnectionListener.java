package ru.aplix.packline.hardware.scales;

public interface ScalesConnectionListener {

	void onConnected();

	void onDisconnected();

	void onConnectionFailed();
}
