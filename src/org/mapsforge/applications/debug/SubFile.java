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
	private byte baseZoomInterval;

	// Tile index segment
	private String indexSignature;
	List<Long> indexEntry;

	List<Tile> tiles;

	/**
	 * The constructor.
	 * 
	 * @param parentFile
	 *            The parent file object that holds this subfile.
	 */
	SubFile(MapFile parentFile, byte baseZoomInterval) {
		System.out.println("Creating new subfile.");
		this.parentFile = parentFile;
		this.tiles = new LinkedList<Tile>();

		this.indexEntry = new ArrayList<Long>(this.parentFile.getMaximalZoomLevel()[baseZoomInterval]
						- this.parentFile.getMinimalZoomLevel()[baseZoomInterval] + 1);

		// this.indexEntry = new long[this.parentFile.getMaximalZoomLevel()[baseZoomInterval]
		// - this.parentFile.getMinimalZoomLevel()[baseZoomInterval] + 1];

		this.baseZoomInterval = baseZoomInterval;
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

	public void addIndexEntry(long data) {
		this.indexEntry.add(data);
	}

	public boolean isWaterTile(int index) {
		return (this.indexEntry.get(index) & 0x000000010000000000L) != 0;
	}

	public long getTileOffset(int index) {
		// TODO correct handling of water tiles
		return this.indexEntry.get(index);
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
}
