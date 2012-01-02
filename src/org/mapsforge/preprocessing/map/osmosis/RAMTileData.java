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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RAMTileData extends TileData {

	private Set<TDNode> pois;
	private Set<TDWay> ways;

	RAMTileData() {
		this.pois = new HashSet<TDNode>();
		this.ways = new HashSet<TDWay>();
	}

	@Override
	void addPOI(TDNode poi) {
		pois.add(poi);
	}

	@Override
	void addWay(TDWay way) {
		ways.add(way);
	}

	@Override
	Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel,
			byte maxValidZoomlevel) {
		HashMap<Byte, List<TDNode>> poisByZoomlevel = new HashMap<Byte, List<TDNode>>();
		for (TDNode poi : pois) {
			byte zoomlevel = poi.getZoomAppear();
			if (zoomlevel > maxValidZoomlevel)
				continue;
			if (zoomlevel < minValidZoomlevel)
				zoomlevel = minValidZoomlevel;
			List<TDNode> group = poisByZoomlevel.get(Byte.valueOf(zoomlevel));
			if (group == null)
				group = new ArrayList<TDNode>();
			group.add(poi);
			poisByZoomlevel.put(Byte.valueOf(zoomlevel), group);
		}

		return poisByZoomlevel;
	}

	@Override
	Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel) {
		HashMap<Byte, List<TDWay>> waysByZoomlevel = new HashMap<Byte, List<TDWay>>();
		for (TDWay way : ways) {
			byte zoomlevel = way.getMinimumZoomLevel();
			if (zoomlevel > maxValidZoomlevel)
				continue;
			if (zoomlevel < minValidZoomlevel)
				zoomlevel = minValidZoomlevel;
			List<TDWay> group = waysByZoomlevel.get(Byte.valueOf(zoomlevel));
			if (group == null)
				group = new ArrayList<TDWay>();
			group.add(way);
			waysByZoomlevel.put(Byte.valueOf(zoomlevel), group);
		}

		return waysByZoomlevel;
	}
}
