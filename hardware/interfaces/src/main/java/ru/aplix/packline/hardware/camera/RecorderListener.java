package ru.aplix.packline.hardware.camera;

public interface RecorderListener {

	void onRecordingStarted();

	void onRecordingStopped();

	void onRecordingFailed();
}
