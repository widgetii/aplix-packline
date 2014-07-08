package ru.aplix.packline.hardware.barcode.basicRS232;

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.barcode.BarcodeListener;
import ru.aplix.packline.hardware.barcode.BarcodeScanner;
import ru.aplix.packline.hardware.barcode.BarcodeScannerConnectionListener;

public class BasicRS232BarcodeScanner2 implements BarcodeScanner<RS232Configuration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ReentrantLock connectionLock;
	private SerialPort serialPort;
	private volatile boolean isConnected = false;

	private RS232Configuration configuration;
	private List<BarcodeListener> listeners;
	private List<BarcodeScannerConnectionListener> connectionListeners;
	private boolean connectOnDemand;

	public BasicRS232BarcodeScanner2() {
		configuration = new RS232Configuration();

		listeners = new Vector<BarcodeListener>();
		connectionListeners = new Vector<BarcodeScannerConnectionListener>();

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
				connectionLock.lock();
				try {
					try {
						synchronized (CommPortIdentifier.class) {
							if (!isConnected()) {
								CommPortIdentifier portId = CommPortIdentifier.getPortIdentifier(configuration.getPortName());
								if (portId == null) {
									throw new RuntimeException(String.format("Port '%s' not found.", configuration.getPortName()));
								}

								serialPort = (SerialPort) portId.open(getClass().getName(), 2000);
								serialPort.addEventListener(new BarcodeSerialPortEventListener());
								serialPort.notifyOnDataAvailable(true);
								serialPort.setSerialPortParams(configuration.getPortSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
										SerialPort.PARITY_NONE);
								serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);

								isConnected = true;
							}
						}

						synchronized (connectionListeners) {
							for (BarcodeScannerConnectionListener listener : connectionListeners) {
								listener.onConnected();
							}
						}
					} catch (Exception e) {
						synchronized (connectionListeners) {
							for (BarcodeScannerConnectionListener listener : connectionListeners) {
								listener.onConnectionFailed();
							}
						}

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
				try {
					connectionLock.lock();
					try {
						if (isConnected) {
							serialPort.removeEventListener();
							serialPort.close();

							isConnected = false;
						}

						synchronized (connectionListeners) {
							for (BarcodeScannerConnectionListener listener : connectionListeners) {
								listener.onDisconnected();
							}
						}
					} finally {
						connectionLock.unlock();
					}
				} catch (Exception e) {
					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
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

	private class BarcodeSerialPortEventListener implements SerialPortEventListener {

		private static final int BUFFER_LENGTH = 1024;
		private byte[] buffer = null;
		private int bufferPos = 0;
		private InputStream inputStream;

		public void serialEvent(SerialPortEvent event) {
			try {
				switch (event.getEventType()) {
				case SerialPortEvent.DATA_AVAILABLE:
					if (buffer == null) {
						buffer = new byte[BUFFER_LENGTH];
					}

					if (inputStream == null) {
						inputStream = serialPort.getInputStream();
					}

					// Add incoming data to our buffer
					int readLenght = Math.min(buffer.length - bufferPos, inputStream.available());
					int bytesReaded = inputStream.read(buffer, bufferPos, readLenght);
					if (bytesReaded <= 0) {
						break;
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

					break;
				default:
					break;
				}
			} catch (IOException e) {
				LOG.error(String.format("Error in %s '%s'", getName(), configuration.getPortName()), e);
			}
		}

		private void onReadString(String value) {
			synchronized (listeners) {
				for (BarcodeListener listener : listeners) {
					listener.onCatchBarcode(value);
				}
			}
		}
	}

	public void addBarcodeListener(BarcodeListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	public void removeBarcodeListener(BarcodeListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public void addConnectionListener(BarcodeScannerConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.add(connectionListener);
		}
	}

	public void removeConnectionListener(BarcodeScannerConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.remove(connectionListener);
		}
	}
}
