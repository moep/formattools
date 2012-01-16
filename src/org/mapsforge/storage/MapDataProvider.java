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
import org.mapsforge.core.Rect;
import org.mapsforge.storage.atoms.Way;
import org.mapsforge.storage.atoms.WaySegment;
import org.mapsforge.storage.poi.PointOfInterest;

/**
 * This interface provides methods for searching an retrieving map data atoms such as streets, POIs etc.
 * It abstracts from an underlying storage architecture. This means that this interface can be used for
 * retrieving data from the mapsforge map format or from an arbitrary database.
 * 
 * @author Karsten Groll
 * 
 */

// TODO finish
public interface MapDataProvider {
	/**
	 * Gets all Way within the given bounding box.
	 * 
	 * @param boundingBox
	 *            The bounding box.
	 * 
	 * @return All ways within the given bounding box.
	 */
	public Collection<Way> getAllWaysInBoundingBox(final Rect boundingBox);

	/**
	 * Gets all POIs within a given bounding having a certain tag ID.
	 * 
	 * @param boundingBox
	 *            The bounding box.
	 * @param allowedTagIDs
	 *            A list of tag IDs. POIs having a tag ID from this list will be added to the return
	 *            set. If set to null, nodes will not be filtered.
	 * @return All POIs with a specified tag ID within a given bounding box.
	 */
	public Collection<PointOfInterest> getAllPoisInBoundingBox(final Rect boundingBox, int[] allowedTagIDs);

	/**
	 * Gets all absolute coordinates in order (lat_1, lon_1), ..., (lat_n, lon_n) for a given way
	 * segment, specified by its id. THe input is a way edge specified by its first coordinate p1 and
	 * its last coordinate p2.
	 * 
	 * @param wayID
	 *            The way ID of the way, the edge belongs to.
	 * @param p1
	 *            The edge's first absolute coordinate.
	 * @param p2
	 *            The edge's last coordinate.
	 * @param baseZoomInterval
	 *            The base zoom interval the edge should be searched on.
	 * @return All absolute coordinates in order (lat_1, lon_1), ..., (lat_n, lon_n) for a given way
	 *         segment.
	 */
	public WaySegment getWayDataForEdge(final long wayID, final GeoCoordinate p1, final GeoCoordinate p2, byte baseZoomInterval);

	Collection<Way> getAllWays(int tileX, int tileY, byte baseZoomInterval);

}
