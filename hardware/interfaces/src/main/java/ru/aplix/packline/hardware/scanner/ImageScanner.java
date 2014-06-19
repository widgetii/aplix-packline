package ru.aplix.packline.hardware.scanner;

import ru.aplix.packline.hardware.Connectable;

public interface ImageScanner<C extends ImageScannerConfiguration> extends Connectable {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();

	void setConnectOnDemand(boolean value);

	boolean getConnectOnDemand();

	void addConnectionListener(ImageScannerConnectionListener connectionListener);

	void removeConnectionListener(ImageScannerConnectionListener connectionListener);

	void acquireImage();

	void addImageListener(ImageListener listener);

	void removeImageListener(ImageListener listener);
}
