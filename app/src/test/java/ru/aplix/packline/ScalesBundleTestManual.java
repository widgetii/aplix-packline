package ru.aplix.packline;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import junit.framework.TestCase;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.ScalesBundle;
import ru.aplix.packline.hardware.scales.ScalesBundleImpl;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;
import ru.aplix.packline.hardware.scales.mera.MeraScalesAuto;
import ru.aplix.packline.hardware.scales.mera.MeraScalesConfiguration;
import ru.aplix.packline.hardware.scales.mera.MeraScalesUniversal;
import ru.aplix.packline.hardware.scales.mera.RS232Configuration;

public class ScalesBundleTestManual extends TestCase {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void test() throws ClassNotFoundException, InterruptedException, IOException {
		MeraScalesAuto sc1 = new MeraScalesAuto();
		assertNotNull(sc1);
		RS232Configuration sc1Conf = new RS232Configuration();
		sc1Conf.setPortName("COM41");
		sc1.setConfiguration(sc1Conf);

		MeraScalesUniversal sc2 = new MeraScalesUniversal();
		assertNotNull(sc2);
		MeraScalesConfiguration sc2Conf = new MeraScalesConfiguration();
		sc2Conf.setPortName("COM43");
		sc2Conf.setProtocolName("Auto");
		sc2.setConfiguration(sc2Conf);

		ScalesBundle<?> scales = new ScalesBundleImpl(sc1, sc2);
		assertNotNull(scales);

		final CountDownLatch connectLatch = new CountDownLatch(1);
		scales.addMeasurementListener(new MeasurementListener() {
			public void onMeasure(Float value) {
				System.out.println(String.format("%.3f kg", value));
			}

			@Override
			public void onWeightStabled(Float value) {
				System.out.println(String.format("%.3f kg of steady weight", value));
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

		System.out.println(String.format("Conneting to %s on %s|%s...", scales.getName(), sc1Conf.getPortName(), sc2Conf.getPortName()));
		scales.connect();
		connectLatch.await();
		if (scales.isConnected()) {
			System.out.println("Press ENTER to terminate the program.");
			System.in.read();

			scales.disconnect();
		}

		System.out.println("Program terminated.");
	}
}
