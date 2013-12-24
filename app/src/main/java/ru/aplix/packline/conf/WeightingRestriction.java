package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.post.PostType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "WeightingRestriction")
public class WeightingRestriction {

	@XmlAttribute(name = "postType")
	private PostType postType;
	@XmlAttribute(name = "maxWeight")
	private Float maxWeight;

	public PostType getPostType() {
		return postType;
	}

	public void setPostType(PostType postType) {
		this.postType = postType;
	}

	public Float getMaxWeight() {
		return maxWeight;
	}

	public void setMaxWeight(Float maxWeight) {
		this.maxWeight = maxWeight;
	}
}
