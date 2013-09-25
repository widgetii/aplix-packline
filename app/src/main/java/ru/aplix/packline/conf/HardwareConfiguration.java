package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Hardware")
public class HardwareConfiguration {

	@XmlAttribute(name = "reconnect")
	private int reconnectInterval;

	@XmlElement(name = "BarcodeScanner", type = Driver.class)
	private Driver barcodeScanner;

	@XmlElement(name = "Scales", type = Driver.class)
	private Driver scales;

	@XmlElement(name = "PhotoCamera", type = Driver.class)
	private Driver photoCamera;

	public int getReconnectInterval() {
		return reconnectInterval;
	}

	public void setReconnectInterval(int reconnectInterval) {
		this.reconnectInterval = reconnectInterval;
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
}
