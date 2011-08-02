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

import java.util.LinkedList;
import java.util.List;

/**
 * Container class for ways.
 * 
 * @author Karsten
 * 
 */
public class Way {
	private String waySignature;
	private int waySize;
	private byte[] subTileBitmap;
	private byte specialByte1;
	private byte specialByte2;
	private byte wayTypeBitmap;
	private List<Integer> tagID;
	private int wayNodeAmount;

	// First way node
	private int firstWayNodeLatDiff;
	private int firstWayNodeLonDiff;

	// Way nodes
	private List<Integer> wayNodesLatDiff;
	private List<Integer> wayNodesLonDiff;

	private byte flags;
	private String name;
	private String reference;

	// Label position
	private int labelPositionLatDiff;
	private int labelPositionLonDiff;

	/**
	 * The constructor.
	 */
	public Way() {
		this.subTileBitmap = new byte[2];
		this.tagID = new LinkedList<Integer>();
		this.wayNodesLatDiff = new LinkedList<Integer>();
		this.wayNodesLonDiff = new LinkedList<Integer>();
	}

	@Override
	public String toString() {
		// TODO finish
		StringBuilder sb = new StringBuilder();
		sb.append("--- W A Y ---").append(MapFile.NL);
		sb.append("Way signature: ").append(this.waySignature).append(MapFile.NL);
		sb.append("Way size: ").append(this.waySize).append(MapFile.NL);
		sb.append("Sub tile bitmap ").append(MapFormatReader.getHex(this.subTileBitmap[0])).append(" ")
				.append(MapFormatReader.getHex(this.subTileBitmap[1])).append(MapFile.NL);
		sb.append("Special byte 1: ").append(MapFormatReader.getHex(this.specialByte1))
				.append(MapFile.NL);
		sb.append("Special byte 2: ").append(MapFormatReader.getHex(this.specialByte2))
				.append(MapFile.NL);
		return sb.toString();
	}

	int getLayer() {
		return this.specialByte1 & 0xf0 + 5;
	}

	int getAmountOfTags() {
		return this.specialByte1 & 0x0f;
	}

	int getAmountOfRelevantRenderingTags() {
		return this.specialByte2 & 0xe0;
	}

	boolean isHighwayTagFlagSet() {
		return (this.wayTypeBitmap & 0x80) != 0;
	}

	boolean isRailwayTagFlagSet() {
		return (this.wayTypeBitmap & 0x40) != 0;
	}

	boolean isBuildingTagFlagSet() {
		return (this.wayTypeBitmap & 0x20) != 0;
	}

	boolean isLanduseTagFlagSet() {
		return (this.wayTypeBitmap & 0x10) != 0;
	}

	boolean isLeisureTagFlagSet() {
		return (this.wayTypeBitmap & 0x08) != 0;
	}

	boolean isAmenityTagFlagSet() {
		return (this.wayTypeBitmap & 0x04) != 0;
	}

	boolean isNaturalTagFlagSet() {
		return (this.wayTypeBitmap & 0x02) != 0;
	}

	boolean isWaterwayTagFlagSet() {
		return (this.wayTypeBitmap & 0x01) != 0;
	}

	void addTagID(int tag) {
		this.tagID.add(tag);
	}

	void addWayNodesLatDiff(int latDiff) {
		this.wayNodesLatDiff.add(latDiff);
	}

