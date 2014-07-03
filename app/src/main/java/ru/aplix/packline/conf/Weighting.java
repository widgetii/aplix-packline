package ru.aplix.packline.conf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Weighting")
public class Weighting {

	@XmlAttribute(name = "minStableWeight")
	private Float minStableWeight;

	@XmlElement(name = "Restriction", type = WeightingRestriction.class)
	private List<WeightingRestriction> weightingRestrictions;

	public Float getMinStableWeight() {
		if (minStableWeight == null || minStableWeight < 0) {
			minStableWeight = Float.valueOf(0);
		}
		return minStableWeight;
	}

	public void setMinStableWeight(Float minStableWeight) {
		this.minStableWeight = minStableWeight;
	}

	public List<WeightingRestriction> getWeightingRestrictions() {
		if (weightingRestrictions == null) {
			weightingRestrictions = new ArrayList<WeightingRestriction>();
		}
		return weightingRestrictions;
	}

	public void setWeightingRestrictions(List<WeightingRestriction> weightingRestrictions) {
		this.weightingRestrictions = weightingRestrictions;
	}
}
