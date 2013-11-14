package ru.aplix.packline.hardware.scales.mera;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.scales.MeasurementListener;
import ru.aplix.packline.hardware.scales.Scales;
import ru.aplix.packline.hardware.scales.ScalesConnectionListener;

public class MeraScalesByte9 implements Scales<RS232Configuration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ReentrantLock connectionLock;
	private CountDownLatch connectLatch = null;
	private SerialPort serialPort;
	private volatile boolean isConnected = false;
	private volatile Float lastMeasurement;

	private RS232Configuration configuration;
	private List<MeasurementListener> listeners;
	private List<ScalesConnectionListener> connectionListeners;
	private boolean connectOnDemand;

	private static final int MERA_SCALES_PORT_SPEED = 14400;

	public MeraScalesByte9() {
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
				Byte9PortEventListener mcspl = null;

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

							mcspl = new Byte9PortEventListener();

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

				if (mcspl == null) {
					return;
				}

				try {
					while (connectLatch.getCount() > 0) {
						mcspl.readDataSafe();
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
						serialPort.getOutputStream().close();
						serialPort.close();
						serialPort = null;

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
	 * Byte 9 Port Event Listener.
	 */
	private class Byte9PortEventListener implements SerialPortEventListener {

		private static final int BYTE9_PACKET_LENGTH = 9;

		private static final byte BYTE9_TERMINATOR = 0x0d;

		private static final byte BYTE9_DEVICE_ID_REQUEST = 0x01;
		private static final byte BYTE9_WEIGHT_REQUEST = 0x10;
		private static final byte BYTE9_DEVICE_ID = (byte) 0xc8;
		private static final byte BYTE9_ERROR_RETURN = 0x0f;

		private static final int NO_DATA_COUNT = 3;
		private static final int NO_DATA_TIMEOUT = 20;
		private static final int REPEAT_COUNT = 10;
		private static final int REPEAT_TIMEOUT = 200;
		private static final int MIN_WRITE_TIMEOUT = 10;

		private byte[] packet = null;
		private InputStream inputStream;
		private OutputStream oututStream;
		private boolean firstCall = true;
		private int writeTimeOut;

		public Byte9PortEventListener() {
			writeTimeOut = MIN_WRITE_TIMEOUT;
		}

		public void serialEvent(SerialPortEvent event) {
		}

		public void readDataSafe() throws IOException, InterruptedException {
			int errorCount = 0;
			while (errorCount != -1) {
				try {
					readData();
					errorCount = -1;
				} catch (IOException ioe) {
					errorCount++;
					if (errorCount >= REPEAT_COUNT) {
						throw ioe;
					} else {
						writeTimeOut *= (2f - 0.1f * (float) errorCount);
						sleep(REPEAT_TIMEOUT);
					}
				}
			}
		}

		public void readData() throws IOException {
			try {
				if (packet == null) {
					packet = new byte[BYTE9_PACKET_LENGTH];
				}

				if (inputStream == null) {
					inputStream = serialPort.getInputStream();
				}
				if (oututStream == null) {
					oututStream = serialPort.getOutputStream();
				}

				boolean ok;
				if (firstCall) {
					ok = sendPacket(BYTE9_DEVICE_ID_REQUEST);
					if (!ok) {
						throw new Exception(String.format("%s: No response.", getName()));
					}
					if (packet[4] != BYTE9_DEVICE_ID) {
						throw new Exception("Invalid device type");
					}

					firstCall = false;
					LOG.info(String.format("Mera scales found. Firmware version: %d.%d", packet[5], packet[6]));
				}

				ok = sendPacket(BYTE9_WEIGHT_REQUEST);
				if (!ok) {
					throw new Exception(String.format("%s: No response.", getName()));
				}

				if (connectLatch.getCount() > 0) {
					onMeasure((float) getSignedValue() / 1000f);
				}
			} catch (Exception e) {
				throw new IOException(String.format("Communication with '%s' failed.", getName()), e);
			}
		}

		private boolean sendPacket(byte command) throws Exception {
			// Prepare packet
			packet[0] = 0;
			packet[1] = 0;
			packet[2] = 0;
			packet[3] = command;
			packet[4] = 0;
			packet[5] = 0;
			packet[6] = 0;
			packet[7] = calcCRC();
			packet[8] = BYTE9_TERMINATOR;

			// Send request
			oututStream.flush();
			sleep(writeTimeOut);
			serialPort.setSerialPortParams(MERA_SCALES_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_MARK);
			sleep(writeTimeOut);
			oututStream.write(packet, 0, 3);
			oututStream.flush();
			sleep(writeTimeOut);
			serialPort.setSerialPortParams(MERA_SCALES_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_SPACE);
			sleep(writeTimeOut);
			oututStream.write(packet, 3, 6);
			oututStream.flush();
			sleep(writeTimeOut);
			serialPort.setSerialPortParams(MERA_SCALES_PORT_SPEED, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
			sleep(writeTimeOut);

			// Receive response
			int bytesReaded = 0;
			int bufferPos = 0;
			int noDataCount = 0;
			while (bufferPos < packet.length) {
				bytesReaded = inputStream.read(packet, bufferPos, packet.length - bufferPos);
				switch (bytesReaded) {
				case -1:
				case 0:
					noDataCount++;
					if (noDataCount >= NO_DATA_COUNT) {
						LOG.debug(String.format("%s: no more data.", getName()));
						return false;
					} else {
						sleep(NO_DATA_TIMEOUT);
					}
					break;
				default:
					noDataCount = 0;
					bufferPos += bytesReaded;
					break;
				}
			}

			// Validate response
			if (bufferPos != packet.length || packet[7] != calcCRC() || packet[8] != BYTE9_TERMINATOR || packet[3] == BYTE9_ERROR_RETURN) {
				LOG.debug(String.format("%s: invalid response received.", getName()));
				return false;
			}

			return true;
		}

		private final byte calcCRC() {
			int result = 0;
			for (int i = 0; i < 7; ++i) {
				result += this.packet[i];
			}
			return (byte) (result & 0xFF);
		}

		public final int getSignedValue() {
			final int s = this.packet[4] & 1;
			final int b1 = (this.packet[4] & 0xff) >>> 1;
			final int b2 = this.packet[5] & 0xff;
			final int b3 = this.packet[6] & 0xff;
			final int base = ((b1 << 16) | (b2 << 8) | b3) - s;

			if (s == 0) {
				return base ^ 0;
			}

			return (-base) ^ 0x7fffff;
		}

		private void onMeasure(Float value) {
			lastMeasurement = value;
			synchronized (listeners) {
				for (MeasurementListener listener : listeners) {
					listener.onMeasure(value);
				}
			}
		}

		private void sleep(long timeout) throws InterruptedException {
			if (connectLatch.await(timeout, TimeUnit.MILLISECONDS)) {
				throw new InterruptedException("Connection has been terminated.");
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

	@Override
	public Float getLastMeasurement() {
		return lastMeasurement;
	}
}
