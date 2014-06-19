package ru.aplix.packline.hardware.scanner;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ImageScannerFactory {

	@SuppressWarnings("rawtypes")
	private static ServiceLoader<ImageScanner> imageScannerLoader = ServiceLoader.load(ImageScanner.class);

	public static ImageScanner<?> createAnyInstance() throws ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Iterator<ImageScanner> i = imageScannerLoader.iterator();
		if (i.hasNext()) {
			return i.next();
		} else {
			throw new ClassNotFoundException();
		}
	}

	public static ImageScanner<?> createInstance(String name) throws ClassNotFoundException {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (ImageScanner<?> is : imageScannerLoader) {
			if (name.equals(is.getName())) {
				return is;
			}
		}
		throw new ClassNotFoundException(name);
	}
}
