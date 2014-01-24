package ru.aplix.packline.hardware.camera.flussonic;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.camera.DVRCameraConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class FlussonicCameraConfiguration implements DVRCameraConfiguration {

	@XmlAttribute(name = "streamName")
	private String streamName;
	@XmlAttribute(name = "hostName")
	private String hostName;
	@XmlAttribute(name = "timeout")
	private int timeout;

	public FlussonicCameraConfiguration() {
		streamName = "";
		hostName = "localhost";
		timeout = 20000;
	}

	public String getStreamName() {
		return streamName;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
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
}
