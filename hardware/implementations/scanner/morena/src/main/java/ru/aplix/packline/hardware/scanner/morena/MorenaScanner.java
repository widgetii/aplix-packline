package ru.aplix.packline.hardware.scanner.morena;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ru.aplix.packline.hardware.scanner.ImageListener;
import ru.aplix.packline.hardware.scanner.ImageScanner;
import ru.aplix.packline.hardware.scanner.ImageScannerConnectionListener;
import eu.gnome.morena.Configuration;
import eu.gnome.morena.Device;
import eu.gnome.morena.Manager;
import eu.gnome.morena.Scanner;
import eu.gnome.morena.TransferDoneListener;

public class MorenaScanner implements ImageScanner<MorenaScannerConfiguration> {

	private final Log LOG = LogFactory.getLog(getClass());

	private MorenaScannerConfiguration configuration;
	private List<ImageListener> listeners;
	private List<ImageScannerConnectionListener> connectionListeners;
	private boolean connectOnDemand;
	private boolean isConnected;

	private Manager manager = null;
	private Device device = null;

	static {
		Configuration.setLogLevel(Level.WARNING);
	}

	public MorenaScanner() {
		configuration = new MorenaScannerConfiguration();

		listeners = new Vector<ImageListener>();
		connectionListeners = new Vector<ImageScannerConnectionListener>();

		isConnected = false;
		connectOnDemand = false;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void setConfiguration(String config) throws IllegalArgumentException {
		try {
			JAXBContext inst = JAXBContext.newInstance(MorenaScannerConfiguration.class);
			Unmarshaller unmarshaller = inst.createUnmarshaller();
			configuration = (MorenaScannerConfiguration) unmarshaller.unmarshal(new StringReader(config));
		} catch (JAXBException e) {
			throw new IllegalArgumentException(e);
		}
	}

	@Override
	public void setConfiguration(MorenaScannerConfiguration config) throws IllegalArgumentException {
		configuration = config;
	}

	@Override
	public MorenaScannerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public void connect() {
		if (!isConnected()) {
			if (manager == null) {
				manager = Manager.getInstance();
			}

			device = null;
			List<Device> devices = manager.listDevices();
			for (Device d : devices) {
				if (d.toString().equals(configuration.getName())) {
					device = d;
				}
			}

			if (device != null) {
				isConnected = true;

				for (ImageScannerConnectionListener listener : connectionListeners) {
					listener.onConnected();
				}
			} else {
				for (ImageScannerConnectionListener listener : connectionListeners) {
					listener.onConnectionFailed();
				}
			}
		}
	}

	private void configureScanner(Scanner scanner) {
		if (configuration.getResolution() != null) {
			scanner.setResolution(configuration.getResolution());
		}
		if (configuration.getDuplex() != null) {
			scanner.setDuplexEnabled(configuration.getDuplex());
		}
		if (configuration.getFunctionalUnit() != null) {
			int fu = -1;
			switch (configuration.getFunctionalUnit()) {
			case FLATBED:
				fu = scanner.getFlatbedFunctionalUnit();
				break;
			case FEEDER:
				fu = scanner.getFeederFunctionalUnit();
				break;
			}
			if (fu != -1) {
				scanner.setFunctionalUnit(fu);
			} else {
				LOG.warn(String.format("Functional unit '%s' not supported by '%s'", configuration.getFunctionalUnit(), configuration.getName()));
			}
		}
		if (configuration.getScanMode() != null) {
			switch (configuration.getScanMode()) {
			case RGB_8:
				scanner.setMode(Scanner.RGB_8);
				break;
			case RGB_16:
				scanner.setMode(Scanner.RGB_16);
				break;
			case GRAY_8:
				scanner.setMode(Scanner.GRAY_8);
				break;
			case GRAY_16:
				scanner.setMode(Scanner.GRAY_16);
				break;
			case BLACK_AND_WHITE:
				scanner.setMode(Scanner.BLACK_AND_WHITE);
				break;
			}
		}
	}

	@Override
	public void disconnect() {
		isConnected = false;
		for (ImageScannerConnectionListener listener : connectionListeners) {
			listener.onDisconnected();
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
	public void addConnectionListener(ImageScannerConnectionListener connectionListener) {
		connectionListeners.add(connectionListener);
	}

	@Override
	public void removeConnectionListener(ImageScannerConnectionListener connectionListener) {
		connectionListeners.remove(connectionListener);
	}

	@Override
	public void acquireImage() {
		try {
			if (device != null) {
				FunctionalUnit selectedFunctionalUnit = null;
				if (device instanceof Scanner) {
					Scanner scanner = (Scanner) device;
					configureScanner(scanner);

					if (Integer.compare(scanner.getFunctionalUnit(), scanner.getFeederFunctionalUnit()) == 0) {
						selectedFunctionalUnit = FunctionalUnit.FEEDER;
					} else {
						selectedFunctionalUnit = FunctionalUnit.FLATBED;
					}
				}
				device.startTransfer(new ImageTransferHandler(selectedFunctionalUnit));
			} else {
				for (ImageListener listener : listeners) {
					listener.onImageAcquisitionFailed();
				}
			}
		} catch (Exception e) {
			LOG.error(String.format("Error in %s '%s'", getName(), configuration.getName()), e);
		}
	}

	@Override
	public void addImageListener(ImageListener listener) {
		listeners.add(listener);

		if (connectOnDemand && (listeners.size() > 0) && !isConnected()) {
			connect();
		}
	}

	@Override
	public void removeImageListener(ImageListener listener) {
		listeners.remove(listener);
	}

	/**
	 * 
	 */
	private class ImageTransferHandler implements TransferDoneListener {

		private final Pattern pattern = Pattern.compile("feeder.*empty", Pattern.CASE_INSENSITIVE);

		private FunctionalUnit currentFunctionalUnit;

		public ImageTransferHandler(FunctionalUnit fu) {
			currentFunctionalUnit = fu;
		}

		@Override
		public void transferDone(File file) {
			if (file != null) {
				try {
					LOG.debug(String.format("Reading acquired image from '%s'", file.getAbsolutePath()));
					Image image = ImageIO.read(file);

					for (ImageListener listener : listeners) {
						listener.onImageAcquired(image);
					}

					if (!FunctionalUnit.FEEDER.equals(currentFunctionalUnit)) {
						for (ImageListener listener : listeners) {
							listener.onImageAcquisitionCompleted();
						}
					}
				} catch (IOException e) {
					LOG.error(String.format("Error in %s '%s'", getName(), configuration.getName()), e);
				}
			}
		}

		@Override
		public void transferFailed(int code, String error) {
			if (FunctionalUnit.FEEDER.equals(currentFunctionalUnit)) {
				if (pattern.matcher(error).find()) {
					for (ImageListener listener : listeners) {
						listener.onImageAcquisitionCompleted();
					}
					return;
				}
			}

			LOG.error(String.format("Image acquisition failed with error '%s' and code '%d'", error, code));

			for (ImageListener listener : listeners) {
				listener.onImageAcquisitionFailed();
			}
		}
	}
}
