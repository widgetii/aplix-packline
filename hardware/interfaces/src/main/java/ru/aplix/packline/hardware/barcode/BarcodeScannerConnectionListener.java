package ru.aplix.packline.hardware.barcode;

public interface BarcodeScannerConnectionListener {

	void onConnected();

	void onDisconnected();

	void onConnectionFailed();
}
