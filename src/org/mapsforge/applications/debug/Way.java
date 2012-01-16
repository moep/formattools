/*
 * Copyright 2010, 2011 mapsforge.org
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.applications.debug;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Container class for ways.
 * 
 * @author Karsten Groll
 * 
 */
class Way {
	private String waySignature;
	private long id;
	private int waySize;
	private byte[] subTileBitmap;
	private byte specialByte;
	private List<Integer> tagID;

	private byte flags;
	private String name;
	private String reference;

	// Label position
	private int labelPositionLatDiff;
	private int labelPositionLonDiff;

	// Number of way data blocks
	private byte numberOfWayDataBlocks;

	// Way data blocks
	private List<WayData> wayData;

	/**
	 * The constructor.
	 */
	Way() {
		this.subTileBitmap = new byte[2];
		this.tagID = new LinkedList<Integer>();
		this.wayData = new ArrayList<WayData>();
	}

	@Override
	public String toString() {
		// TODO finish
		// StringBuilder sb = new StringBuilder();
		// sb.append("--- W A Y ---").append(MapFile.NL);
		// sb.append("Way signature: ").append(this.waySignature).append(MapFile.NL);
		// sb.append("Way size: ").append(this.waySize).append(MapFile.NL);
		// sb.append("Sub tile bitmap ").append(MapFormatReader.getHex(this.subTileBitmap[0])).append(" ")
		// .append(MapFormatReader.getHex(this.subTileBitmap[1])).append(MapFile.NL);
		// sb.append("Special byte 1: ").append(MapFormatReader.getHex(this.specialByte))
		// .append(MapFile.NL);
		// return sb.toString();
		return "TODO";
	}

	int getLayer() {
		return this.specialByte & 0xf0 + 5;
	}

	int getAmountOfTags() {
		return this.specialByte & 0x0f;
	}

	boolean isWayFlagSet() {
		return (this.flags & 0x80) != 0;
	}

	boolean isReferenceFlagSet() {
		return (this.flags & 0x40) != 0;
	}

	boolean isLabelPositionFlagSet() {
		return (this.flags & 0x20) != 0;
	}

	boolean isMultipolygonFlagSet() {
		return (this.flags & 0x10) != 0;
	}

	void addTagID(int id) {
		this.tagID.add(id);
	}

	// void addWayDataBlock(WayData wayDataBlock) {
	// this.wayData.add(wayDataBlock);
	// }

	WayData createAndAddWayDataBlock(byte numberOfWayCoordinateBlocks) {
		WayData ret = new WayData(numberOfWayCoordinateBlocks);
		this.wayData.add(ret);
		return ret;
	}

	/**
	 * @return the waySignature
	 */
	String getWaySignature() {
		return waySignature;
	}

	/**
	 * @param waySignature
	 *            the waySignature to set
	 */
	void setWaySignature(String waySignature) {
		this.waySignature = waySignature;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the waySize
	 */
	int getWaySize() {
		return waySize;
	}

	/**
	 * @param waySize
	 *            the waySize to set
	 */
	void setWaySize(int waySize) {
		this.waySize = waySize;
	}

	/**
	 * @return the subTileBitmap
	 */
	byte[] getSubTileBitmap() {
		return subTileBitmap;
	}

	/**
	 * 
	 * @param byte1
	 *            High byte
	 * @param byte2
	 *            Low byte
	 */
	void setSubTileBitmap(byte byte1, byte byte2) {
		this.subTileBitmap[0] = byte1;
		this.subTileBitmap[1] = byte2;
	}

	/**
	 * @return the specialByte1
	 */
	byte getSpecialByte() {
		return specialByte;
	}

	/**
	 * @param specialByte
	 *            the specialByte1 to set
	 */
	void setSpecialByte(byte specialByte) {
		this.specialByte = specialByte;
	}

	/**
	 * 
	 * @return Number of way data blocks.
	 */
	byte getNumberOfWayDataBlocks() {
		return numberOfWayDataBlocks;
	}

	/**
	 * 
	 * @param numberOfWayDataBlocks
	 *            Number of way data blocks to be set.
	 */
	void setNumberOfWayDataBlocks(byte numberOfWayDataBlocks) {
		this.numberOfWayDataBlocks = numberOfWayDataBlocks;
	}

	/**
	 * @return the tagID
	 */
	List<Integer> getTagID() {
		return tagID;
	}

	/**
	 * @param tagID
	 *            the tagID to set
	 */
	void setTagID(List<Integer> tagID) {
		this.tagID = tagID;
	}

	/**
	 * @return the flags
	 */
	byte getFlags() {
		return flags;
	}

	/**
	 * @param flags
	 *            the flags to set
	 */
	void setFlags(byte flags) {
		this.flags = flags;
	}

	/**
	 * @return the name
	 */
	String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the reference
	 */
	String getReference() {
		return reference;
	}

	/**
	 * @param reference
	 *            the reference to set
	 */
	void setReference(String reference) {
		this.reference = reference;
	}

	/**
	 * @return the labelPositionLatDiff
	 */
	int getLabelPositionLatDiff() {
		return labelPositionLatDiff;
	}

	/**
	 * @param labelPositionLatDiff
	 *            the labelPositionLatDiff to set
	 */
	void setLabelPositionLatDiff(int labelPositionLatDiff) {
		this.labelPositionLatDiff = labelPositionLatDiff;
	}

	/**
	 * @return the labelPositionLonDiff
	 */
	int getLabelPositionLonDiff() {
		return labelPositionLonDiff;
	}

	/**
	 * @param labelPositionLonDiff
	 *            the labelPositionLonDiff to set
	 */
	void setLabelPositionLonDiff(int labelPositionLonDiff) {
		this.labelPositionLonDiff = labelPositionLonDiff;
	}

	public List<WayData> getWayData() {
		return wayData;
	}

	class WayData {
		private byte numberOfWayCoordinateBlocks;

		// Way nodes per way coordinate block
		private int[] numberOfWayNodes;

		// Latitude and longitude differences for way j on way coordinate block i
		private int[][] latDiff;
		private int[][] lonDiff;

		private WayData(byte numberOfWayCoordinateBlocks) {
			this.numberOfWayCoordinateBlocks = numberOfWayCoordinateBlocks;

			this.numberOfWayNodes = new int[this.numberOfWayCoordinateBlocks];
			this.latDiff = new int[this.numberOfWayCoordinateBlocks][];
			this.lonDiff = new int[this.numberOfWayCoordinateBlocks][];
		}

		int[] getNumberOfWayNodes() {
			return numberOfWayNodes;
		}

		int[][] getLatDiff() {
			return this.latDiff;
		}

		int[][] getLonDiff() {
			return this.lonDiff;
		}
	}

}
