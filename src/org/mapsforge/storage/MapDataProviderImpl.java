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
import org.mapsforge.storage.tile.TilePersistanceManager;

public class MapDataProviderImpl implements MapDataProvider {

	private TilePersistanceManager tpm = null;

	public MapDataProviderImpl(TilePersistanceManager tpm) {
		this.tpm = tpm;
	}

	@Override
	public Collection<Way> getAllWaysInBoundingBox(GeoCoordinate p1, GeoCoordinate p2) {
		// TODO implement

		// Get all tiles needed for getting the data

		// For each tile: extract ways and ignore duplicates

		return null;
	}

}
