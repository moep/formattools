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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;

//TODO could be implemented more efficiently with graphs: each line string is an edge, use an undirected graph and search for strongly connected components

class WayPolygonizer {

	private static final int MIN_NODES_POLYGON = 4;
	private final GeometryFactory geometryFactory = new GeometryFactory();

	private List<Deque<TDWay>> polygons;
	private List<TDWay> dangling;
	private List<TDWay> illegal;
	private HashMap<Integer, List<Integer>> outerToInner;

	/**
	 * Tries to merge ways to closed polygons. The ordering of waynodes is preserved during the merge process.
	 * 
	 * @param ways
	 *            An array of ways that should be merged. Ways may be given in any order and may already be
	 *            closed.
	 */
	void mergePolygons(TDWay[] ways) {
		polygons = new ArrayList<Deque<TDWay>>();
		dangling = new ArrayList<RAMTileData.TDWay>();
		illegal = new ArrayList<RAMTileData.TDWay>();

		Deque<TDWay> ungroupedWays = new ArrayDeque<TDWay>();

		// initially all ways are ungrouped
		for (TDWay tdWay : ways) {
			// reset reversed flag, may already be set when way is part of another relation
			tdWay.setReversedInRelation(false);

			// first extract all way that are closed polygons in their own right
			if (isClosedPolygon(tdWay)) {
				if (tdWay.getWayNodes().length < MIN_NODES_POLYGON)
					illegal.add(tdWay);
				else {
					Deque<TDWay> cluster = new ArrayDeque<RAMTileData.TDWay>();
					cluster.add(tdWay);
					polygons.add(cluster);
				}
			} else
				ungroupedWays.add(tdWay);
		}

		// all ways have been polygons, nice!
		if (ungroupedWays.isEmpty())
			return;

		if (ungroupedWays.size() == 1) {
			dangling.add(ungroupedWays.getFirst());
			return;
		}

		boolean startNewPolygon = true;

		while (true) {
			boolean merge = false;
			if (startNewPolygon) {
				// we start a new polygon either during first iteration or when
				// the previous iterations merged ways to a closed polygon and there
				// are still ungrouped ways left
				Deque<TDWay> cluster = new ArrayDeque<RAMTileData.TDWay>();
				// get the first way of the yet ungrouped ways and form a new group
				cluster.add(ungroupedWays.removeFirst());
				polygons.add(cluster);
				startNewPolygon = false;
			}

			// test if we can merge the current polygon with an ungrouped way
			Iterator<TDWay> it = ungroupedWays.iterator();
			while (it.hasNext()) {
				TDWay current = it.next();

				Deque<TDWay> currentPolygonSegments = polygons.get(polygons.size() - 1);
				// first way in current polygon
				TDWay c1Start = currentPolygonSegments.getFirst();
				// last way in current polygon
				TDWay c1End = currentPolygonSegments.getLast();

				long startFirst = c1Start.isReversedInRelation() ? c1Start.getWayNodes()[c1Start.getWayNodes().length - 1]
						.getId() : c1Start.getWayNodes()[0].getId();

				long endLast = c1End.isReversedInRelation() ? c1End.getWayNodes()[0].getId() : c1End
						.getWayNodes()[c1End.getWayNodes().length - 1].getId();

				long currentFirst = current.getWayNodes()[0].getId();
				long currentLast = current.getWayNodes()[current.getWayNodes().length - 1].getId();

				// current way end connects to the start of the current polygon (correct direction)
				if (startFirst == currentLast) {
					merge = true;
					it.remove();
					// add way to start of current polygon
					currentPolygonSegments.offerFirst(current);

				}
				// // current way start connects to the start of the current polygon (reversed
				// direction)
				else if (startFirst == currentFirst) {
					current.setReversedInRelation(true);
					merge = true;
					it.remove();
					currentPolygonSegments.offerFirst(current);
				}
				// current way start connects to the end of the current polygon (correct direction)
				else if (endLast == currentFirst) {
					merge = true;
					it.remove();
					// add way to end of current polygon
					currentPolygonSegments.offerLast(current);

				}
				// // current way end connects to the end of the current polygon (reversed direction)
				else if (endLast == currentLast) {
					current.setReversedInRelation(true);
					merge = true;
					it.remove();
					// add way to end of current polygon
					currentPolygonSegments.offerLast(current);

				}
			}

			Deque<TDWay> currentCluster = polygons.get(polygons.size() - 1);
			boolean closed = isClosedPolygon(currentCluster);
			// not a closed polygon and no more ways to merge
			if (!closed) {
				if (ungroupedWays.isEmpty() || !merge) {
					dangling.addAll(polygons.get(polygons.size() - 1));
					// may be a non operation when ungrouped is empty
					dangling.addAll(ungroupedWays);
					polygons.remove(polygons.size() - 1);
					return;
				}
			} else {
				// built a closed polygon and no more ways left --> we are finished
				if (ungroupedWays.isEmpty()) {
					return;
				}

				startNewPolygon = true;

			}

			// if we are here, the polygon is not yet closed, but there are also some ungrouped ways
			// which may be mergeable in the next iteration
		}

	}

