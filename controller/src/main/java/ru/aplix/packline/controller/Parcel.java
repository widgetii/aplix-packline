package ru.aplix.packline.controller;

public class Parcel implements Comparable<Parcel> {
	
	private String id;
	private long timestamp;

	public Parcel(String id) {
		this.id = id;
		this.timestamp = System.currentTimeMillis();
	}

	public String getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public int compareTo(Parcel o) {
		return id.compareTo(o.getId());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Parcel other = (Parcel) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
