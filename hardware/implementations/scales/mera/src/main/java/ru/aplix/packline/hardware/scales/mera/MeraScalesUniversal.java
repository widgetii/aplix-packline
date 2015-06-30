package ru.aplix.packline.hardware.scales.mera;

import static ru.aplix.mera.scales.ScalesService.newScalesService;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.mera.message.MeraConsumer;
import ru.aplix.mera.scales.ScalesErrorHandle;
import ru.aplix.mera.scales.ScalesErrorMessage;
import ru.aplix.mera.scales.ScalesPort;
import ru.aplix.mera.scales.ScalesPortHandle;
import ru.aplix.mera.scales.ScalesPortId;
import ru.aplix.mera.scales.ScalesService;
import ru.aplix.mera.scales.ScalesStatus;
import ru.aplix.mera.scales.ScalesStatusMessage;
import ru.aplix.mera.scales.WeightHandle;
import ru.aplix.mera.scales.WeightMessage;
import ru.aplix.mera.scales.byte9.Byte9Protocol;
import ru.aplix.mera.scales.config.ScalesConfig;
import ru.aplix.mera.scales.config.WeightSteadinessPolicy;
import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;

public class MeraScalesUniversal implements Scales<MeraScalesConfiguration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ReentrantLock connectionLock;
	private final ScalesService scalesService;
	private ScalesPortHandle handle = null;
	private volatile boolean isConnected = false;
	private volatile Float lastMeasurement;

	private MeraScalesConfiguration configuration;
	private Set<MeasurementListener> listeners;
	private Set<ScalesConnectionListener> connectionListeners;
	private boolean connectOnDemand;

	public MeraScalesUniversal() {
		scalesService = newScalesService();

		configuration = new MeraScalesConfiguration();

		listeners = new HashSet<MeasurementListener>();
		connectionListeners = new HashSet<ScalesConnectionListener>();

		connectionLock = new ReentrantLock();

		connectOnDemand = false;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void setConfiguration(String config) throws IllegalArgumentException {
		try {
			JAXBContext inst = JAXBContext.newInstance(MeraScalesConfiguration.class);
			Unmarshaller unmarshaller = inst.createUnmarshaller();
			configuration = (MeraScalesConfiguration) unmarshaller.unmarshal(new StringReader(config));
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void setConfiguration(MeraScalesConfiguration config) throws IllegalArgumentException {
		configuration = config;
	}

	@Override
	public MeraScalesConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void connect() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				connectionLock.lock();
				try {
					try {
						if (handle == null) {
							ScalesPortId portId = findPort(configuration.getPortName(), configuration.getProtocolName());
							if (portId == null) {
								throw new RuntimeException(String.format("Port '%s (%s)' not found.", configuration.getPortName(),
										configuration.getProtocolName()));
							}

							final ScalesPort port = portId.getPort();
							port.setConfig(updateConfig(port.getConfig()));
							handle = port.subscribe(new StatusListener());
							handle.listenForErrors(new ErrorListener());
							handle.requestWeight(new WeightListener());
						}

						if (isConnected) {
							for (ScalesConnectionListener listener : connectionListeners) {
								listener.onConnected();
							}
						}
					} catch (Exception e) {
						for (ScalesConnectionListener listener : connectionListeners) {
							listener.onConnectionFailed();
						}

						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
					}
				} finally {
					connectionLock.unlock();
				}
			}
		}).start();
	}

	@Override
	public void disconnect() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				connectionLock.lock();
				try {
					try {
						if (handle != null) {
							handle.unsubscribe();
							handle = null;

							if (!isConnected) {
								for (ScalesConnectionListener listener : connectionListeners) {
									listener.onConnectionFailed();
								}
								return;
							}
						}

						if (isConnected) {
							isConnected = false;
						}

						for (ScalesConnectionListener listener : connectionListeners) {
							listener.onDisconnected();
						}
					} catch (Exception e) {
						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
					}
				} finally {
					connectionLock.unlock();
				}
			}
		}).start();
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}

	private ScalesConfig updateConfig(ScalesConfig scalesConfig) {
		if (configuration.getWeighingPeriod() != null) {
			scalesConfig = scalesConfig.setWeighingPeriod(configuration.getWeighingPeriod());
		}
		if (configuration.getSteadyWeightSamples() != null) {
			WeightSteadinessPolicy wsp = new ImprovedWeightSteadinessPolicy(configuration.getSteadyWeightSamples());
			scalesConfig = scalesConfig.setWeightSteadinessPolicy(wsp);
		}

		if (configuration.getConnectionTimeout() != null) {
			scalesConfig = scalesConfig.set(Byte9Protocol.BYTE9_CONNECTION_TIMEOUT, configuration.getConnectionTimeout());
		}
		if (configuration.getCommandRetries() != null) {
			scalesConfig = scalesConfig.set(Byte9Protocol.BYTE9_COMMAND_RETRIES, configuration.getCommandRetries());
		}
		if (configuration.getResponseTimeout() != null) {
			scalesConfig = scalesConfig.set(Byte9Protocol.BYTE9_RESPONSE_TIMEOUT, configuration.getResponseTimeout());
		}
		if (configuration.getDataDelay() != null) {
			scalesConfig = scalesConfig.set(Byte9Protocol.BYTE9_DATA_DELAY, configuration.getDataDelay());
		}

		return scalesConfig;
	}

	private ScalesPortId findPort(String portName, String protocolName) {
		ScalesPortId result = null;
		Iterator<? extends ScalesPortId> portList = scalesService.getScalesPortIds().iterator();
		while (result == null && portList.hasNext()) {
			ScalesPortId portId = portList.next();

			if (portId.getPortId().equals(portName) && portId.getProtocol().getProtocolId().equals(protocolName)) {
				result = portId;
			}
		}
		return result;
	}

	@Override
	public void setConnectOnDemand(boolean value) {
		this.connectOnDemand = value;
	}

	@Override
	public boolean getConnectOnDemand() {
		return connectOnDemand;
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
	public void addMeasurementListener(MeasurementListener listener) {
		listeners.add(listener);
		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	@Override
	public void removeMeasurementListener(MeasurementListener listener) {
		listeners.remove(listener);
	}

	/**
	 * 
	 */
	private class StatusListener implements MeraConsumer<ScalesPortHandle, ScalesStatusMessage> {

		private boolean wasError = false;

		@Override
		public void consumerSubscribed(ScalesPortHandle handle) {
		}

		@Override
		public void consumerUnsubscribed(ScalesPortHandle handle) {
		}

		@Override
		public void messageReceived(ScalesStatusMessage message) {
			if (ScalesStatus.SCALES_CONNECTED.equals(message.getScalesStatus())) {
				if (wasError) {
					return;
				}

				connectionLock.lock();
				try {
					if (!isConnected) {
						isConnected = true;
						for (ScalesConnectionListener listener : connectionListeners) {
							listener.onConnected();
						}
					}
				} finally {
					connectionLock.unlock();
				}
			} else if (ScalesStatus.SCALES_ERROR.equals(message.getScalesStatus())) {
				wasError = true;

				LOG.error(String.format("Error in %s '%s': %s", getName(), configuration.getPortName(), message.getScalesError()));

				disconnect();
			}
		}
	}

	/**
	 * 
	 */
	private class ErrorListener implements MeraConsumer<ScalesErrorHandle, ScalesErrorMessage> {

		@Override
		public void consumerSubscribed(ScalesErrorHandle handle) {
		}

		@Override
		public void consumerUnsubscribed(ScalesErrorHandle handle) {
		}

		@Override
		public void messageReceived(ScalesErrorMessage message) {
			LOG.error(String.format("Error in %s '%s': %s", getName(), configuration.getPortName(), message.getErrorMessage()), message.getCause());
		}
	}

	/**
	 * 
	 */
	private class WeightListener implements MeraConsumer<WeightHandle, WeightMessage> {

		@Override
		public void consumerSubscribed(WeightHandle handle) {
		}

		@Override
		public void consumerUnsubscribed(WeightHandle handle) {
		}

		@Override
		public void messageReceived(WeightMessage message) {
			boolean firstMeasure = lastMeasurement == null;
			lastMeasurement = message.getWeight() / 1000f;
			for (MeasurementListener listener : listeners) {
				if (!firstMeasure && message.getWeight() > 0) {
					listener.onWeightStabled(lastMeasurement);
				} else {
					listener.onMeasure(lastMeasurement);
				}
			}
		}
	}

	@Override
	public Float getLastMeasurement() {
		return lastMeasurement;
	}
}
