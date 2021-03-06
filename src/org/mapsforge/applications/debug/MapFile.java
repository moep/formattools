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

import static org.mapsforge.applications.debug.MapFormatReader.getHex;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is a representation of a map file. It contains its basic structure with ways and POIs.
 * This class can be used to edit and convert .map files to other formats.
 * 
 * @author Karsten
 * 
 */
class MapFile {

	static final String NL = "\r\n";

	private byte[] magicByte;
	private int headerSize;
	private int fileVersion;
	private byte flags;
	private byte amountOfZoomIntervals;
	private String projection;
	private int tileSize;
	private long fileSize;
	private String languagePreference;

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

	// Subfiles
	private List<SubFile> subFiles;

	/**
	 * The constructor.
	 */
	public MapFile() {
		// Log.d("Map file has been initialized");
		this.subFiles = new LinkedList<SubFile>();
	}

	@Override
	public String toString() {
		return this.headerToString();
	}

	/**
	 * 
	 * @return a string representation of the file header.
	 */
	public String headerToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("------ H E A D E R ------").append(MapFile.NL);
		sb.append("Magic bytes: " + getHex(this.magicByte) + " (" + new String(this.magicByte) + ")")
				.append(MapFile.NL);
		sb.append("Header size: " + getHex(this.headerSize) + " (" + this.headerSize + ")").append(
				MapFile.NL);
		sb.append("File version: " + getHex(this.fileVersion) + " (" + this.fileVersion + ")").append(
				MapFile.NL);
		sb.append("Flags: " + getHex(this.flags)).append(MapFile.NL);
		sb.append(
				"Amount of zoom intervals: " + getHex(this.amountOfZoomIntervals) + " ("
						+ this.amountOfZoomIntervals + ")").append(MapFile.NL);
		sb.append("Projection: " + this.projection).append(MapFile.NL);
		sb.append("Tile size: " + getHex(this.tileSize) + " (" + this.tileSize + ")")
				.append(MapFile.NL);
		sb.append("Bounding box: ").append(MapFile.NL);
		sb.append("  Max lat: " + getHex(this.maxLat) + " (" + this.maxLat + ")").append(MapFile.NL);
		sb.append("  Min lon: " + getHex(this.minLon) + " (" + this.minLon + ")").append(MapFile.NL);
		sb.append("  Min Lat: " + getHex(this.minLat) + " (" + this.minLat + ")").append(MapFile.NL);
		sb.append("  Max Lon: " + getHex(this.maxLon) + " (" + this.maxLon + ")").append(MapFile.NL);

		if (isMapStartPositionFlagSet()) {
			sb.append("Map start position (lon/lat): " + this.mapStartLon + " " + this.mapStartLat)
					.append(
							MapFile.NL);
		}

		sb
				.append(
						"Date of creation: " + getHex(this.dateOfCreation) + " (" + this.dateOfCreation
								+ ")").append(MapFile.NL);
		sb.append(
				"Amount of POI mappings: " + getHex(this.amountOfPOIMappings) + " ("
						+ this.amountOfPOIMappings + ")").append(MapFile.NL);
		for (int id = 0; id < this.amountOfPOIMappings; ++id) {
			sb.append("  " + id + " => " + this.poiMappings[id]).append(MapFile.NL);
		}

		sb.append(
				"Amount of way mappings: " + getHex(this.amountOfWayTagMappings) + " ("
						+ this.amountOfWayTagMappings + ")").append(MapFile.NL);
		for (int id = 0; id < this.amountOfWayTagMappings; ++id) {
			sb.append("  " + id + " => " + this.wayTagMappings[id]).append(MapFile.NL);
		}

		sb.append("Comment: " + this.comment).append(MapFile.NL);
		sb.append("Zoom interval configuration:").append(MapFile.NL);
		sb
				.append(
						"  Zoom interval\tBase zoom level\tMinimal zoom level\tMaximal zoom level\tAbsolute start position\tSubfile size")
				.append(MapFile.NL);
		for (int interval = 0; interval < this.amountOfZoomIntervals; ++interval) {
			sb.append(
					"  " + interval + "\t\t" + this.baseZoomLevel[interval] + "\t\t"
							+ this.minimalZoomLevel[interval] + "\t\t\t"
							+ this.maximalZoomLevel[interval]
							+ "\t\t\t" + this.absoluteStartPosition[interval] + "\t\t\t"
							+ this.subFileSize[interval]).append(MapFile.NL);
		}

		return sb.toString();
	}

	/**
	 * @return true if the map is in debug mode.
	 */
	boolean isDebugFlagSet() {
		return (this.flags & 0x80) != 0;
	}

	/**
	 * @return true if the map has map start position data.
	 */
	boolean isMapStartPositionFlagSet() {
		return (this.flags & 0x40) != 0;
	}

	/**
	 * @return true if the map has a start zoom level.
	 */
	boolean isZoomLevelFlagSet() {
		return (this.flags & 0x20) != 0;
	}

	/**
	 * @return true if the map has a language preference.
	 */
	boolean isLanguagePreferenceFlagSet() {
		return (this.flags & 0x10) != 0;
	}

	/**
	 * @return true if the map has an attached comment.
	 */
	boolean isCommentFlagSet() {
		return (this.flags & 0x08) != 0;
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
		this.wayTagMappings = new String[this.amountOfWayTagMappings];
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

	/**
	 * Adds a subfile for a base zoom interval.
	 * 
	 * @param sf
	 *            The subfile (@link {@link SubFile})
	 */
	public void addSubFile(SubFile sf) {
		this.subFiles.add(sf);
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

	public void setBoundingBox(int minLat, int minLon, int maxLat, int maxLon) {
		this.minLat = minLat;
		this.minLon = minLon;
		this.maxLat = maxLat;
		this.maxLon = maxLon;
	}

	public int getMapStartLon() {
		return mapStartLon;
	}

	public int getMapStartLat() {
		return mapStartLat;
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

		// System.out.println("setZoomIntervalConfiguration(" + zoomInterval + "," +
		// this.baseZoomLevel[zoomInterval]
		// + "," + this.minimalZoomLevel[zoomInterval] + "," + this.maximalZoomLevel[zoomInterval] + ","
		// + this.absoluteStartPosition[zoomInterval]
		// + "," + this.subFileSize[zoomInterval] + ")");

	}

	public void setLanguagePreference(String languagePreference) {
		this.languagePreference = languagePreference;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public int getMaxLat() {
		return maxLat;
	}

	public int getMinLon() {
		return minLon;
	}

	public int getMinLat() {
		return minLat;
	}

	public int getMaxLon() {
		return maxLon;
	}

	public byte[] getBaseZoomLevel() {
		return baseZoomLevel;
	}

	public byte[] getMinimalZoomLevel() {
		return minimalZoomLevel;
	}

	public byte[] getMaximalZoomLevel() {
		return maximalZoomLevel;
	}

	public long[] getAbsoluteStartPosition() {
		return absoluteStartPosition;
	}

	long[] getSubFileSize() {
		return subFileSize;
	}

	/**
	 * @return the subFiles
	 */
	public List<SubFile> getSubFiles() {
		return subFiles;
	}

	public long getFileSize() {
		return this.fileSize;
	}

	public String getLanguagePreference() {
		return this.languagePreference;
	}

}
