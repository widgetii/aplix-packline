package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "PrintMode")
@XmlEnum
public enum PrintMode {

	@XmlEnumValue("JAVA2D")
	JAVA2D,

	@XmlEnumValue("JAVA2D_WO_COPIES")
	JAVA2D_WO_COPIES,

	@XmlEnumValue("POSTSCRIPT")
	POSTSCRIPT,

	@XmlEnumValue("PCL")
	PCL,

	@XmlEnumValue("PDF")
	PDF,

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