	void relatePolygons() {
		outerToInner = new HashMap<Integer, List<Integer>>();
		if (polygons.isEmpty())
			return;

		Polygon[] polygonGeometries = new Polygon[polygons.size()];
		int i = 0;
		for (Deque<TDWay> polygon : polygons) {
			polygonGeometries[i++] = geometryFactory.createPolygon(
					geometryFactory.createLinearRing(toCoordinates(polygon)), null);
		}

		outerToInner = new HashMap<Integer, List<Integer>>();
		HashSet<Integer> inner = new HashSet<Integer>();
		for (int k = 0; k < polygonGeometries.length; k++) {
			if (inner.contains(Integer.valueOf(k)))
				continue;
			for (int l = k + 1; l < polygonGeometries.length; l++) {
				if (inner.contains(Integer.valueOf(l)))
					continue;

				if (polygonGeometries[k].covers(polygonGeometries[l])) {
					List<Integer> inners = outerToInner.get(Integer.valueOf(k));
					if (inners == null) {
						inners = new ArrayList<Integer>();
						outerToInner.put(Integer.valueOf(k), inners);
					}
					inners.add(Integer.valueOf(l));
					inner.add(Integer.valueOf(l));
				} else if (!outerToInner.containsKey(Integer.valueOf(k))
						&& polygonGeometries[l].covers(polygonGeometries[k])) {
					List<Integer> inners = outerToInner.get(Integer.valueOf(l));
					if (inners == null) {
						inners = new ArrayList<Integer>();
						outerToInner.put(Integer.valueOf(l), inners);
					}
					inners.add(Integer.valueOf(k));
					inner.add(Integer.valueOf(k));
				}
			}

			// single polygon without any inner polygons
			if (!outerToInner.containsKey(Integer.valueOf(k)) && !inner.contains(Integer.valueOf(k)))
				outerToInner.put(Integer.valueOf(k), null);

		}
	}

	void polygonizeAndRelate(TDWay[] ways) {
		mergePolygons(ways);
		relatePolygons();
	}

	List<Deque<TDWay>> getPolygons() {
		return polygons;
	}

	List<TDWay> getDangling() {
		return dangling;
	}

	List<TDWay> getIllegal() {
		return illegal;
	}

	HashMap<Integer, List<Integer>> getOuterToInner() {
		return outerToInner;
	}

	private static boolean isClosedPolygon(Deque<TDWay> currentPolygonSegments) {
		TDWay c1Start = currentPolygonSegments.getFirst();
		TDWay c1End = currentPolygonSegments.getLast();

		long startFirst = c1Start.isReversedInRelation() ? c1Start.getWayNodes()[c1Start.getWayNodes().length - 1]
				.getId() : c1Start.getWayNodes()[0].getId();

		long endLast = c1End.isReversedInRelation() ? c1End.getWayNodes()[0].getId()
				: c1End.getWayNodes()[c1End.getWayNodes().length - 1].getId();

		return startFirst == endLast;
	}

	private static boolean isClosedPolygon(TDWay way) {
		TDNode[] waynodes = way.getWayNodes();
		return waynodes[0].getId() == waynodes[waynodes.length - 1].getId();
	}

	private static Coordinate[] toCoordinates(Collection<TDWay> linestrings) {

		Coordinate[][] temp = new Coordinate[linestrings.size()][];
		int i = 0;
		int n = 0;
		for (TDWay tdWay : linestrings) {
			temp[i] = JTSUtils.toCoordinates(tdWay);
			n += temp[i].length;
			++i;
		}
		Coordinate[] res = new Coordinate[n];
		int pos = 0;
		for (i = 0; i < temp.length; i++) {
			System.arraycopy(temp[i], 0, res, pos, temp[i].length);
			pos += temp[i].length;
		}
		return res;
	}

	class PolygonMergeException extends Exception {
		private static final long serialVersionUID = 1L;
	}

