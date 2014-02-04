package ru.aplix.packline.hardware.scales.mera;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import ru.aplix.mera.scales.ScalesPort;
import ru.aplix.mera.scales.WeightMessage;
import ru.aplix.mera.scales.backend.WeightUpdate;
import ru.aplix.mera.scales.config.WeightSteadinessDetector;
import ru.aplix.mera.scales.config.WeightSteadinessPolicy;

final class ImprovedWeightSteadinessPolicy implements WeightSteadinessPolicy, WeightSteadinessDetector {

	public static final int DEFAULT_SIZE = 10;
	private static final double CONSIDER_ALMOST_ZERO = 0.000001d;

	boolean lastWeightStabled;
	private CircularFifoBuffer buffer;

	public ImprovedWeightSteadinessPolicy(int size) {
		lastWeightStabled = false;
		buffer = new CircularFifoBuffer(size > 0 ? size : DEFAULT_SIZE);
	}

	@Override
	public WeightSteadinessDetector createSteadinessDetector(ScalesPort port) {
		return this;
	}

	@Override
	public int steadyWeight(WeightUpdate weightUpdate) {
		boolean weightStabled = false;
		try {
			double value = (double) weightUpdate.getWeight() / 1000d;

			buffer.add(new Double(value));
			if (buffer.isFull()) {
				@SuppressWarnings("unchecked")
				Double[] values = (Double[]) buffer.toArray(new Double[0]);
				StandardDeviation sd = new StandardDeviation();
				double sdValue = sd.evaluate(ArrayUtils.toPrimitive(values));
				weightStabled = (sdValue < CONSIDER_ALMOST_ZERO);
			}

			if (!lastWeightStabled && weightStabled) {
				return weightUpdate.getWeight();
			} else {
				return NON_STEADY_WEIGHT;
			}
		} finally {
			lastWeightStabled = weightStabled;
		}
	}

	@Override
	public WeightSteadinessDetector weightChanged(WeightMessage steadyWeight, WeightUpdate weightUpdate) {
		if (steadyWeight(weightUpdate) == NON_STEADY_WEIGHT) {
			return this;
		} else {
			return null;
		}
	}
}
