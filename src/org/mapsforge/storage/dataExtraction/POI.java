/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.storage.dataExtraction;

import java.util.LinkedList;
import java.util.List;

import org.mapsforge.storage.poi.PointOfInterest;

/**
 * Container class for POIs in the map database. This class is not intended to be mixed with
 * {@link PointOfInterest} or with any other class of the POI package.
 * 
 * 
 * @author Karsten Groll
 * 
 */
public class POI {
	private String poiSignature;
	// Position
	private int latDiff;
	private int lonDiff;
	private byte specialByte;
	private List<Integer> tagID;
	private byte flags;
	private String name;
	private int elevation;
	private String houseNumber;

	/**
	 * The constructor.
	 */
	public POI() {
		this.tagID = new LinkedList<Integer>();
	}

	/**
	 * @return Layer (OSM-Tag: layer=...) + 5 (to avoid negative values).
	 */
	int getLayer() {
		return this.specialByte & 0xf0 + 5;
	}

	/**
	 * @return Amount of tags for the POI.
	 */
	int getAmountOfTags() {
		return this.specialByte & 0x0f;
	}

	boolean isPOINameFlagSet() {
		return (this.flags & 0x80) != 0;
	}

	boolean isElevationFlagSet() {
		return (this.flags & 0x40) != 0;
	}

	boolean isHouseNumberFlagSet() {
		return (this.flags & 0x20) != 0;
	}

	void addTagID(int tagID1) {
		this.tagID.add(tagID1);
	}

	/**
	 * @return the poiSignature
	 */
	public String getPoiSignature() {
		return poiSignature;
	}

	/**
	 * @param poiSignature
	 *            the poiSignature to set
	 */
	public void setPoiSignature(String poiSignature) {
		this.poiSignature = poiSignature;
	}

	/**
	 * @return the flags
	 */
	public byte getFlags() {
		return flags;
	}

	/**
	 * @param flags
	 *            the flags to set
	 */
	public void setFlags(byte flags) {
		this.flags = flags;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the elevation
	 */
	public int getElevation() {
		return elevation;
	}

	/**
	 * @param elevation
	 *            the elevation to set
	 */
	public void setElevation(int elevation) {
		this.elevation = elevation;
	}

	/**
	 * @return the houseNumber
	 */
	public String getHouseNumber() {
		return houseNumber;
	}

	/**
	 * @param houseNumber
	 *            the houseNumber to set
	 */
	public void setHouseNumber(String houseNumber) {
		this.houseNumber = houseNumber;
	}

	/**
	 * @return the latDiff
	 */
	public int getLatDiff() {
		return latDiff;
	}

	/**
	 * @return the lonDiff
	 */
	public int getLonDiff() {
		return lonDiff;
	}

	/**
	 * Sets the POI's position difference to the top left corner.
	 * 
	 * @param latDiff
	 *            Latitude difference
	 * @param lonDiff
	 *            Longitude difference
	 */
	public void setPosition(int latDiff, int lonDiff) {
		this.latDiff = latDiff;
		this.lonDiff = lonDiff;
	}

	/**
	 * @return the specialByte
	 */
	public byte getSpecialByte() {
		return specialByte;
	}

	/**
	 * @param specialByte
	 *            the specialByte to set
	 */
	public void setSpecialByte(byte specialByte) {
		this.specialByte = specialByte;
	}

}
