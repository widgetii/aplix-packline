package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.time.DateUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ActivityMonitor")
public class ActivityMonitorConfiguration {

	@XmlAttribute(name = "idleShortTreshold")
	private long idleShortTresholdInSeconds;

	@XmlAttribute(name = "idleLongTreshold")
	private long idleLongTresholdInSeconds;

	public ActivityMonitorConfiguration() {
		idleShortTresholdInSeconds = 600;
		idleLongTresholdInSeconds = 1800;
	}

	public long getIdleShortTreshold() {
		return idleShortTresholdInSeconds;
	}

	public long getIdleShortTresholdInMillis() {
		return idleShortTresholdInSeconds * DateUtils.MILLIS_PER_SECOND;
	}

	public void setIdleShortTreshold(long idleShortTreshold) {
		this.idleShortTresholdInSeconds = idleShortTreshold;
	}

	public long getIdleLongTreshold() {
		return idleLongTresholdInSeconds;
	}

	public long getIdleLongTresholdInMillis() {
		return idleLongTresholdInSeconds * DateUtils.MILLIS_PER_SECOND;
	}

	public void setIdleLongTreshold(long idleLongTreshold) {
		this.idleLongTresholdInSeconds = idleLongTreshold;
	}
}
