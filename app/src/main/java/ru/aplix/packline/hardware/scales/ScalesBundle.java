package ru.aplix.packline.hardware.scales;

import java.util.Enumeration;

public interface ScalesBundle<C extends ScalesConfiguration> extends Scales<C>, Enumeration<Scales<C>> {

	void resetEnumerator();

	boolean isNoMoreThanOneLoaded();

	Scales<C> whoIsLoaded();
}
