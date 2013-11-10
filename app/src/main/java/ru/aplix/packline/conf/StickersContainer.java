package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "StickersContainer")
public class StickersContainer {

	@XmlElement(name = "ForContainers")
	private Stickers stickersForContainers;

	@XmlElement(name = "ForCustomers")
	private Stickers stickersForCustomers;

	public Stickers getForContainers() {
		if (stickersForContainers == null) {
			stickersForContainers = new Stickers();
		}
		return stickersForContainers;
	}

	public void setForContainers(Stickers stickersForContainers) {
		this.stickersForContainers = stickersForContainers;
	}

	public Stickers getForCustomers() {
		if (stickersForCustomers == null) {
			stickersForCustomers = new Stickers();
		}
		return stickersForCustomers;
	}

	public void setForCustomers(Stickers stickersForCustomers) {
		this.stickersForCustomers = stickersForCustomers;
	}
}
