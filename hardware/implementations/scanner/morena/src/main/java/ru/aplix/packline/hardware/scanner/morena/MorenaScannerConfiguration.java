package ru.aplix.packline.hardware.scanner.morena;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import ru.aplix.packline.hardware.scanner.ImageScannerConfiguration;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Configuration")
public class MorenaScannerConfiguration implements ImageScannerConfiguration {

	@XmlAttribute(name = "name")
	private String name;
	@XmlAttribute(name = "resolution")
	private Integer resolution;
	@XmlAttribute(name = "duplex")
	private Boolean duplex;
	@XmlAttribute(name = "functionalUnit")
	private FunctionalUnit functionalUnit;
	@XmlAttribute(name = "scanMode")
	private ScanMode scanMode;
	@XmlAttribute(name = "tryNextPage")
	private Boolean tryNextPage;
	@XmlElement(name = "Frame")
	private Frame frame;

	public MorenaScannerConfiguration() {
		name = "";
		resolution = null;
		duplex = false;
		functionalUnit = FunctionalUnit.FLATBED;
		scanMode = ScanMode.RGB_8;
		tryNextPage = false;
		frame = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getResolution() {
		return resolution;
	}

	public void setResolution(Integer resolution) {
		this.resolution = resolution;
	}

	public Boolean getDuplex() {
		return duplex;
	}

	public void setDuplex(Boolean duplex) {
		this.duplex = duplex;
	}

	public FunctionalUnit getFunctionalUnit() {
		return functionalUnit;
	}

	public void setFunctionalUnit(FunctionalUnit functionalUnit) {
		this.functionalUnit = functionalUnit;
	}

	public ScanMode getScanMode() {
		return scanMode;
	}

	public void setScanMode(ScanMode scanMode) {
		this.scanMode = scanMode;
	}

	public Boolean getTryNextPage() {
		return tryNextPage;
	}

	public void setTryNextPage(Boolean tryNextPage) {
		this.tryNextPage = tryNextPage;
	}

	public Frame getFrame() {
		return frame;
	}

	public void setFrame(Frame frame) {
		this.frame = frame;
	}

}
