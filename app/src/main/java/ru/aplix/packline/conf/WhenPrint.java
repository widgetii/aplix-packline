package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "WhenPrint")
@XmlEnum
public enum WhenPrint {

	@XmlEnumValue("MANUALLY")
	MANUALLY,

	@XmlEnumValue("BEFORE-WEIGHTING")
	BEFORE_WEIGHTING,

	@XmlEnumValue("AFTER-WEIGHTING")
	AFTER_WEIGHTING;

	public String value() {
		return name();
	}

	public static WhenPrint fromValue(String v) {
		return valueOf(v);
	}
}
