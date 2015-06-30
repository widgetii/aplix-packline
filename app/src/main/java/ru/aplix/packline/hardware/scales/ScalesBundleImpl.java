package ru.aplix.packline.hardware.scales;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

public class ScalesBundleImpl<C extends ScalesConfiguration> implements ScalesBundle<C> {

	private List<ScalesBundleConnectionListener> scalesList;
	private Set<ScalesConnectionListener> connectionListeners;
	private Set<MeasurementListener> measurementListeners;
	private int connectLatch;
	private int position;
	private Float value;

	@SafeVarargs
	public ScalesBundleImpl(Scales<C>... scales) {
		this(Arrays.asList(scales));
	}

	public ScalesBundleImpl(List<Scales<C>> scales) {
		scalesList = new ArrayList<ScalesBundleConnectionListener>();
		for (Scales<C> s : scales) {
			scalesList.add(new ScalesBundleConnectionListener(s));
		}
		connectLatch = scalesList.size();

		connectionListeners = new HashSet<ScalesConnectionListener>();
		measurementListeners = new HashSet<MeasurementListener>();
	}

	@Override
	public void connect() {
		connectLatch = scalesList.size();
		scalesList.stream().forEach(scl -> scl.scales.connect());
	}

	@Override
	public void disconnect() {
		connectLatch = scalesList.size();
		scalesList.stream().forEach(scl -> scl.scales.disconnect());
	}

	@Override
	public boolean isConnected() {
		return scalesList.stream().allMatch(scl -> scl.scales.isConnected());
	}

	@Override
	public String getName() {
		return StringUtils.join(scalesList.stream().map(scl -> scl.scales.getName()).collect(Collectors.toList()), "|");
	}

	@Override
	public void setConfiguration(String config) throws IllegalArgumentException {
		if (position > -1 && position < scalesList.size()) {
			Scales<C> scales = scalesList.get(position).scales;
			scales.setConfiguration(config);
		}
	}

	@Override
	public void setConfiguration(C config) throws IllegalArgumentException {
		if (position > -1 && position < scalesList.size()) {
			Scales<C> scales = scalesList.get(position).scales;
			scales.setConfiguration(config);
		}
	}

	@Override
	public C getConfiguration() {
		if (position > -1 && position < scalesList.size()) {
			Scales<C> scales = scalesList.get(position).scales;
			return scales.getConfiguration();
		} else {
			return null;
		}
	}

	@Override
	public void setConnectOnDemand(boolean value) {
		scalesList.stream().forEach(scl -> scl.scales.setConnectOnDemand(value));
	}

	@Override
	public boolean getConnectOnDemand() {
		return scalesList.stream().anyMatch(scl -> scl.scales.getConnectOnDemand());
	}

	@Override
	public void addMeasurementListener(MeasurementListener listener) {
		measurementListeners.add(listener);
	}

	@Override
	public void removeMeasurementListener(MeasurementListener listener) {
		measurementListeners.remove(listener);
	}

	@Override
	public void addConnectionListener(ScalesConnectionListener connectionListener) {
		connectionListeners.add(connectionListener);
	}

	@Override
	public void removeConnectionListener(ScalesConnectionListener connectionListener) {
		connectionListeners.remove(connectionListener);
	}

	@Override
	public Float getLastMeasurement() {
		return value;
	}

	@Override
	public void resetEnumerator() {
		position = scalesList.size() > 0 ? 0 : -1;
	}

	@Override
	public boolean hasMoreElements() {
		return position > -1 && position < scalesList.size();
	}

	@Override
	public Scales<C> nextElement() {
		if (position > -1 && position < scalesList.size()) {
			return scalesList.get(position++).scales;
		} else {
			return null;
		}
	}

	@Override
	public boolean isNoMoreThanOneLoaded() {
		return scalesList.stream().collect(
				Collectors.summingInt((ScalesBundleConnectionListener sbcl) -> sbcl.scales.getLastMeasurement() != null
						&& sbcl.scales.getLastMeasurement() > 0f ? 1 : 0)) <= 1;
	}

	@Override
	public Scales<C> whoIsLoaded() {
		Optional<ScalesBundleConnectionListener> optional = scalesList.stream()
				.filter(sbcl -> sbcl.scales.getLastMeasurement() != null && sbcl.scales.getLastMeasurement() > 0f).findFirst();
		return optional.isPresent() ? optional.get().scales : null;
	}

	/**
	 *
	 */
	private class ScalesBundleConnectionListener implements ScalesConnectionListener, MeasurementListener {

		private Scales<C> scales;

		public ScalesBundleConnectionListener(Scales<C> scales) {
			this.scales = scales;
			scales.addConnectionListener(this);
			scales.addMeasurementListener(this);
		}

		@Override
		public void onConnected() {
			synchronized (ScalesBundleImpl.this) {
				connectLatch--;
				if (connectLatch == 0) {
					boolean allok = isConnected();
					connectionListeners.stream().forEach(allok ? scl -> scl.onConnected() : scl -> scl.onConnectionFailed());
				}
			}
		}

		@Override
		public void onDisconnected() {
			synchronized (ScalesBundleImpl.this) {
				connectLatch--;
				if (connectLatch == 0) {
					connectionListeners.stream().forEach(scl -> scl.onDisconnected());
				}
			}
		}

		@Override
		public void onConnectionFailed() {
			synchronized (ScalesBundleImpl.this) {
				connectLatch--;
				if (connectLatch == 0) {
					connectionListeners.stream().forEach(scl -> scl.onConnectionFailed());
				}
			}
		}

		@Override
		public void onMeasure(Float value) {
			ScalesBundleImpl.this.value = value;
			measurementListeners.stream().forEach(ml -> ml.onMeasure(value));
		}

		@Override
		public void onWeightStabled(Float value) {
			ScalesBundleImpl.this.value = value;

			boolean allOtherZero = scalesList.stream().filter(scl -> scl.scales != scales)
					.allMatch(scl -> scl.scales.getLastMeasurement() == null || Math.abs(scl.scales.getLastMeasurement()) < 1e-5f);
			if (allOtherZero) {
				measurementListeners.stream().forEach(ml -> ml.onWeightStabled(value));
			}
		}
	}
}
