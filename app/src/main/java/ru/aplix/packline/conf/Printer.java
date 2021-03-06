package ru.aplix.packline.conf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Printer")
public class Printer {

	@XmlAttribute(name = "id")
	private String id;

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "ipAddress")
	private String ipAddress;

	@XmlAttribute(name = "port")
	private int port;

	@XmlAttribute(name = "printMode")
	private PrintMode printMode;

	@XmlElement(name = "MediaAttribute")
	private List<String> mediaAttributes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public PrintMode getPrintMode() {
		return printMode;
	}

	public void setPrintMode(PrintMode printMode) {
		this.printMode = printMode;
	}

	public List<String> getMediaAttributes() {
		if (mediaAttributes == null) {
			mediaAttributes = new ArrayList<String>();
		}
		return mediaAttributes;
	}

	public void setMediaAttributes(List<String> mediaAttributes) {
		this.mediaAttributes = mediaAttributes;
	}
}
