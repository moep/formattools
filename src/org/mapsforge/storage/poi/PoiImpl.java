package org.mapsforge.storage.poi;

import org.mapsforge.core.GeoCoordinate;

public class PoiImpl implements PointOfInterest{
	private final long id;
	private final double latitude;
	private final double longitude;
	private final String name;
	private final PoiCategory category;

	public PoiImpl(long id, double latitude, double longitude, String name,	PoiCategory category) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.category = category;
	}
	
	
	@Override
	public long getId() {
		return this.id;
	}

	@Override
	public double getLatitude() {
		return this.latitude;
	}

	@Override
	public double getLongitude() {
		return this.longitude;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PoiCategory getCategory() {
		return this.category;
	}

	@Override
	public GeoCoordinate getGeoCoordinate() {
		return new GeoCoordinate(this.latitude, this.longitude);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("POI: (").append(this.latitude).append(',').append(this.longitude);
		sb.append(") ").append(this.name).append(' ').append(this.category.getID());
		return sb.toString();
	}
	
}
