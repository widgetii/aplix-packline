package ru.aplix.packline.hardware.camera.raspberry;

import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.camera.ImageListener;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;

public class RaspberryCameraTestManual {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("No hostname has been specified.");
			return;
		}

		new RaspberryCameraTestManual(args[args.length - 1]).run();
	}

	private String hostName;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public RaspberryCameraTestManual(String hostName) {
		this.hostName = hostName;
	}

	public void run() {
		try {
			final CountDownLatch connectLatch = new CountDownLatch(1);
			final CountDownLatch photoLatch = new CountDownLatch(1);

			RaspberryCameraConfiguration configuration = new RaspberryCameraConfiguration();
			configuration.setHostName(hostName);
			configuration.setEnabled(true);

			RaspberryCamera camera = new RaspberryCamera();
			camera.setConfiguration(configuration);
			camera.addImageListener(new ImageListener() {

				public void onImageAcquired(PhotoCameraImage value) {
					System.out.println(String.format("Image acquired: %s", value.getImageId()));
					photoLatch.countDown();
				}

				public void onImageAcquisitionFailed() {
					System.out.println("ERROR: Image acquisition failed.");
					photoLatch.countDown();
				}
			});
			camera.addConnectionListener(new PhotoCameraConnectionListener() {
				public void onConnected() {
					connectLatch.countDown();
				}

				public void onDisconnected() {
					connectLatch.countDown();
				}

				public void onConnectionFailed() {
					connectLatch.countDown();
				}
			});

			System.out.println(String.format("Conneting to %s...", hostName));
			camera.connect();
			connectLatch.await();
			if (camera.isConnected()) {
				System.out.println("Connected");

				camera.makePhoto();
				photoLatch.await();

				camera.disconnect();
			}

			System.out.println("Program terminated.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
