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
package org.mapsforge.applications.debug;

import java.util.ArrayList;
import static org.mapsforge.applications.debug.MapFormatReader.getHex;

/**
 * This class is a representation of a map file. It contains its basic structure with ways and POIs.
 * This class can be used to edit and convert .map files to other formats.
 * 
 * @author Karsten
 * 
 */
public class MapFile {

	private static final String NL = "\r\n";

	private byte[] magicByte;
	private int headerSize;
	private int fileVersion;
	private byte flags;
	private byte amountOfZoomIntervals;
	private String projection;
	private int tileSize;

	// Bounding box
	private int maxLat;
	private int minLon;
	private int minLat;
	private int maxLon;

	// Map start position
	private int mapStartLon;
	private int mapStartLat;

	private long dateOfCreation;

	// POI tag mapping
	private int amountOfPOIMappings;
	private String[] poiMappings;

	// Ways tag mapping
	private int amountOfWayTagMappings;
	private String[] wayTagMappings;

	private String comment;

	// Zoom interval configuration
	private byte[] baseZoomLevel;
	private byte[] minimalZoomLevel;
	private byte[] maximalZoomLevel;
	private long[] absoluteStartPosition;
	private long[] subFileSize;
	private ArrayList<Long>[] tileOffsets;

	/**
	 * The constructor.
	 */
	public MapFile() {
		// Log.d("Map file has been initialized");
	}

	/**
	 * 
	 * @return a string representation of the file header.
	 */
	public String headerToString() {
		// TODO finish
		System.out.println("bla");
		StringBuilder sb = new StringBuilder();
		sb.append("------ H E A D E R ------").append(MapFile.NL);
		sb.append("Magic bytes: " + getHex(this.magicByte) + " (" + new String(this.magicByte) + ")")
				.append(MapFile.NL);
		sb.append("Header size: " + getHex(this.headerSize) + " (" + this.headerSize + ")").append(
				MapFile.NL);

		return sb.toString();
	}

	/**
	 * This method prepares the array that contains the mappings for tag IDs to tag names for POIs.
	 */
	public void preparePOIMappings() {
		this.poiMappings = new String[this.amountOfPOIMappings];
	}

	/**
	 * This method prepares the array that contains the mappings for tag IDs to tag names for ways.
	 */
	public void prepareWayTagMappings() {
		this.wayTagMappings = new String[this.amountOfPOIMappings];
	}

	/**
	 * This method prepares the arrays containing information about the base zoom level, minimal and
	 * maximal zoom level, the absolute start position and the subfile size.
	 */
	public void prepareZoomIntervalConfiguration() {
		this.baseZoomLevel = new byte[this.amountOfZoomIntervals];
		this.minimalZoomLevel = new byte[this.amountOfZoomIntervals];
		this.maximalZoomLevel = new byte[this.amountOfZoomIntervals];
		this.absoluteStartPosition = new long[this.amountOfZoomIntervals];
		this.subFileSize = new long[this.amountOfZoomIntervals];
	}

	public byte[] getMagicByte() {
		return magicByte;
	}

	public void setMagicByte(byte[] magicByte) {
		this.magicByte = magicByte;
	}

	public int getHeaderSize() {
		return headerSize;
	}

	public void setHeaderSize(int headerSize) {
		this.headerSize = headerSize;
	}

	public int getFileVersion() {
		return fileVersion;
	}

	public void setFileVersion(int fileVersion) {
		this.fileVersion = fileVersion;
	}

	public byte getFlags() {
		return flags;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

	public byte getAmountOfZoomIntervals() {
		return amountOfZoomIntervals;
	}

	public void setAmountOfZoomIntervals(byte amountOfZoomIntervals) {
		this.amountOfZoomIntervals = amountOfZoomIntervals;
	}

	public String getProjection() {
		return projection;
	}

	public void setProjection(String projection) {
		this.projection = projection;
	}

	public int getTileSize() {
		return tileSize;
	}

	public void setTileSize(int tileSize) {
		this.tileSize = tileSize;
	}

	public void setBoundingBox(int maxLat, int minLon, int minLat, int maxLon) {
		this.maxLat = maxLat;
		this.minLon = minLon;
		this.minLat = minLat;
		this.maxLon = maxLon;
	}

	public void setMapStartPosition(int lat, int lon) {
		this.mapStartLat = lat;
		this.mapStartLon = lon;
	}

	public long getDateOfCreation() {
		return dateOfCreation;
	}

	public void setDateOfCreation(long dateOfCreation) {
		this.dateOfCreation = dateOfCreation;
	}

	/**
	 * @return the amountOfPOIMappings
	 */
	public int getAmountOfPOIMappings() {
		return amountOfPOIMappings;
	}

	/**
	 * @param amountOfPOIMappings
	 *            the amountOfPOIMappings to set
	 */
	public void setAmountOfPOIMappings(int amountOfPOIMappings) {
		this.amountOfPOIMappings = amountOfPOIMappings;
	}

	/**
	 * @return the pOIMappings
	 */
	public String[] getPOIMappings() {
		return poiMappings;
	}

	/**
	 * @param poiMappings
	 *            the pOIMappings to set
	 */
	public void setPOIMappings(String[] poiMappings) {
		this.poiMappings = poiMappings;
	}

	/**
	 * @return the amountOfWayTagMappings
	 */
	public int getAmountOfWayTagMappings() {
		return amountOfWayTagMappings;
	}

	/**
	 * @param amountOfWayTagMappings
	 *            the amountOfWayTagMappings to set
	 */
	public void setAmountOfWayTagMappings(int amountOfWayTagMappings) {
		this.amountOfWayTagMappings = amountOfWayTagMappings;
	}

	/**
	 * @return the wayTagMappings
	 */
	public String[] getWayTagMappings() {
		return wayTagMappings;
	}

	/**
	 * @param wayTagMappings
	 *            the wayTagMappings to set
	 */
	public void setWayTagMappings(String[] wayTagMappings) {
		this.wayTagMappings = wayTagMappings;
	}

	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param comment
	 *            the comment to set
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setZoomIntervalConfiguration(int zoomInterval, byte baseZoomLevel,
			byte minimalZoomLevel, byte maximalZoomLevel, long absoluteStartPosition, long subFileSize) {
		this.baseZoomLevel[zoomInterval] = baseZoomLevel;
		this.minimalZoomLevel[zoomInterval] = minimalZoomLevel;
		this.maximalZoomLevel[zoomInterval] = maximalZoomLevel;
		this.absoluteStartPosition[zoomInterval] = absoluteStartPosition;
		this.subFileSize[zoomInterval] = subFileSize;

	}

}
