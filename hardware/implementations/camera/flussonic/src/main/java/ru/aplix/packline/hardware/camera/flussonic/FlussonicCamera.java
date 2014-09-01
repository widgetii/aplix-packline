package ru.aplix.packline.hardware.camera.flussonic;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.camera.DVRCamera;
import ru.aplix.packline.hardware.camera.DVRCameraConnectionListener;
import ru.aplix.packline.hardware.camera.RecorderListener;

public class FlussonicCamera implements DVRCamera<FlussonicCameraConfiguration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private static final int CMD_STREAM_HEALTH = 100;
	private static final int CMD_DVR_ENABLE = 200;
	private static final int CMD_DVR_DISABLE = 300;
	private static final int CMD_TERMINATE = -1;

	private CountDownLatch connectLatch = null;

	private volatile boolean isConnected = false;
	private volatile boolean isRecording = false;

	private FlussonicCameraConfiguration configuration;
	private List<RecorderListener> listeners;
	private List<DVRCameraConnectionListener> connectionListeners;
	protected LinkedList<Integer> commands;
	private boolean connectOnDemand;

	public FlussonicCamera() {
		configuration = new FlussonicCameraConfiguration();

		listeners = new Vector<RecorderListener>();
		connectionListeners = new Vector<DVRCameraConnectionListener>();

		commands = new LinkedList<Integer>();
		connectOnDemand = false;
	}

	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void setConfiguration(String config) throws IllegalArgumentException {
		try {
			JAXBContext inst = JAXBContext.newInstance(FlussonicCameraConfiguration.class);
			Unmarshaller unmarshaller = inst.createUnmarshaller();
			configuration = (FlussonicCameraConfiguration) unmarshaller.unmarshal(new StringReader(config));
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void setConfiguration(FlussonicCameraConfiguration config) throws IllegalArgumentException {
		configuration = config;
	}

	@Override
	public FlussonicCameraConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void connect() {
		new Thread(new Runnable() {
			public void run() {
				boolean doRun = false;

				// Connection block
				try {
					if (!isConnected()) {
						connectLatch = new CountDownLatch(1);

						isConnected = true;
						doRun = true;

						sendCommand(CMD_STREAM_HEALTH, true);
					}

					synchronized (connectionListeners) {
						for (DVRCameraConnectionListener listener : connectionListeners) {
							listener.onConnected();
						}
					}
				} catch (Exception e) {
					synchronized (connectionListeners) {
						for (DVRCameraConnectionListener listener : connectionListeners) {
							listener.onConnectionFailed();
						}
					}

					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
				}

				if (!doRun) {
					return;
				}

				// Running block
				while (connectLatch.getCount() > 0) {
					try {
						processCommand();
					} catch (Exception e) {
						LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
					}
					waitForCommands();
				}

				// Disconnection block
				try {
					isConnected = false;
					connectLatch = null;

					synchronized (connectionListeners) {
						for (DVRCameraConnectionListener listener : connectionListeners) {
							listener.onDisconnected();
						}
					}
				} catch (Exception e) {
					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
				}
			}
		}).start();
	}

	@Override
	public void disconnect() {
		sendCommand(CMD_TERMINATE, false);

		if (!isConnected()) {
			connect();
		}
	}

	@Override
	public boolean isConnected() {
		return isConnected;
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
	public void addConnectionListener(DVRCameraConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.add(connectionListener);
		}
	}

	@Override
	public void removeConnectionListener(DVRCameraConnectionListener connectionListener) {
		synchronized (connectionListeners) {
			connectionListeners.remove(connectionListener);
		}
	}

	@Override
	public void addRecorderListener(RecorderListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}

		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	@Override
	public void removeRecorderListener(RecorderListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	@Override
	public void enableRecording() {
		sendCommand(CMD_DVR_ENABLE, false);

		if (!isConnected()) {
			connect();
		}
	}

	@Override
	public void disableRecording() {
		sendCommand(CMD_DVR_DISABLE, false);

		if (!isConnected()) {
			connect();
		}
	}

	@Override
	public boolean isRecording() {
		return isRecording;
	}

	private void waitForCommands() {
		synchronized (commands) {
			if ((commands.size() == 0) && (connectLatch.getCount() > 0)) {
				try {
					commands.wait();
				} catch (InterruptedException ie) {
					ie.printStackTrace();
				}
			}
		}
	}

	private void sendCommand(int cmd, boolean begin) {
		synchronized (commands) {
			if (begin) {
				commands.add(0, new Integer(cmd));
			} else {
				commands.add(new Integer(cmd));
			}
			commands.notify();
		}
	}

	private Integer getCommand() {
		synchronized (commands) {
			return commands.poll();
		}
	}

	private void processCommand() {
		Integer cmd = getCommand();
		if (cmd == null) {
			return;
		}

		switch (cmd) {
		case CMD_STREAM_HEALTH:
			isRecording = sendHttpRequest("stream_health");
			break;
		case CMD_DVR_ENABLE:
			if (sendHttpRequest("dvr_enable")) {
				isRecording = true;

				synchronized (listeners) {
					for (RecorderListener listener : listeners) {
						listener.onRecordingStarted();
					}
				}
			} else {
				synchronized (listeners) {
					for (RecorderListener listener : listeners) {
						listener.onRecordingFailed();
					}
				}
			}
			break;
		case CMD_DVR_DISABLE:
			if (sendHttpRequest("dvr_disable")) {
				isRecording = false;

				synchronized (listeners) {
					for (RecorderListener listener : listeners) {
						listener.onRecordingStopped();
					}
				}
			} else {
				synchronized (listeners) {
					for (RecorderListener listener : listeners) {
						listener.onRecordingFailed();
					}
				}
			}
			break;
		case CMD_TERMINATE:
			connectLatch.countDown();
			break;
		}
	}

	private boolean sendHttpRequest(String requestName) {
		boolean result = false;
		try {
			String url = String.format("http://%s/flussonic/api/%s/%s", configuration.getHostName(), requestName, configuration.getStreamName());
			LOG.debug(String.format("Requesting DVR camera: %s", url));

			URL obj = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
			String authorization = configuration.getUserName() + ":" + configuration.getPassword();
			String encoded = Base64.encodeBase64String(authorization.getBytes());
			connection.setRequestProperty("Authorization", "Basic " + encoded);
			connection.setConnectTimeout(configuration.getTimeout());
			if (HttpURLConnection.HTTP_OK != connection.getResponseCode()) {
				throw new Exception(connection.getResponseMessage());
			}
			
			LOG.debug("Camera response: " + connection.getResponseMessage());

			result = true;
		} catch (Exception e) {
			LOG.error(String.format("Error in %s '%s'", getName(), configuration.getHostName()), e);
		}
		return result;
	}
}
