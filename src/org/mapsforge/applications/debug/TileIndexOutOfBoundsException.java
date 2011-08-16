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

/**
 * This exception is raised whenever a tile is requested that does not lie within a map's bounding box
 * or zoom intervals.
 * 
 * @author Karsten Groll
 * 
 */
public class TileIndexOutOfBoundsException extends Exception {
	/**
	 * Automatically generated SUID.
	 */
	private static final long serialVersionUID = 4963406980294684666L;

	/**
	 * Default exception call.
	 * 
	 * @param x
	 *            The requested x-coordinate.
	 * @param y
	 *            The requested y-coordinate.
	 * @param zoomInterval
	 *            The requested zoom interval.
	 */
	public TileIndexOutOfBoundsException(int x, int y, byte zoomInterval) {
		super("Tile coordinates (" + x + ", " + y + ") at zoom interval " + zoomInterval
				+ " are out of the sub file's bounding box.");
	}
}
