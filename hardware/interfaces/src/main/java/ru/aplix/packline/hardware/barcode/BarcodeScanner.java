package ru.aplix.packline.hardware.barcode;

public interface BarcodeScanner<C extends BarcodeScannerConfiguration> {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();

	void connect();

	void disconnect();

	boolean isConnected();

	void setConnectOnDemand(boolean value);

	boolean getConnectOnDemand();

	void addBarcodeListener(BarcodeListener listener);

	void removeBarcodeListener(BarcodeListener listener);

	void addConnectionListener(BarcodeScannerConnectionListener connectionListener);

	void removeConnectionListener(BarcodeScannerConnectionListener connectionListener);
}
