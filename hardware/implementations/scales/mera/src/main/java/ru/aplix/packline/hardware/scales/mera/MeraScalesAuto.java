package ru.aplix.packline.hardware.scales.mera;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;

public class MeraScalesAuto implements Scales<RS232Configuration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ReentrantLock connectionLock;
	private CountDownLatch connectLatch = null;
	private SerialPort serialPort;
	private volatile boolean isConnected = false;

	private RS232Configuration configuration;
	private List<MeasurementListener> listeners;
	private List<ScalesConnectionListener> connectionListeners;
	private boolean connectOnDemand;
	
	private static final int MERA_SCALES_PORT_SPEED = 115200;

	public MeraScalesAuto() {
		configuration = new RS232Configuration();

		listeners = new Vector<MeasurementListener>();
		connectionListeners = new Vector<ScalesConnectionListener>();

		connectionLock = new ReentrantLock();

		connectOnDemand = false;
	}

	public String getName() {
		return getClass().getSimpleName();
	}

	public void setConfiguration(String config) throws IllegalArgumentException {
		try {
			JAXBContext inst = JAXBContext.newInstance(RS232Configuration.class);
			Unmarshaller unmarshaller = inst.createUnmarshaller();
			configuration = (RS232Configuration) unmarshaller.unmarshal(new StringReader(config));
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setConfiguration(RS232Configuration config) throws IllegalArgumentException {
		configuration = config;
	}

	public RS232Configuration getConfiguration() {
		return configuration;
	}

	public void connect() {
		new Thread(new Runnable() {
			public void run() {
				AutoPortEventListener portListener = null;

				connectionLock.lock();
				try {
					try {
						if (!isConnected()) {
							CommPortIdentifier portId = findPort(configuration.getPortName());
							if (portId == null) {
								throw new RuntimeException(String.format("Port '%s' not found.", configuration.getPortName()));
							}

							connectLatch = new CountDownLatch(1);

							serialPort = (SerialPort) portId.open(getClass().getName(), 2000);
							serialPort.setSerialPortParams(MERA_SCALES_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
							serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
							serialPort.enableReceiveTimeout(configuration.getTimeout());

							portListener = new AutoPortEventListener();

							isConnected = true;
						}

						synchronized (connectionListeners) {
							for (ScalesConnectionListener listener : connectionListeners) {
								listener.onConnected();
							}
						}
					} catch (Exception e) {
						synchronized (connectionListeners) {
							for (ScalesConnectionListener listener : connectionListeners) {
								listener.onConnectionFailed();
							}
						}

						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
					}
				} finally {
					connectionLock.unlock();
				}

				if (portListener == null) {
					return;
				}

				try {
					while (connectLatch.getCount() > 0) {
						portListener.readData();
					}
				} catch (Exception e) {
					if (connectLatch.getCount() > 0) {
						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
					}
				}

				connectionLock.lock();
				try {
					try {
						isConnected = false;
						connectLatch = null;

						serialPort.getInputStream().close();
						serialPort.close();

						synchronized (connectionListeners) {
							for (ScalesConnectionListener listener : connectionListeners) {
								listener.onDisconnected();
							}
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

	public void disconnect() {
		new Thread(new Runnable() {
			public void run() {
				connectionLock.lock();
				try {
					try {
						if (isConnected) {
							if (connectLatch != null) {
								connectLatch.countDown();
							}
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

	public boolean isConnected() {
		return isConnected;
	}

	public void setConnectOnDemand(boolean value) {
		this.connectOnDemand = value;
	}

	public boolean getConnectOnDemand() {
		return connectOnDemand;
	}

	@SuppressWarnings({ "unchecked" })
	private CommPortIdentifier findPort(String portName) {
		CommPortIdentifier result = null;
		Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
		while (result == null && portList.hasMoreElements()) {
			CommPortIdentifier portId = portList.nextElement();
			if (portId.getPortType() == CommPortIdentifier.PORT_SERIAL && portId.getName().equals(portName)) {
				result = portId;
			}
		}
		return result;
	}

	/**
	 * Auto Port Event Listener.
	 */
	private class AutoPortEventListener implements SerialPortEventListener {

		private static final int BUFFER_LENGTH = 1024;
		private byte[] buffer = null;
		private int bufferPos = 0;
		private InputStream inputStream;

		private Pattern packetPattern;

		public AutoPortEventListener() {
			String numberPattern = "\\s*([\\+|\\-]{0,1}[\\d\\.]+)\\s*";
			packetPattern = Pattern.compile(numberPattern + "#" + numberPattern + "\\s*([S|s|D|d]+)\\s*");
		}

		public void serialEvent(SerialPortEvent event) {
			switch (event.getEventType()) {
			case SerialPortEvent.DATA_AVAILABLE:
				try {
					readData();
				} catch (IOException e) {
					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
				}
				break;
			default:
				break;
			}
		}

		public void readData() throws IOException {
			if (buffer == null) {
				buffer = new byte[BUFFER_LENGTH];
			}

			if (inputStream == null) {
				inputStream = serialPort.getInputStream();
			}

			// Add incoming data to our buffer
			int bytesReaded = -1;
			if (inputStream.available() > 0) {
				int readLenght = Math.min(buffer.length - bufferPos, inputStream.available());
				bytesReaded = inputStream.read(buffer, bufferPos, readLenght);
			} else {
				byte value = (byte) inputStream.read();
				if (value >= 0) {
					buffer[bufferPos] = value;
					bytesReaded = 1;
				}
			}

			if (bytesReaded <= 0) {
				return;
			}

			bufferPos += bytesReaded;

			// If buffer is not enough for data available,
			// then throw an exception
			if (bufferPos == buffer.length) {
				bufferPos = 0;
				throw new RuntimeException("Buffer overflow.");
			}

			// Search line terminators in buffer
			// and parse strings
			int endPos = 0;
			int startPos = 0;
			for (int i = 0; i < bufferPos; i++) {
				short thisByte = buffer[i];
				short nextByte = (i < bufferPos - 1) ? buffer[i + 1] : Short.MIN_VALUE;

				int increment = 0;
				if ((thisByte == 10 && nextByte == 13) || (thisByte == 13 && nextByte == 10)) {
					increment = 2;
				} else if ((thisByte == 10) || (thisByte == 13)) {
					increment = 1;
				}

				if (increment > 0) {
					endPos = i;
					String string = new String(buffer, startPos, endPos - startPos);
					if (string != null && string.length() > 0) {
						onReadString(string);
					}

					endPos += increment;
					startPos = endPos;
					i = startPos;
				}
			}

			// If buffer is fully parsed then reset its position
			if (endPos == bufferPos) {
				bufferPos = 0;
			} else
			// If something was left in buffer then
			// shift it to the beginning of our buffer
			if (endPos > 0 && endPos < bufferPos) {
				for (int i = endPos; i <= bufferPos; i++) {
					buffer[i - endPos] = buffer[i];
				}
				bufferPos -= endPos;
			}

			// Wait for another part of data
		}

		private void onReadString(String value) {
			boolean invalidFormat = true;
			Matcher m = packetPattern.matcher(value);
			if (m.matches() && m.groupCount() == 3) {
				try {
					Float f = Float.parseFloat(m.group(2));
					onMeasure(f);
					invalidFormat = false;
				} catch (NumberFormatException nfe) {
				}
			}
			if (invalidFormat) {
				LOG.debug(String.format("%s: invalid data format.", getName()));
			}
		}

		private void onMeasure(Float value) {
			synchronized (listeners) {
				for (MeasurementListener listener : listeners) {
					listener.onMeasure(value);
				}
			}
		}
	}

	public void addMeasurementListener(MeasurementListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	public void removeMeasurementListener(MeasurementListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void addConnectionListener(ScalesConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.add(connectionListener);
		}
	}

	public void removeConnectionListener(ScalesConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.remove(connectionListener);
		}
	}
}
