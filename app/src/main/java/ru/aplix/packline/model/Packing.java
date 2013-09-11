package ru.aplix.packline.model;

public class Packing {

	private Long packingId;
	private String packingCode;
	private PackingType packingType;
	private PackingSize packingSize;
	private Float totalWeight;

	public Long getPackingId() {
		return packingId;
	}

	public void setPackingId(Long packingId) {
		this.packingId = packingId;
	}

	public String getPackingCode() {
		return packingCode;
	}

	public void setPackingCode(String packingCode) {
		this.packingCode = packingCode;
	}

	public PackingType getPackingType() {
		return packingType;
	}

	public void setPackingType(PackingType packingType) {
		this.packingType = packingType;
	}

	public PackingSize getPackingSize() {
		if (packingSize == null) {
			packingSize = new PackingSize();
		}
		return packingSize;
	}

	public void setPackingSize(PackingSize packingSize) {
		this.packingSize = packingSize;
	}

	public Float getTotalWeight() {
		return totalWeight;
	}

	public void setTotalWeight(Float totalWeight) {
		this.totalWeight = totalWeight;
	}
}
