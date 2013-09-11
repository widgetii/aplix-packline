package ru.aplix.packline.model;

public class PackingSize {

	private float length = 0f;
	private float height = 0f;
	private float width = 0f;

	public PackingSize() {
	}

	public PackingSize(float length, float height, float width) {
		this.length = length;
		this.height = height;
		this.width = width;
	}

	public float getLength() {
		return length;
	}

	public void setLength(float length) {
		this.length = length;
	}

	public float getHeight() {
		return height;
	}

	public void setHeight(float height) {
		this.height = height;
	}

	public float getWidth() {
		return width;
	}

	public void setWidth(float width) {
		this.width = width;
	}

	@Override
	public String toString() {
		return String.format("%.1f x %.1f x %.1f", length, height, width);
	}
}
