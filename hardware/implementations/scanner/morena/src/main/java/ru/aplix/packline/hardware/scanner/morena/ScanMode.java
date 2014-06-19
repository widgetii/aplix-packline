package ru.aplix.packline.hardware.scanner.morena;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ScanMode")
@XmlEnum
public enum ScanMode {
	
	@XmlEnumValue("rgb-8")
    RGB_8,

	@XmlEnumValue("rgb-16")
    RGB_16,

	@XmlEnumValue("gray-8")
    GRAY_8,

	@XmlEnumValue("gray-16")
    GRAY_16,

	@XmlEnumValue("black-and-white")
    BLACK_AND_WHITE;

	public String value() {
		return name();
	}

	public static ScanMode fromValue(String v) {
		return valueOf(v);
	}
}
