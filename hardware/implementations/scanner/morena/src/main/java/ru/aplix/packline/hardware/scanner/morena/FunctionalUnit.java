package ru.aplix.packline.hardware.scanner.morena;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "FunctionalUnit")
@XmlEnum
public enum FunctionalUnit {

	@XmlEnumValue("flatbed")
	FLATBED,

	@XmlEnumValue("feeder")
	FEEDER;

	public String value() {
		return name();
	}

	public static FunctionalUnit fromValue(String v) {
		return valueOf(v);
	}
}
