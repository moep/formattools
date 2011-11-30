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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Container class for tile data. (No image data.)
 * 
 * @author Karsten Groll
 * 
 */
class Tile {
	/** The @link {@link MapFile} object that contains this tile. (For debugging purposes.) */
	private MapFile parentMapFile;
	/** The @link {@link SubFile} object that contains this tile. (For debugging purposes.) */
	private SubFile parentSubFile;

	// Tile header
	String tileSignature;

	// Zoom table
	Hashtable<Integer, Integer> numPOIs;
	Hashtable<Integer, Integer> numWays;

	int firstWayOffset;

	// POIs and ways
	private List<POI> pois;
	private List<Way> ways;

	public List<POI> getPois() {
		return pois;
	}

	public List<Way> getWays() {
		return ways;
	}

	/**
	 * The constructor.
	 */
	public Tile() {
		this.numPOIs = new Hashtable<Integer, Integer>();
		this.numWays = new Hashtable<Integer, Integer>();

		// this.parentMapFile = parentMapFile;
		// this.parentSubFile = parentSubFile;

		this.pois = new LinkedList<POI>();
		this.ways = new LinkedList<Way>();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("-- T I L E --").append(MapFile.NL);

		if (this.parentMapFile.isDebugFlagSet()) {
			sb.append("Tile signature: ").append(this.tileSignature).append(MapFile.NL);
		}
		sb.append("Zoom level\t#POIs\tWays").append(MapFile.NL);
		int minZoomLevel = this.parentMapFile.getMinimalZoomLevel()[this.parentSubFile
				.getBaseZoomInterval()];
		int numZoomLevels = this.parentMapFile.getMaximalZoomLevel()[this.parentSubFile
				.getBaseZoomInterval()]
				- this.parentMapFile.getMinimalZoomLevel()[this.parentSubFile.getBaseZoomInterval()];

		for (int zoomLevel = minZoomLevel; zoomLevel < minZoomLevel + numZoomLevels; ++zoomLevel) {
			sb.append(zoomLevel).append("\t").append(numPOIs.get(zoomLevel)).append("\t")
					.append(numWays.get(zoomLevel)).append(MapFile.NL);
		}

		sb.append("First way offset: ").append(this.firstWayOffset).append(MapFile.NL);

		return sb.toString();
	}

	public int getCumulatedNumberOfPoisOnZoomlevel(int zoomLevel) {
		return this.numPOIs.get(zoomLevel);
	}

	public int getCumulatedNumberOfWaysOnZoomLevel(int zoomLevel) {
		return this.numWays.get(zoomLevel);
	}

	public void addZoomTableRow(int row, int numPois, int numWays1) {
		this.numPOIs.put(row, numPois);
		this.numWays.put(row, numWays1);
	}

	/**
	 * Adds a POI to this tile.
	 * 
	 * @param poi
	 *            the POI that should be added.
	 */
	public void addPOI(POI poi) {
		this.pois.add(poi);
	}

	/**
	 * Adds a way to this tile.
	 * 
	 * @param way
	 *            the way that should be added.
	 */
	public void addWay(Way way) {
		this.ways.add(way);
	}

	public int getFirstWayOffset() {
		return firstWayOffset;
	}

	public void setFirstWayOffset(int firstWayOffset) {
		this.firstWayOffset = firstWayOffset;
	}

	/**
	 * @return the tileSignature
	 */
	public String getTileSignature() {
		return tileSignature;
	}

	/**
	 * @param tileSignature
	 *            the tileSignature to set
	 */
	public void setTileSignature(String tileSignature) {
		this.tileSignature = tileSignature;
	}

}
