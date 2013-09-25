package ru.aplix.packline.hardware.barcode.basicRS232;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.barcode.BarcodeScannerConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class RS232Configuration implements BarcodeScannerConfiguration {

	@XmlAttribute(name = "portName")
	private String portName;
	@XmlAttribute(name = "portSpeed")
	private int portSpeed;
	@XmlAttribute(name = "timeout")
	private int timeout;

	public RS232Configuration() {
		portName = "COM1";
		portSpeed = 57600;
		timeout = 200;
	}

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

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
