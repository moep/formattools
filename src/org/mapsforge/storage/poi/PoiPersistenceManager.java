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
package org.mapsforge.storage.poi;

import java.util.Collection;

import org.mapsforge.core.GeoCoordinate;

/**
 * Abstracts from an underlying Storage/DB by providing methods for inserting/deleting/searching
 * {@link PointOfInterest} objects in named Storage/DB.
 * 
 * Remember to call the {@link #close()} method as soon as your done manipulating the Storage/DB via
 * this {@link PoiPersistenceManager}.
 * 
 * @author weise
 * @author Karsten Groll
 * 
 */
public interface PoiPersistenceManager {

	/**
	 * Inserts a single {@link PointOfInterest} into storage.
	 * 
	 * @param poi
	 *            {@link PointOfInterest} to insert into storage.
	 */
	public void insertPointOfInterest(PointOfInterest poi);

	/**
	 * Inserts {@link PointOfInterest} into this {@link PoiPersistenceManager}.
	 * 
	 * @param pois
	 *            collection of {@link PointOfInterest} to insert into storage.
	 */
	public void insertPointsOfInterest(Collection<PointOfInterest> pois);

	/**
	 * Removes a point of interest from this {@link PoiPersistenceManager}.
	 * 
	 * @param poi
	 *            the {@link PointOfInterest} to be removed.
	 */
	public void removePointOfInterest(PointOfInterest poi);

	/**
	 * @param poiID
	 *            the id of the point of interest that shall be returned.
	 * @return a single {@link PointOfInterest} p where p.id == poiID.
	 */
	public PointOfInterest findPointByID(long poiID);

	/**
	 * Fetch {@link PointOfInterest} from underlying storage near a given position.
	 * 
	 * @param point
	 *            {@link GeoCoordinate} center of the search.
	 * @param distance
	 *            in meters
	 * @param categoryName
	 *            unique title of {@link PoiCategory} the returned {@link PointOfInterest} should belong
	 *            to.
	 * @param limit
	 *            max number of {@link PointOfInterest} to be returned.
	 * @return {@link Collection} of {@link PointOfInterest} of the given {@link PoiCategory} near the
	 *         given position.
	 */
	public Collection<PointOfInterest> findNearPosition(GeoCoordinate point,
			int distance, String categoryName, int limit);

	/**
	 * Find all {@link PointOfInterest} of the given {@link PoiCategory} in a rectangle specified by the
	 * two given {@link GeoCoordinate}s.
	 * 
	 * @param p1
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (minLat, minLon)
	 * @param p2
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (maxLat, maxLon)
	 * @param categoryName
	 *            unique title of {@link PoiCategory} the returned {@link PointOfInterest} should belong
	 *            to.
	 * @param limit
	 *            max number of {@link PointOfInterest} to be returned.
	 * @return {@link Collection} of {@link PointOfInterest} of the given {@link PoiCategory} contained
	 *         in the rectangle specified by the two given {@link GeoCoordinate}s.
	 */
	public Collection<PointOfInterest> findInRect(GeoCoordinate p1,
			GeoCoordinate p2, String categoryName, int limit);

	/**
	 * Find all {@link PointOfInterest} of the given {@link PoiCategory} in a rectangle specified by the
	 * two given {@link GeoCoordinate}s. The only POIs that are allowed by the {@link PoiCategoryFilter}
	 * object will be returned.
	 * 
	 * @param p1
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (minLat, minLon)
	 * @param p2
	 *            {@link GeoCoordinate} specifying one corner of the rectangle. (maxLat, maxLon)
	 * @param filter
	 *            POI category filter object that helps determining whether a POI should be added to the
	 *            set or not.
	 * @param limit
	 *            max number of {@link PointOfInterest} to be returned.
	 * @return {@link Collection} of {@link PointOfInterest} of the given {@link PoiCategory} contained
	 *         in the rectangle specified by the two given {@link GeoCoordinate}s.
	 */
	public Collection<PointOfInterest> findInRectWithFilter(GeoCoordinate p1,
			GeoCoordinate p2, PoiCategoryFilter filter, int limit);

	/**
	 * Sets this manager's {@link PoiCategoryManager} for retrieving and editing POI categories.
	 * 
	 * @param categoryManager
	 *            The category manager to be set.
	 */
	public void setCategoryManager(PoiCategoryManager categoryManager);

	/**
	 * 
	 * @return The persistence managers category manager for retrieving and editing POI categories.
	 */
	public PoiCategoryManager getCategoryManager();

	/**
	 * Use this to free claimed resources. After that you might no longer be able to query for points of
	 * interest with this instance of {@link IPoiQuery}. This should always be a called a soon as you
	 * are done querying.
	 */
	public void close();

}
