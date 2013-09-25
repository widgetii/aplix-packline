package ru.aplix.packline.hardware.camera;

import ru.aplix.packline.hardware.Connectable;

public interface PhotoCamera<C extends PhotoCameraConfiguration> extends Connectable {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();
	
	void setConnectOnDemand(boolean value);

    boolean getConnectOnDemand();

	void addConnectionListener(PhotoCameraConnectionListener connectionListener);

	void removeConnectionListener(PhotoCameraConnectionListener connectionListener);

	void makePhoto();

	void addImageListener(ImageListener listener);

	void removeImageListener(ImageListener listener);
}
