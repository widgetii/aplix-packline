package ru.aplix.packline.hardware.camera;

public interface PhotoCamera<C extends PhotoCameraConfiguration> {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();

	void connect();

	void disconnect();

	boolean isConnected();

	void setConnectOnDemand(boolean value);

	boolean getConnectOnDemand();

	void addConnectionListener(PhotoCameraConnectionListener connectionListener);

	void removeConnectionListener(PhotoCameraConnectionListener connectionListener);

	void makePhoto();

	void addImageListener(ImageListener listener);

	void removeImageListener(ImageListener listener);
}
