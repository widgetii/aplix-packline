package ru.aplix.packline.hardware.camera.raspberry;

import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.camera.ImageListener;
import ru.aplix.packline.hardware.camera.PhotoCamera;
import ru.aplix.packline.hardware.camera.PhotoCameraConnectionListener;
import ru.aplix.packline.hardware.camera.PhotoCameraImage;

public class RaspberryCamera implements PhotoCamera<RaspberryCameraConfiguration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private ReentrantLock connectionLock;
	private volatile boolean isConnected = false;

	private RaspberryCameraConfiguration configuration;
	private Set<ImageListener> listeners;
	private Set<PhotoCameraConnectionListener> connectionListeners;
	private boolean connectOnDemand;

	private static final String RESPONSE_OK = "OK";
	private static final int FAST_RESPONSE_DELAY = 3000;

	public RaspberryCamera() {
		configuration = new RaspberryCameraConfiguration();

		listeners = new HashSet<ImageListener>();
		connectionListeners = new HashSet<PhotoCameraConnectionListener>();

		connectionLock = new ReentrantLock();

		connectOnDemand = false;
	}

	public String getName() {
		return getClass().getSimpleName();
	}

	public void setConfiguration(String config) throws IllegalArgumentException {
		try {
			JAXBContext inst = JAXBContext.newInstance(RaspberryCameraConfiguration.class);
			Unmarshaller unmarshaller = inst.createUnmarshaller();
			configuration = (RaspberryCameraConfiguration) unmarshaller.unmarshal(new StringReader(config));
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void setConfiguration(RaspberryCameraConfiguration config) throws IllegalArgumentException {
		configuration = config;
	}

	public RaspberryCameraConfiguration getConfiguration() {
		return configuration;
	}

	public void connect() {
		new Thread(new Runnable() {
			public void run() {
				connectionLock.lock();
				try {
					try {
						if (!isConnected()) {
							isConnected = true;
						}

						synchronized (connectionListeners) {
							for (PhotoCameraConnectionListener listener : connectionListeners) {
								listener.onConnected();
							}
						}
					} catch (Exception e) {
						synchronized (connectionListeners) {
							for (PhotoCameraConnectionListener listener : connectionListeners) {
								listener.onConnectionFailed();
							}
						}

						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
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
							isConnected = false;
						}

						synchronized (connectionListeners) {
							for (PhotoCameraConnectionListener listener : connectionListeners) {
								listener.onDisconnected();
							}
						}
					} catch (Exception e) {
						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
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

	public void addConnectionListener(PhotoCameraConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.add(connectionListener);
		}
	}

	public void removeConnectionListener(PhotoCameraConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.remove(connectionListener);
		}
	}

	public void makePhoto() {
		new Thread(new Runnable() {
			public void run() {
				try {
					if (!isConnected()) {
						throw new Exception("No connection with camera.");
					}

					// Generate image UUID
					String imageId = UUID.randomUUID().toString();
					String url = String.format("http://%s/take/%s", configuration.getHostName(), imageId);

					// Loop until successful response will be received
					// or timeout will occur
					String response = null;
					long time = System.currentTimeMillis();
					boolean timeout = false;
					boolean repeated = false;
					do {
						if (repeated) {
							Thread.sleep(1000);
						}

						// Send HTTP request to device
						LOG.debug("Sending image request:\n" + url);
						URL obj = new URL(url);
						HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
						connection.setConnectTimeout(configuration.getTimeout());
						if (HttpURLConnection.HTTP_OK != connection.getResponseCode()) {
							throw new Exception(connection.getResponseMessage());
						}

						// Parse device response
						response = IOUtils.toString(connection.getInputStream()).trim();
						LOG.debug(String.format("Received response from device: %s.", response));

						// Check whether timeout has occurred
						if (configuration.getTimeout() > 0) {
							timeout = System.currentTimeMillis() - time > configuration.getTimeout();
						}

						repeated = true;
					} while (!RESPONSE_OK.equals(response) && !timeout);

					if (RESPONSE_OK.equals(response) && !timeout) {
						// If successful response has been received too fast
						// then wait a bit to show the progress
						long diff = FAST_RESPONSE_DELAY - (System.currentTimeMillis() - time);
						if (diff > 0) {
							Thread.sleep(diff);
						}

						// If response is successful then generate mock image
						PhotoCameraImage pci = new PhotoCameraImage();
						pci.setImageId(imageId);
						pci.setSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

						// Notify listeners about successful acquisition
						synchronized (listeners) {
							for (ImageListener listener : listeners) {
								listener.onImageAcquired(pci);
							}
						}
					} else {
						// Notify listeners that image acquisition has failed
						synchronized (listeners) {
							for (ImageListener listener : listeners) {
								listener.onImageAcquisitionFailed();
							}
						}
					}
				} catch (Exception e) {
					// Notify listeners that image acquisition has failed
					synchronized (listeners) {
						for (ImageListener listener : listeners) {
							listener.onImageAcquisitionFailed();
						}
					}

					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
				}
			}
		}).start();
	}

	public void addImageListener(ImageListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	public void removeImageListener(ImageListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
}
