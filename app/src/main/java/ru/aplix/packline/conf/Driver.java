package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ru.aplix.packline.utils.StringXmlAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
public class Driver {

	@XmlAttribute(name = "name")
	private String name;

	@XmlAttribute(name = "enabled")
	private boolean enabled;

	@XmlElement(name = "Configuration")
	@XmlJavaTypeAdapter(StringXmlAdapter.class)
	private String configuration;

	public Driver() {
		name = "n/a";
		enabled = true;
		configuration = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getConfiguration() {
		return configuration;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
}
