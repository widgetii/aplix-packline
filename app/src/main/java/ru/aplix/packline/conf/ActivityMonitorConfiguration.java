package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.time.DateUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ActivityMonitor")
public class ActivityMonitorConfiguration {

	@XmlAttribute(name = "idleTreshold")
	private long idleTresholdInSeconds;

	public ActivityMonitorConfiguration() {
		idleTresholdInSeconds = 600;
	}

	public long getIdleTreshold() {
		return idleTresholdInSeconds;
	}

	public long getIdleTresholdInMillis() {
		return idleTresholdInSeconds * DateUtils.MILLIS_PER_SECOND;
	}

	public void setIdleTreshold(long idleTreshold) {
		this.idleTresholdInSeconds = idleTreshold;
	}
}
