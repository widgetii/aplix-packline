package ru.aplix.packline.hardware.scales.mera;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;

public class MeraScalesTestManual {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("No COM port has been specified.");
			return;
		}

		new MeraScalesTestManual(args[args.length - 1]).run();
	}

	private String portName;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public MeraScalesTestManual(String portName) {
		this.portName = portName;
	}

	public void run() {
		try {
			final CountDownLatch connectLatch = new CountDownLatch(1);

			RS232Configuration configuration = new RS232Configuration();
			configuration.setPortName(portName);
			configuration.setPortSpeed(57600);
			configuration.setEnabled(true);

			MeraScales scanner = new MeraScales();
			scanner.setConfiguration(configuration);
			scanner.addMeasurementListener(new MeasurementListener() {
				private int textLength = -1;

				public void onMeasure(Float value) {
					print(String.format("%.3f", value));
				}

				private void print(String value) {
					for (int i = 0; i < textLength; i++) {
						System.out.print("\b");
					}

					textLength = value.length();
					System.out.print(value);
				}
			});
			scanner.addConnectionListener(new ScalesConnectionListener() {
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

			System.out.println(String.format("Conneting to %s...", portName));
			scanner.connect();
			connectLatch.await();
			if (scanner.isConnected()) {
				System.out.println("Connected");
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