	void addWayNodesLonDiff(int lonDiff) {
		this.wayNodesLonDiff.add(lonDiff);
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

	/**
	 * @return the waySignature
	 */
	public String getWaySignature() {
		return waySignature;
	}

	/**
	 * @param waySignature
	 *            the waySignature to set
	 */
	public void setWaySignature(String waySignature) {
		this.waySignature = waySignature;
	}

	/**
	 * @return the waySize
	 */
	public int getWaySize() {
		return waySize;
	}

	/**
	 * @param waySize
	 *            the waySize to set
	 */
	public void setWaySize(int waySize) {
		this.waySize = waySize;
	}

	/**
	 * @return the subTileBitmap
	 */
	public byte[] getSubTileBitmap() {
		return subTileBitmap;
	}

	/**
	 * 
	 * @param byte1
	 *            High byte
	 * @param byte2
	 *            Low byte
	 */
	public void setSubTileBitmap(byte byte1, byte byte2) {
		this.subTileBitmap[0] = byte1;
		this.subTileBitmap[1] = byte2;
	}

	/**
	 * @return the specialByte1
	 */
	public byte getSpecialByte1() {
		return specialByte1;
	}

	/**
	 * @param specialByte1
	 *            the specialByte1 to set
	 */
	public void setSpecialByte1(byte specialByte1) {
		this.specialByte1 = specialByte1;
	}

	/**
	 * @return the specialByte2
	 */
	public byte getSpecialByte2() {
		return specialByte2;
	}

	/**
	 * @param specialByte2
	 *            the specialByte2 to set
	 */
	public void setSpecialByte2(byte specialByte2) {
		this.specialByte2 = specialByte2;
	}

	/**
	 * @return the wayTagBitmap
	 */
	public byte getWayTypeBitmap() {
		return wayTypeBitmap;
	}

	/**
	 * @param wayTagBitmap
	 *            the wayTagBitmap to set
	 */
	public void setWayTypeBitmap(byte wayTagBitmap) {
		this.wayTypeBitmap = wayTagBitmap;
	}

	/**
	 * @return the tagID
	 */
	public List<Integer> getTagID() {
		return tagID;
	}

	/**
	 * @param tagID
	 *            the tagID to set
	 */
	public void setTagID(List<Integer> tagID) {
		this.tagID = tagID;
	}

	/**
	 * @return the wayNodeAmount
	 */
	public int getWayNodeAmount() {
		return wayNodeAmount;
	}

	/**
	 * @param wayNodeAmount
	 *            the wayNodeAmount to set
	 */
	public void setWayNodeAmount(int wayNodeAmount) {
		this.wayNodeAmount = wayNodeAmount;
	}

	/**
	 * @return the firstWayNodeLatDiff
	 */
	public int getFirstWayNodeLatDiff() {
		return firstWayNodeLatDiff;
	}

	/**
	 * @param firstWayNodeLatDiff
	 *            the firstWayNodeLatDiff to set
	 */
	public void setFirstWayNodeLatDiff(int firstWayNodeLatDiff) {
		this.firstWayNodeLatDiff = firstWayNodeLatDiff;
	}

	/**
	 * @return the firstWayNodeLonDiff
	 */
	public int getFirstWayNodeLonDiff() {
		return firstWayNodeLonDiff;
	}

	/**
	 * @param firstWayNodeLonDiff
	 *            the firstWayNodeLonDiff to set
	 */
	public void setFirstWayNodeLonDiff(int firstWayNodeLonDiff) {
		this.firstWayNodeLonDiff = firstWayNodeLonDiff;
	}

	/**
	 * @return the wayNodesLatDiff
	 */
	public List<Integer> getWayNodesLatDiff() {
		return wayNodesLatDiff;
	}

	/**
	 * @param wayNodesLatDiff
	 *            the wayNodesLatDiff to set
	 */
	public void setWayNodesLatDiff(List<Integer> wayNodesLatDiff) {
		this.wayNodesLatDiff = wayNodesLatDiff;
	}

	/**
	 * @return the wayNodesLonDiff
	 */
	public List<Integer> getWayNodesLonDiff() {
		return wayNodesLonDiff;
	}

	/**
	 * @param wayNodesLonDiff
	 *            the wayNodesLonDiff to set
	 */
	public void setWayNodesLonDiff(List<Integer> wayNodesLonDiff) {
		this.wayNodesLonDiff = wayNodesLonDiff;
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
	 * @return the reference
	 */
	public String getReference() {
		return reference;
	}

	/**
	 * @param reference
	 *            the reference to set
	 */
	public void setReference(String reference) {
		this.reference = reference;
	}

	/**
	 * @return the labelPositionLatDiff
	 */
	public int getLabelPositionLatDiff() {
		return labelPositionLatDiff;
	}

	/**
	 * @param labelPositionLatDiff
	 *            the labelPositionLatDiff to set
	 */
	public void setLabelPositionLatDiff(int labelPositionLatDiff) {
		this.labelPositionLatDiff = labelPositionLatDiff;
	}

	/**
	 * @return the labelPositionLonDiff
	 */
	public int getLabelPositionLonDiff() {
		return labelPositionLonDiff;
	}

	/**
	 * @param labelPositionLonDiff
	 *            the labelPositionLonDiff to set
	 */
	public void setLabelPositionLonDiff(int labelPositionLonDiff) {
		this.labelPositionLonDiff = labelPositionLonDiff;
	}

	// Multipolygon
	// TODO add data for multipolygons

}
