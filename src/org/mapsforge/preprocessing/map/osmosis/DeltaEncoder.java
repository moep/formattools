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
import java.util.Iterator;
import java.util.List;

class DeltaEncoder {

	static List<WayDataBlock> encode(List<WayDataBlock> blocks) {
		if (blocks == null)
			return null;
		List<WayDataBlock> results = new ArrayList<WayDataBlock>();

		for (WayDataBlock wayDataBlock : blocks) {
			List<Integer> outer = deltaEncode(wayDataBlock.getOuterWay());
			List<List<Integer>> inner = null;
			if (wayDataBlock.getInnerWays() != null) {
				inner = new ArrayList<List<Integer>>();
				for (List<Integer> list : wayDataBlock.getInnerWays()) {
					inner.add(deltaEncode(list));
				}
			}
			results.add(new WayDataBlock(outer, inner, Encoding.DELTA));
		}

		return results;
	}

	private static List<Integer> deltaEncode(List<Integer> list) {
		if (list == null)
			return null;
		ArrayList<Integer> result = new ArrayList<Integer>();

		if (list.isEmpty())
			return result;

		Iterator<Integer> it = list.iterator();
		// add the first way node to the result list
		Integer prevLat = it.next();
		Integer prevLon = it.next();

		result.add(prevLat);
		result.add(prevLon);

		while (it.hasNext()) {
			Integer currentLat = it.next();
			Integer currentLon = it.next();
			result.add(Integer.valueOf((currentLat.intValue() - prevLat.intValue())));
			result.add(Integer.valueOf(currentLon.intValue() - prevLon.intValue()));

			prevLat = currentLat;
			prevLon = currentLon;
		}

		return result;
	}

	public enum Encoding {
		NONE, DELTA, DOUBLE_DELTA
	}

}
