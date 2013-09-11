package ru.aplix.packline.hardware.scales;

import java.util.Iterator;
import java.util.ServiceLoader;

public class ScalesFactory {

    @SuppressWarnings("rawtypes")
    private static ServiceLoader<Scales> scalesLoader = ServiceLoader.load(Scales.class);

    public static Scales<?> createAnyInstance() throws ClassNotFoundException {
        @SuppressWarnings("rawtypes")
        Iterator<Scales> i = scalesLoader.iterator();
        if (i.hasNext()) {
            return i.next();
        } else {
            throw new ClassNotFoundException();
        }
    }

    public static Scales<?> createInstance(String name) throws ClassNotFoundException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException();
        }
        for (Scales<?> bs : scalesLoader) {
            if (name.equals(bs.getName())) {
                return bs;
            }
        }
        throw new ClassNotFoundException();
    }
}
