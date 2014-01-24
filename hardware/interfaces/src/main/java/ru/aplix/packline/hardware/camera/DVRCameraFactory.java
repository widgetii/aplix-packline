package ru.aplix.packline.hardware.camera;

import java.util.Iterator;
import java.util.ServiceLoader;

public class DVRCameraFactory {

	@SuppressWarnings("rawtypes")
	private static ServiceLoader<DVRCamera> dvrCameraLoader = ServiceLoader.load(DVRCamera.class);

	public static DVRCamera<?> createAnyInstance() throws ClassNotFoundException {
		@SuppressWarnings("rawtypes")
		Iterator<DVRCamera> i = dvrCameraLoader.iterator();
		if (i.hasNext()) {
			return i.next();
		} else {
			throw new ClassNotFoundException();
		}
	}

	public static DVRCamera<?> createInstance(String name) throws ClassNotFoundException {
		if (name == null || name.length() == 0) {
			throw new IllegalArgumentException();
		}
		for (DVRCamera<?> bs : dvrCameraLoader) {
			if (name.equals(bs.getName())) {
				return bs;
			}
		}
		throw new ClassNotFoundException(name);
	}
}
