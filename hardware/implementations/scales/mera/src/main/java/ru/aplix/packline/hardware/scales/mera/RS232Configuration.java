package ru.aplix.packline.hardware.scales.mera;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.scales.ScalesConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class RS232Configuration implements ScalesConfiguration {

	@XmlAttribute(name = "portName")
	private String portName;
	@XmlAttribute(name = "receiveTimeout")
	private int timeout;

	public RS232Configuration() {
		portName = "COM1";
		timeout = 200;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
