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
import java.util.LinkedList;
import java.util.List;


/**
 * This class is a container for so called sub files. These files contain all tiles for a certain base
 * zoom level.
 * 
 * @author Karsten
 * 
 */
class SubFile {
	/** The sub file's parent file. (The {@link MapFile}) */
	private MapFile parentFile;
	/** The base zoom interval of this tile. (For debugging purposes.) */
	private byte baseZoomInterval;

	// Tile index segment
	private String indexSignature;
	List<Long> indexEntry;
	// This subfiles' tiles
	List<Tile> tiles;
	private byte[][] rawTiles;
	private int numBlocks;
	private int startOffset;

	/**
	 * The constructor.
	 * 
	 * @param parentFile
	 *            The parent file object that holds this subfile.
	 * @param baseZoomInterval
	 *            The base zoom interval that is covered by this tile.
	 */
	SubFile(MapFile parentFile, byte baseZoomInterval) {
		this.parentFile = parentFile;
		this.tiles = new LinkedList<Tile>();

		this.indexEntry = new ArrayList<Long>(this.parentFile.getMaximalZoomLevel()[baseZoomInterval]
				- this.parentFile.getMinimalZoomLevel()[baseZoomInterval] + 1);

		// this.indexEntry = new long[this.parentFile.getMaximalZoomLevel()[baseZoomInterval]
		// - this.parentFile.getMinimalZoomLevel()[baseZoomInterval] + 1];

		this.baseZoomInterval = baseZoomInterval;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("--- S U B F I L E ").append(this.baseZoomInterval).append(" ---").append(MapFile.NL);
		sb.append("Tile ID\tIndex\tWater tile").append(MapFile.NL);

		int i = 0;
		for (Long ie : this.indexEntry) {
			sb.append(i).append("\t").append(ie).append("\t").append(isWaterTile(i))
					.append(MapFile.NL);
			++i;
		}

		sb.append("#Tiles: " + this.tiles.size()).append(MapFile.NL);

		return sb.toString();
	}

	public int getAmountOfTilesInIndex() {
		return this.indexEntry.size();
	}

	public boolean isEmptyTile(int tileID) {
		// Last tile? (Cannot be empty)
		if (tileID == indexEntry.size() - 1) {
			return indexEntry.get(tileID).equals(indexEntry.get(tileID - 1));
		}

		return indexEntry.get(tileID).equals(indexEntry.get(tileID + 1));
	}

	/**
	 * Returns the n-th tile in this subfile.
	 * 
	 * @param index
	 *            The tile's index (number)
	 * @return The n-th (index-th) tile in this subfile.
	 */
	public Tile getTile(int index) {
		return this.tiles.get(index);
	}

	public void addTile(Tile tile) {
		this.tiles.add(tile);
	}

	public void addRawTile(byte[] tileData, int index) {
		if (this.rawTiles == null) {
			this.rawTiles = new byte[this.numBlocks][];
		}

		this.rawTiles[index] = tileData;
	}

	public void addIndexEntry(long data) {
		this.indexEntry.add(data);
	}

	public boolean isWaterTile(int index) {
		return (this.indexEntry.get(index) & 0x1000000000L) != 0;
	}

	public long getTileOffset(int index) {
		if (index >= this.indexEntry.size()) {
			return this.indexEntry.get(indexEntry.size() - 1) & 0x7FFFFFFFFFL;
		}
		return this.indexEntry.get(index) & 0x7FFFFFFFFFL;
	}

	public byte getBaseZoomInterval() {
		return baseZoomInterval;
	}

	public void setBaseZoomInterval(byte baseZoomInterval) {
		this.baseZoomInterval = baseZoomInterval;
	}

	public String getIndexSignature() {
		return indexSignature;
	}

	public void setIndexSignature(String indexSignature) {
		this.indexSignature = indexSignature;
	}

	public void setNumberOfBlocks(int numBlocks) {
		this.numBlocks = numBlocks;

	}

	public int getNumberOfBlocks() {
		return this.numBlocks;

	}

	public void setSubfileStartOffset(int tileStartOffset) {
		this.startOffset = tileStartOffset;
	}

	public int getSubfileStartOffset() {
		return this.startOffset;
	}

}
