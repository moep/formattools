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
package org.mapsforge.storage;

import java.util.Collection;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.storage.atoms.Way;

/**
 * This interface provides methods for searching an retrieving map data atoms such as streets, POIs etc.
 * 
 * @author Karsten Groll
 * 
 */

// TODO finish
public interface MapDataProvider {
	/**
	 * Gets all Way within the given bounding box.
	 * 
	 * @param p1
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (minLat, minLon)
	 * 
	 * @param p2
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (maxLat, maxLon)
	 * 
	 * @return All ways within the given bounding box.
	 */
	public Collection<Way> getAllWaysInBoundingBox(GeoCoordinate p1, GeoCoordinate p2);
}
