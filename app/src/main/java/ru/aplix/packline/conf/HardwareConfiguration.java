package ru.aplix.packline.conf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Hardware")
public class HardwareConfiguration {

	@XmlAttribute(name = "reconnect")
	private int reconnectInterval;

	@XmlAttribute(name = "blockWorkOnDVRCameraError")
	private boolean blockWorkOnDVRCameraError;

	@XmlElement(name = "BarcodeScanner", type = Driver.class)
	private Driver barcodeScanner;

	@XmlElement(name = "Scales", type = Driver.class)
	private Driver scales;

	@XmlElement(name = "PhotoCamera", type = Driver.class)
	private Driver photoCamera;

	@XmlElement(name = "DVRCamera", type = Driver.class)
	private Driver dvrCamera;

	@XmlElement(name = "ImageScanner", type = Driver.class)
	private Driver imageScanner;

	@XmlElementWrapper(name = "Printers")
	@XmlElement(name = "Printer", type = Printer.class)
	private List<Printer> printers;

	@XmlElement(name = "Controller")
	private Controller controller;

	public int getReconnectInterval() {
		return reconnectInterval;
	}

	public void setReconnectInterval(int reconnectInterval) {
		this.reconnectInterval = reconnectInterval;
	}

	public boolean isBlockWorkOnDVRCameraError() {
		return blockWorkOnDVRCameraError;
	}

	public void setBlockWorkOnDVRCameraError(boolean blockWorkOnDVRCameraError) {
		this.blockWorkOnDVRCameraError = blockWorkOnDVRCameraError;
	}

	public Driver getBarcodeScanner() {
		if (barcodeScanner == null) {
			barcodeScanner = new Driver();
		}
		return barcodeScanner;
	}

	public void setBarcodeScanner(Driver barcodeScanner) {
		this.barcodeScanner = barcodeScanner;
	}

	public Driver getScales() {
		if (scales == null) {
			scales = new Driver();
		}
		return scales;
	}

	public void setScales(Driver scales) {
		this.scales = scales;
	}

	public Driver getPhotoCamera() {
		if (photoCamera == null) {
			photoCamera = new Driver();
		}
		return photoCamera;
	}

	public void setPhotoCamera(Driver photoCamera) {
		this.photoCamera = photoCamera;
	}

	public Driver getDVRCamera() {
		if (dvrCamera == null) {
			dvrCamera = new Driver();
		}
		return dvrCamera;
	}

	public void setDVRCamera(Driver dvrCamera) {
		this.dvrCamera = dvrCamera;
	}

	public Driver getImageScanner() {
		if (imageScanner == null) {
			imageScanner = new Driver();
		}
		return imageScanner;
	}

	public void setImageScanner(Driver imageScanner) {
		this.imageScanner = imageScanner;
	}

	public List<Printer> getPrinters() {
		if (printers == null) {
			printers = new ArrayList<Printer>();
		}
		return printers;
	}

	public void setPrinters(List<Printer> printers) {
		this.printers = printers;
	}

	public Printer lookupPrinter(final String printerId) {
		if (printerId == null || printers == null) {
			return null;
		}
		Printer printer = (Printer) CollectionUtils.find(printers, new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return printerId.equals(((Printer) item).getId());
			}
		});
		return printer;
	}

	public Printer lookupPrinter(final PrintMode printMode) {
		if (printMode == null || printers == null) {
			return null;
		}
		Printer printer = (Printer) CollectionUtils.find(printers, new Predicate() {
			@Override
			public boolean evaluate(Object item) {
				return printMode.equals(((Printer) item).getPrintMode());
			}
		});
		return printer;
	}

	public Controller getController() {
		if (controller == null) {
			controller = new Controller();
		}
		return controller;
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}
}
