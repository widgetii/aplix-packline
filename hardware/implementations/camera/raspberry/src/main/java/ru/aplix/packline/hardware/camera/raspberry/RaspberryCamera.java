package ru.aplix.packline.hardware.camera.raspberry;

import java.awt.image.BufferedImage;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
	private List<ImageListener> listeners;
	private List<PhotoCameraConnectionListener> connectionListeners;
	private boolean connectOnDemand;

	public RaspberryCamera() {
		configuration = new RaspberryCameraConfiguration();

		listeners = new Vector<ImageListener>();
		connectionListeners = new Vector<PhotoCameraConnectionListener>();

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

					String imageId = UUID.randomUUID().toString();
					String url = String.format("http://%s/take.py?%s", configuration.getHostName(), imageId);

					URL obj = new URL(url);
					HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
					if (HttpURLConnection.HTTP_OK != connection.getResponseCode()) {
						throw new Exception(connection.getResponseMessage());
					}
					
					Thread.sleep(3000);

					PhotoCameraImage pci = new PhotoCameraImage();
					pci.setImageId(imageId);
					pci.setSource(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

					synchronized (listeners) {
						for (ImageListener listener : listeners) {
							listener.onImageAcquired(pci);
						}
					}
				} catch (Exception e) {
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
