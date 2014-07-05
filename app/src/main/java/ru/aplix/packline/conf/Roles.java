package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Roles")
public class Roles {

	@XmlAttribute(name = "acceptance")
	private Boolean acceptance;

	@XmlAttribute(name = "packing")
	private Boolean packing;

	@XmlAttribute(name = "labeling")
	private Boolean labeling;

	@XmlAttribute(name = "gluing")
	private Boolean gluing;

	@XmlAttribute(name = "warranty")
	private Boolean warranty;

	@XmlAttribute(name = "returns")
	private Boolean returns;

	public Boolean getAcceptance() {
		return acceptance != null ? acceptance : true;
	}

	public void setAcceptance(Boolean acceptance) {
		this.acceptance = acceptance;
	}

	public Boolean getPacking() {
		return packing != null ? packing : true;
	}

	public void setPacking(Boolean packing) {
		this.packing = packing;
	}

	public Boolean getLabeling() {
		return labeling != null ? labeling : true;
	}

	public void setLabeling(Boolean labeling) {
		this.labeling = labeling;
	}

	public Boolean getGluing() {
		return gluing != null ? gluing : true;
	}

	public void setGluing(Boolean gluing) {
		this.gluing = gluing;
	}

	public Boolean getWarranty() {
		return warranty != null ? warranty : true;
	}

	public void setWarranty(Boolean warranty) {
		this.warranty = warranty;
	}

	public Boolean getReturns() {
		return returns != null ? returns : true;
	}

	public void setReturns(Boolean returns) {
		this.returns = returns;
	}

}