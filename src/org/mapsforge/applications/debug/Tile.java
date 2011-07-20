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

import java.util.Hashtable;

/**
 * Container class for tile data. (No image data.)
 * 
 * @author Karsten
 * 
 */
class Tile {
	/** Redundant information for ... */

	// Tile header
	String tileSignature;

	// Zoom table
	Hashtable<Integer, Integer> numPOIs;
	Hashtable<Integer, Integer> numWays;

	int firstWayOffset;

	public Tile() {
		this.numPOIs = new Hashtable<Integer, Integer>();
		this.numWays = new Hashtable<Integer, Integer>();
	}

	public void addZoomTableRow(int row, int numPois, int numWays) {
		this.numPOIs.put(row, numPois);
		this.numWays.put(row, numWays);
	}

	public int getFirstWayOffset() {
		return firstWayOffset;
	}

	public void setFirstWayOffset(int firstWayOffset) {
		this.firstWayOffset = firstWayOffset;
	}

}
