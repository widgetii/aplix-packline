package ru.aplix.packline.conf;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import ru.aplix.packline.post.PostType;

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
	@XmlAttribute(name = "copies")
	private Integer copies;
	@XmlAttribute(name = "weight")
	private Float weight;
	@XmlElementWrapper(name = "PostTypeRestriction")
	@XmlElement(name = "PostType", type = PostType.class)
	private List<PostType> postTypes;

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

	public Integer getCopies() {
		return copies;
	}

	public void setCopies(Integer copies) {
		this.copies = copies;
	}

	public Float getWeight() {
		return weight;
	}

	public void setWeight(Float weight) {
		this.weight = weight;
	}

	public List<PostType> getPostTypes() {
		if (postTypes == null) {
			postTypes = new ArrayList<PostType>();
		}
		return postTypes;
	}

	public void setPostTypes(List<PostType> postTypes) {
		this.postTypes = postTypes;
	}
}
