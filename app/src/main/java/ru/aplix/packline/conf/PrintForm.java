package ru.aplix.packline.conf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Form")
public class PrintForm {

	@XmlAttribute(name = "name")
	private String name;
	@XmlAttribute(name = "file")
	private String file;
	@XmlAttribute(name = "printerId")
	private String printerId;
	@XmlAttribute(name = "autoPrint")
	private Boolean autoPrint;

	@XmlTransient
	private Printer printer;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getPrinterId() {
		return printerId;
	}

	public void setPrinterId(String printerId) {
		this.printerId = printerId;
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	public Boolean getAutoPrint() {
		return autoPrint;
	}

	public void setAutoPrint(Boolean autoPrint) {
		this.autoPrint = autoPrint;
	}
}
