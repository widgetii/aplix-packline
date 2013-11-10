package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PrintMode")
@XmlEnum
public enum PrintMode {

	@XmlEnumValue("JAVA2D")
	JAVA2D,

	@XmlEnumValue("POSTSCRIPT")
	POSTSCRIPT,

	@XmlEnumValue("PCL")
	PCL,

	@XmlEnumValue("EZPL")
	EZPL,

	@XmlEnumValue("ZPL2")
	ZPL2;

	public String value() {
		return name();
	}

	public static PrintMode fromValue(String v) {
		return valueOf(v);
	}
}
