package ru.aplix.packline.hardware.scales.middle;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;

public class MiddleScalesTestManual {

	private static final boolean PRINT_IN_ONE_LINE = false;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("No COM port or Protocol has been specified.");
			return;
		}

		new MiddleScalesTestManual(args[0], args[1]).run();
	}

	private String portName;
	private String protocolName;

	/**
	 * Class constructor.
	 * 
	 * @param portName
	 */
	public MiddleScalesTestManual(String portName, String protocolName) {
		this.portName = portName;
		this.protocolName = protocolName;
	}

	protected Scales<?> createScales() {
		if ("Auto".equalsIgnoreCase(protocolName)) {
			RS232Configuration configuration = new RS232Configuration();
			configuration.setPortName(portName);

			MiddleScalesAuto result = new MiddleScalesAuto();
			result.setConfiguration(configuration);
			return result;
		}  else {
			throw new IllegalArgumentException(String.format("Protocol '%s' not supported.", protocolName));
		}
	}

	public void run() {
		try {
			final CountDownLatch connectLatch = new CountDownLatch(1);

			Scales<?> scales = createScales();
			scales.addMeasurementListener(new MeasurementListener() {
				private int textLength = -1;

				public void onMeasure(Float value) {
					if (PRINT_IN_ONE_LINE) {
						print(String.format("%.3f kg", value));
					} else {
						System.out.println(String.format("%.3f kg", value));
					}
				}

				@Override
				public void onWeightStabled(Float value) {
					if (PRINT_IN_ONE_LINE) {
						print(String.format("%.3f kg of steady weight", value));
					} else {
						System.out.println(String.format("%.3f kg of steady weight", value));
					}
				}

				private void print(String value) {
					for (int i = 0; i < textLength; i++) {
						System.out.print("\b");
					}

					textLength = value.length();
					System.out.print(value);
				}
			});
			scales.addConnectionListener(new ScalesConnectionListener() {
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
			scales.connect();
			connectLatch.await();
			if (scales.isConnected()) {
				waitForEnter();
				scales.disconnect();
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
