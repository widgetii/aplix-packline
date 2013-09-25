package ru.aplix.packline.hardware.barcode.basicRS232;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScannerConnectionListener;

public class BasicRS232BarcodeScannerTestManual {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("No COM port has been specified.");
			return;
		}

		new BasicRS232BarcodeScannerTestManual(args[args.length - 1]).run();
	}

	private String portName;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public BasicRS232BarcodeScannerTestManual(String portName) {
		this.portName = portName;
	}

	public void run() {
		try {
			final CountDownLatch connectLatch = new CountDownLatch(1);

			RS232Configuration configuration = new RS232Configuration();
			configuration.setPortName(portName);
			configuration.setPortSpeed(115200);

			BasicRS232BarcodeScanner scanner = new BasicRS232BarcodeScanner();
			scanner.setConfiguration(configuration);
			scanner.addBarcodeListener(new BarcodeListener() {
				public void onCatchBarcode(String value) {
					System.out.println(value);
				}
			});
			scanner.addConnectionListener(new BarcodeScannerConnectionListener() {
				public void onConnected() {
					connectLatch.countDown();
					System.out.println("Connected.");
				}

				public void onDisconnected() {
					connectLatch.countDown();
					System.out.println("Disconnected.");
				}

				public void onConnectionFailed() {
					connectLatch.countDown();
					System.out.println("Connection failed.");
				}
			});

			System.out.println(String.format("Conneting to %s...", portName));
			scanner.connect();
			connectLatch.await();
			if (scanner.isConnected()) {
				waitForEnter();
				scanner.disconnect();
			}

			System.out.println("Program terminated.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void waitForEnter() {
		try {
			System.out.println("Press ENTER to terminate the program.");
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
