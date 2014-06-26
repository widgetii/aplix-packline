package ru.aplix.packline.hardware.scanner.morena;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.scanner.ImageListener;
import ru.aplix.packline.hardware.scanner.ImageScannerConnectionListener;

public class MorenaScannerTestManual {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("No scanner name has been specified.");
			return;
		}

		new MorenaScannerTestManual(args[0]).run();
	}

	private String name;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public MorenaScannerTestManual(String name) {
		this.name = name;
	}

	public void run() {
		try {
			final CountDownLatch connectLatch = new CountDownLatch(1);
			final CountDownLatch photoLatch = new CountDownLatch(1);

			MorenaScannerConfiguration configuration = new MorenaScannerConfiguration();
			configuration.setName(name);
			configuration.setFunctionalUnit(FunctionalUnit.FEEDER);

			MorenaScanner scanner = new MorenaScanner();
			scanner.setConfiguration(configuration);
			scanner.addImageListener(new ImageListener() {

				@Override
				public void onImageAcquired(File imageFile) {
					System.out.println("Image acquired");
				}

				@Override
				public void onImageAcquisitionCompleted() {
					photoLatch.countDown();
				}

				@Override
				public void onImageAcquisitionFailed() {
					System.out.println("ERROR: Image acquisition failed.");
					photoLatch.countDown();
				}
			});
			scanner.addConnectionListener(new ImageScannerConnectionListener() {
				@Override
				public void onConnected() {
					connectLatch.countDown();
				}

				@Override
				public void onDisconnected() {
					connectLatch.countDown();
				}

				@Override
				public void onConnectionFailed() {
					connectLatch.countDown();
				}
			});

			System.out.println(String.format("Conneting to %s...", name));
			scanner.connect();
			connectLatch.await();
			if (scanner.isConnected()) {
				System.out.println("Connected");

				scanner.acquireImage();
				photoLatch.await();

				scanner.disconnect();
			}

			System.out.println("Program terminated.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