	public static void main(String[] args) {
		TDWay w4 = new TDWay(4, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(9, 52455000, 13291000, (short) 0, (byte) 0, "", ""),
				new TDNode(10, 52456000, 13291000, (short) 0, (byte) 0, "", "") });
		TDWay w2 = new TDWay(2, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(10, 52456000, 13291000, (short) 0, (byte) 0, "", ""),
				new TDNode(11, 52456000, 13292000, (short) 0, (byte) 0, "", "") });
		TDWay w6 = new TDWay(6, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(11, 52456000, 13292000, (short) 0, (byte) 0, "", ""),
				new TDNode(0, 52457000, 13293000, (short) 0, (byte) 0, "", "") });
		TDWay w1 = new TDWay(1, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(0, 52457000, 13293000, (short) 0, (byte) 0, "", ""),
				new TDNode(1, 52455000, 13293000, (short) 0, (byte) 0, "", "") });
		TDWay w5 = new TDWay(5, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(1, 52455000, 13293000, (short) 0, (byte) 0, "", ""),
				new TDNode(9, 52455000, 13291000, (short) 0, (byte) 0, "", "") });

		TDWay w3 = new TDWay(3, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(5, 0, 0, (short) 0, (byte) 0, "", ""),
				new TDNode(6, 0, 0, (short) 0, (byte) 0, "", ""),
				new TDNode(5, 0, 0, (short) 0, (byte) 0, "", "") });

		TDWay w7 = new TDWay(7, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(12, 52455500, 13291500, (short) 0, (byte) 0, "", ""),
				new TDNode(13, 52455700, 13291500, (short) 0, (byte) 0, "", ""),
				new TDNode(14, 52455700, 13291700, (short) 0, (byte) 0, "", ""),
				new TDNode(15, 52455500, 13291700, (short) 0, (byte) 0, "", ""),
				new TDNode(12, 52455500, 13291500, (short) 0, (byte) 0, "", "") });

		TDWay w8 = new TDWay(8, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(16, 52454500, 13292000, (short) 0, (byte) 0, "", ""),
				new TDNode(17, 52455500, 13292000, (short) 0, (byte) 0, "", ""),
				new TDNode(18, 52455500, 13292500, (short) 0, (byte) 0, "", "") });

		TDWay w9 = new TDWay(9, (byte) 0, "name1", "ref", new TDNode[] {
				new TDNode(18, 52455500, 13292500, (short) 0, (byte) 0, "", ""),
				new TDNode(19, 52454500, 13292500, (short) 0, (byte) 0, "", ""),
				new TDNode(16, 52454500, 13292000, (short) 0, (byte) 0, "", "") });

		TDWay[] ways = new TDWay[] { w1, w2, w3, w4, w5, w6, w7, w8, w9 };
		WayPolygonizer polygonizer = new WayPolygonizer();
		polygonizer.mergePolygons(ways);
		Collection<Deque<TDWay>> polygons = polygonizer.getPolygons();
		polygonizer.relatePolygons();
		System.out.println(polygons.size());

		for (Deque<TDWay> wayCluster : polygons) {
			System.out.println("CLUSTER:");
			for (TDWay way : wayCluster) {
				System.out.print(way.getId() + "\t");
			}
			System.out.println();
		}
	}

	// public static void main(String[] args) {
	// GeometryFactory geometryFactory = new GeometryFactory();
	// Polygonizer polygonizer = new Polygonizer();
	//
	// Coordinate[] coordinates = new Coordinate[] {
	// new Coordinate(0, 0),
	// new Coordinate(10, 0),
	// new Coordinate(10, 10),
	// new Coordinate(0, 10),
	// new Coordinate(0, 0)
	// };
	//
	// Coordinate[] coordinates2 = new Coordinate[] {
	// new Coordinate(0, 0),
	// new Coordinate(-10, 0),
	// new Coordinate(-10, -10),
	// new Coordinate(0, -10)
	// };
	//
	// Coordinate[] coordinates3 = new Coordinate[] {
	// new Coordinate(0, -10),
	// new Coordinate(0, 0)
	// };
	//
	// Coordinate[] coordinates4 = new Coordinate[] {
	// new Coordinate(2, 2),
	// new Coordinate(4, 2),
	// new Coordinate(4, 4),
	// new Coordinate(2, 4),
	// new Coordinate(2, 2)
	// };
	//
	// Polygon p = geometryFactory.createPolygon(geometryFactory.createLinearRing(coordinates), null);
	// Polygon p4 = geometryFactory
	// .createPolygon(geometryFactory.createLinearRing(coordinates4), null);
	// Geometry p2 = geometryFactory.createLineString(coordinates2);
	// Geometry p3 = geometryFactory.createLineString(coordinates3);
	// polygonizer.add(p);
	// polygonizer.add(p2);
	// polygonizer.add(p3);
	// polygonizer.add(p4);
	// Collection<Polygon> polygons = polygonizer.getPolygons();
	// for (Polygon polygon : polygons) {
	// System.out.println(polygon.toText());
	// }
	// }

}
