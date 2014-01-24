package ru.aplix.packline.hardware.camera.flussonic;

import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.camera.DVRCameraConnectionListener;
import ru.aplix.packline.hardware.camera.RecorderListener;

public class FlussonicCameraTestManual {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("No hostname or stream name has been specified.");
			return;
		}

		new FlussonicCameraTestManual(args[0], args[1]).run();
	}

	private String hostName;
	private String streamName;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public FlussonicCameraTestManual(String hostName, String streamName) {
		this.hostName = hostName;
		this.streamName = streamName;
	}

	public void run() {
		try {
			final CountDownLatch connectedLatch = new CountDownLatch(1);
			final CountDownLatch disconnectedLatch = new CountDownLatch(1);
			final CountDownLatch recordingStartedLatch = new CountDownLatch(1);
			final CountDownLatch recordingStoppedLatch = new CountDownLatch(1);

			FlussonicCameraConfiguration configuration = new FlussonicCameraConfiguration();
			configuration.setStreamName(streamName);
			configuration.setHostName(hostName);
			configuration.setTimeout(2000);

			FlussonicCamera camera = new FlussonicCamera();
			camera.setConfiguration(configuration);
			camera.addRecorderListener(new RecorderListener() {
				@Override
				public void onRecordingStarted() {
					System.out.println("Recording started");
					recordingStartedLatch.countDown();
				}

				@Override
				public void onRecordingStopped() {
					System.out.println("Recording stopped");
					recordingStoppedLatch.countDown();
				}

				@Override
				public void onRecordingFailed() {
					System.out.println("Recording failed");
					recordingStartedLatch.countDown();
					recordingStoppedLatch.countDown();
				}
			});
			camera.addConnectionListener(new DVRCameraConnectionListener() {
				@Override
				public void onConnected() {
					System.out.println("Connected");
					connectedLatch.countDown();
				}

				@Override
				public void onDisconnected() {
					System.out.println("Disconnected");
					disconnectedLatch.countDown();
				}

				@Override
				public void onConnectionFailed() {
					System.out.println("Connection failed");
					connectedLatch.countDown();
					disconnectedLatch.countDown();
				}
			});

			System.out.println(String.format("Conneting to %s...", hostName));
			camera.connect();
			connectedLatch.await();
			if (camera.isConnected()) {
				camera.enableRecording();
				recordingStartedLatch.await();

				if (camera.isRecording()) {
					Thread.sleep(3000);

					camera.disableRecording();
					recordingStoppedLatch.await();
				}

				camera.disconnect();
				disconnectedLatch.await();
			}

			System.out.println("Program terminated");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
