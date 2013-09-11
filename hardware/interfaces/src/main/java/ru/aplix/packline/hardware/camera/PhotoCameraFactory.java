package ru.aplix.packline.hardware.camera;

import java.util.Iterator;
import java.util.ServiceLoader;

public class PhotoCameraFactory {

	@SuppressWarnings("rawtypes")
	private static ServiceLoader<PhotoCamera> photoCameraLoader = ServiceLoader.load(PhotoCamera.class);

	public static PhotoCamera<?> createAnyInstance() throws ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Iterator<PhotoCamera> i = photoCameraLoader.iterator();
		if (i.hasNext()) {
			return i.next();
		} else {
			throw new ClassNotFoundException();
		}
	}

	public static PhotoCamera<?> createInstance(String name) throws ClassNotFoundException {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (PhotoCamera<?> bs : photoCameraLoader) {
			if (name.equals(bs.getName())) {
				return bs;
			}
		}
		throw new ClassNotFoundException();
	}
}
