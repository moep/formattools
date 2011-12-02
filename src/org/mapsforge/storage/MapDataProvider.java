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

import org.mapsforge.core.Rect;
import org.mapsforge.storage.atoms.Way;

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

}
