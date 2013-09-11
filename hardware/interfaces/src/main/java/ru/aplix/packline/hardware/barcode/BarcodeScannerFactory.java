package ru.aplix.packline.hardware.barcode;

import java.util.Iterator;
import java.util.ServiceLoader;

public class BarcodeScannerFactory {

	@SuppressWarnings("rawtypes")
	private static ServiceLoader<BarcodeScanner> barcodeScannerLoader = ServiceLoader.load(BarcodeScanner.class);

	public static BarcodeScanner<?> createAnyInstance() throws ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Iterator<BarcodeScanner> i = barcodeScannerLoader.iterator();
		if (i.hasNext()) {
			return i.next();
		} else {
			throw new ClassNotFoundException();
		}
	}

	public static BarcodeScanner<?> createInstance(String name) throws ClassNotFoundException {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (BarcodeScanner<?> bs : barcodeScannerLoader) {
			if (name.equals(bs.getName())) {
				return bs;
			}
		}
		throw new ClassNotFoundException();
	}
}
