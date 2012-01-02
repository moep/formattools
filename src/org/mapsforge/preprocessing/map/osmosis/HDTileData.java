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
package org.mapsforge.preprocessing.map.osmosis;

import gnu.trove.list.array.TLongArrayList;

import java.util.List;
import java.util.Map;

class HDTileData extends TileData {
	private final TLongArrayList pois;
	private final TLongArrayList ways;

	HDTileData() {
		pois = new TLongArrayList();
		ways = new TLongArrayList();
	}

	TLongArrayList getPois() {
		return pois;
	}

	TLongArrayList getWays() {
		return ways;
	}

	@Override
	void addPOI(TDNode poi) {
		pois.add(poi.getId());
	}

	@Override
	void addWay(TDWay way) {
		ways.add(way.getId());
	}

	@Override
	Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		throw new UnsupportedOperationException(HDTileData.class.getName()
				+ "does not support this operation");
	}

	@Override
	Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		throw new UnsupportedOperationException(HDTileData.class.getName()
				+ "does not support this operation");
	}

}