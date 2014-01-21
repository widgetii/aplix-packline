package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "Controller")
public class Controller {

	private boolean enabled;

	private String url;

	public Controller() {
		enabled = true;
		url = "";
	}

	public boolean isEnabled() {
		return enabled;
	}

	@XmlAttribute(name = "enabled")
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getUrl() {
		return url;
	}

	@XmlElement(name = "Url")
	public void setUrl(String url) {
		if (url == null || url.length() == 0) {
			url = "";
		} else {
			if (url.charAt(url.length() - 1) != '/') {
				url += "/";
			}
		}
		this.url = url;
	}
}
