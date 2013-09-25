package ru.aplix.packline.hardware.barcode;

import ru.aplix.packline.hardware.Connectable;

public interface BarcodeScanner<C extends BarcodeScannerConfiguration> extends Connectable {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();
	
	void setConnectOnDemand(boolean value);

    boolean getConnectOnDemand();

	void addBarcodeListener(BarcodeListener listener);

	void removeBarcodeListener(BarcodeListener listener);

	void addConnectionListener(BarcodeScannerConnectionListener connectionListener);

	void removeConnectionListener(BarcodeScannerConnectionListener connectionListener);
}
