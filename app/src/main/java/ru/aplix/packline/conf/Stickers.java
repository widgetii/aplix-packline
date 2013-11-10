package ru.aplix.packline.conf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import ru.aplix.converters.fr2afop.fr.type.BarCodeType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Stickers")
public class Stickers {

	@XmlAttribute(name = "printerId")
	private String printerId;
	
	@XmlAttribute(name = "barCodeType")
	private BarCodeType barCodeType;

	@XmlElement(name = "Quantity")
	private List<Integer> quantity;

	@XmlTransient
	private Printer printer;

	public String getPrinterId() {
		return printerId;
	}

	public void setPrinterId(String printerId) {
		this.printerId = printerId;
	}

	public List<Integer> getQuantity() {
		if (quantity == null) {
			quantity = new ArrayList<Integer>();
		}
		return quantity;
	}

	public void setQuantity(List<Integer> quantity) {
		this.quantity = quantity;
	}

	public Printer getPrinter() {
		return printer;
	}

	public void setPrinter(Printer printer) {
		this.printer = printer;
	}

	public BarCodeType getBarCodeType() {
		return barCodeType;
	}

	public void setBarCodeType(BarCodeType barCodeType) {
		this.barCodeType = barCodeType;
	}
}
