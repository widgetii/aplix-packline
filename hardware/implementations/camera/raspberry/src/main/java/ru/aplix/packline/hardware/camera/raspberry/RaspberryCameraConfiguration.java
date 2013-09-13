package ru.aplix.packline.hardware.camera.raspberry;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.camera.PhotoCameraConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "PhotoCamera")
public class RaspberryCameraConfiguration implements PhotoCameraConfiguration {

	@XmlAttribute(name = "hostName")
	private String hostName;
	@XmlAttribute(name = "timeout")
	private int timeout;
	@XmlAttribute(name = "enabled")
	private boolean enabled;

	public RaspberryCameraConfiguration() {
		hostName = "localhost";
		timeout = 20000;
		enabled = true;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
