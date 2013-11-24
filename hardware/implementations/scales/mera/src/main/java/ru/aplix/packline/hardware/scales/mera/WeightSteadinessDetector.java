package ru.aplix.packline.hardware.scales.mera;

import java.util.List;
import java.util.Vector;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import ru.aplix.packline.hardware.scales.MeasurementListener;

public class WeightSteadinessDetector {

	public static final int DEFAULT_SIZE = 10;
	private static final double CONSIDER_ALMOST_ZERO = 0.000001d;

	boolean lastWeightStabled;
	private CircularFifoBuffer buffer;
	private List<MeasurementListener> listeners;

	public WeightSteadinessDetector(int size) {
		lastWeightStabled = false;
		listeners = new Vector<MeasurementListener>();
		buffer = new CircularFifoBuffer(size > 0 ? size : DEFAULT_SIZE);
	}

	public void addMeasurementListener(MeasurementListener listener) {
		listeners.add(listener);
	}

	public void removeMeasurementListener(MeasurementListener listener) {
		listeners.remove(listener);
	}

	public boolean hasMeasurementListeners() {
		return listeners.size() > 0;
	}

	public void measure(Float value) {
		boolean weightStabled = false;

		buffer.add(new Double(value));
		if (buffer.isFull()) {
			@SuppressWarnings("unchecked")
			Double[] values = (Double[]) buffer.toArray(new Double[0]);
			StandardDeviation sd = new StandardDeviation();
			double sdValue = sd.evaluate(ArrayUtils.toPrimitive(values));
			weightStabled = (sdValue < CONSIDER_ALMOST_ZERO) && !(value < CONSIDER_ALMOST_ZERO);
		}

		for (MeasurementListener listener : listeners) {
			if (!lastWeightStabled && weightStabled) {
				listener.onWeightStabled(value);
			} else {
				listener.onMeasure(value);
			}
		}

		lastWeightStabled = weightStabled;
	}
}
