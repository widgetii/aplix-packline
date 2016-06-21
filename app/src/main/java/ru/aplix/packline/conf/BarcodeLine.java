package ru.aplix.packline.conf;

import ru.aplix.converters.fr2afop.fr.type.BarCodeType;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "BarcodeLine")
public class BarcodeLine {

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
			quantity = new ArrayList<>();
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
