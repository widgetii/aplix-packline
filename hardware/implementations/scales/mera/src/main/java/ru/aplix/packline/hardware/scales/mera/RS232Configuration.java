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
	@XmlAttribute(name = "readTimeout")
	private int readTimeout;
	@XmlAttribute(name = "writeTimeout")
	private int writeTimeout;

	public RS232Configuration() {
		portName = "COM1";
		readTimeout = 200;
		writeTimeout = 20;
	}

	public String getPortName() {
		return portName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int timeout) {
		this.readTimeout = timeout;
	}

	public int getWriteTimeout() {
		return writeTimeout;
	}

	public void setWriteTimeout(int writeTimeout) {
		this.writeTimeout = writeTimeout;
	}
}
