package ru.aplix.packline.hardware.scanner.morena;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Frame")
public class Frame {

	@XmlAttribute(name = "x", required = true)
	private int x;
	@XmlAttribute(name = "y", required = true)
	private int y;
	@XmlAttribute(name = "width", required = true)
	private int width;
	@XmlAttribute(name = "height", required = true)
	private int height;

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

}
