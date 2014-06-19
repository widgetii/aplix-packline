package ru.aplix.packline;

import junit.framework.TestCase;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.barcode.BarcodeScannerFactory;
import ru.aplix.packline.hardware.camera.DVRCamera;
import ru.aplix.packline.hardware.camera.DVRCameraFactory;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraFactory;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesFactory;
import ru.aplix.packline.hardware.scanner.ImageScanner;
import ru.aplix.packline.hardware.scanner.ImageScannerFactory;

public class HardwareTest extends TestCase {

	public void testBarcodeScannerFactoty() throws ClassNotFoundException {
		BarcodeScanner<?> bs = BarcodeScannerFactory.createAnyInstance();
		assertNotNull(bs);

		bs = BarcodeScannerFactory.createInstance(bs.getName());
		assertNotNull(bs);
	}

	public void testPhotoCameraFactoty() throws ClassNotFoundException {
		PhotoCamera<?> pc = PhotoCameraFactory.createAnyInstance();
		assertNotNull(pc);

		pc = PhotoCameraFactory.createInstance(pc.getName());
		assertNotNull(pc);
	}

	public void testImageScannerFactoty() throws ClassNotFoundException {
		ImageScanner<?> is = ImageScannerFactory.createAnyInstance();
		assertNotNull(is);

		is = ImageScannerFactory.createInstance(is.getName());
		assertNotNull(is);
	}

	public void testDVRCameraFactoty() throws ClassNotFoundException {
		DVRCamera<?> dc = DVRCameraFactory.createAnyInstance();
		assertNotNull(dc);

		dc = DVRCameraFactory.createInstance(dc.getName());
		assertNotNull(dc);
	}

	public void testScalesFactoty() throws ClassNotFoundException {
		Scales<?> sc = ScalesFactory.createAnyInstance();
		assertNotNull(sc);

		sc = ScalesFactory.createInstance(sc.getName());
		assertNotNull(sc);
	}
}
