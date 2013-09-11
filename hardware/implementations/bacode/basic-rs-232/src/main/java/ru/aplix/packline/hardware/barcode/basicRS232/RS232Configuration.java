package ru.aplix.packline.hardware.barcode.basicRS232;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.barcode.BarcodeScannerConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BarcodeScanner")
public class RS232Configuration implements BarcodeScannerConfiguration {

	@XmlAttribute(name = "portName")
	private String portName;
	@XmlAttribute(name = "portSpeed")
	private int portSpeed;
	@XmlAttribute(name = "enabled")
	private boolean enabled;

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public int getPortSpeed() {
		return portSpeed;
	}

	public void setPortSpeed(int portSpeed) {
		this.portSpeed = portSpeed;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
