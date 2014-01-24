package ru.aplix.packline.hardware.camera;

import ru.aplix.packline.hardware.Connectable;

public interface DVRCamera<C extends DVRCameraConfiguration> extends Connectable {

	String getName();

	void setConfiguration(String config) throws IllegalArgumentException;

	void setConfiguration(C config) throws IllegalArgumentException;

	C getConfiguration();

	void setConnectOnDemand(boolean value);

	boolean getConnectOnDemand();

	void addConnectionListener(DVRCameraConnectionListener connectionListener);

	void removeConnectionListener(DVRCameraConnectionListener connectionListener);

	void enableRecording();

	void disableRecording();

	boolean isRecording();

	void addRecorderListener(RecorderListener listener);

	void removeRecorderListener(RecorderListener listener);
}
