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
import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.procedure.TLongProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDRelation;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

/**
 * A TileBasedDataStore that uses the RAM as storage device for temporary data structures.
 * 
 * @author bross
 */
final class RAMTileBasedDataStore extends BaseTileBasedDataStore {
	private final TLongObjectHashMap<TDNode> nodes;
	protected final TLongObjectHashMap<TDWay> ways;
	protected final TLongObjectHashMap<TDRelation> multipolygons;

	private final RAMTileData[][][] tileData;

	private RAMTileBasedDataStore(double minLat, double maxLat, double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration, bboxEnlargement);
	}

	private RAMTileBasedDataStore(Rect bbox, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		super(bbox, zoomIntervalConfiguration, bboxEnlargement);
		this.nodes = new TLongObjectHashMap<TDNode>();
		this.ways = new TLongObjectHashMap<TDWay>();
		this.multipolygons = new TLongObjectHashMap<TDRelation>();
		this.tileData = new RAMTileData[zoomIntervalConfiguration.getNumberOfZoomIntervals()][][];
		// compute number of tiles needed on each base zoom level
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			this.tileData[i] = new RAMTileData[tileGridLayouts[i].getAmountTilesHorizontal()][tileGridLayouts[i]
					.getAmountTilesVertical()];
		}
	}

	static RAMTileBasedDataStore newInstance(Rect bbox, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		return new RAMTileBasedDataStore(bbox, zoomIntervalConfiguration, bboxEnlargement);
	}

	static RAMTileBasedDataStore newInstance(double minLat, double maxLat, double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		return new RAMTileBasedDataStore(minLat, maxLat, minLon, maxLon, zoomIntervalConfiguration,
				bboxEnlargement);
	}

	static RAMTileBasedDataStore getStandardInstance(double minLat, double maxLat, double minLon,
			double maxLon, int bboxEnlargement) {

		return new RAMTileBasedDataStore(minLat, maxLat, minLon, maxLon,
				ZoomIntervalConfiguration.getStandardConfiguration(), bboxEnlargement);
	}

	@Override
	public TDNode getNode(long id) {
		return nodes.get(id);
	}

	@Override
	public TDWay getWay(long id) {
		return ways.get(id);
	}

	@Override
	public Rect getBoundingBox() {
		return boundingbox;
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return zoomIntervalConfiguration;
	}

	@Override
	public boolean addNode(Node node) {
		TDNode tdNode = TDNode.fromNode(node);
		nodes.put(tdNode.getId(), tdNode);
		addPOI(tdNode);
		return true;
	}

	@Override
	public boolean addWay(Way way) {
		TDWay tdWay = TDWay.fromWay(way, this);
		if (tdWay == null)
			return false;
		ways.put(tdWay.getId(), tdWay);
		maxWayID = Math.max(maxWayID, way.getId());

		if (tdWay.isCoastline()) {
			// find matching tiles on zoom level 12
			Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(tdWay, TileInfo.TILE_INFO_ZOOMLEVEL, 0);
			for (TileCoordinate tileCoordinate : coastLineTiles) {
				TLongHashSet coastlines = tilesToCoastlines.get(tileCoordinate);
				if (coastlines == null) {
					coastlines = new TLongHashSet();
					tilesToCoastlines.put(tileCoordinate, coastlines);
				}
				coastlines.add(tdWay.getId());
			}
		}

		return true;
	}

	@Override
	public void addRelation(Relation relation) {
		TDRelation tdRelation = TDRelation.fromRelation(relation, this);
		if (tdRelation != null) {
			multipolygons.put(relation.getId(), tdRelation);
		}
	}

	@Override
	public void complete() {
		// Polygonize multipolygon
		RelationHandler relationHandler = new RelationHandler();
		multipolygons.forEachValue(relationHandler);

		WayHandler wayHandler = new WayHandler();
		ways.forEachValue(wayHandler);

		MapFileWriterTask.TAG_MAPPING.optimizePoiOrdering(histogramPoiTags);
		MapFileWriterTask.TAG_MAPPING.optimizeWayOrdering(histogramWayTags);
	}

	@Override
	public TileData getTile(int zoom, int tileX, int tileY) {
		return getTileImpl(zoom, tileX, tileY);
	}

	@Override
	protected RAMTileData getTileImpl(int zoom, int tileX, int tileY) {
		int tileCoordinateXIndex = tileX - tileGridLayouts[zoom].getUpperLeft().getX();
		int tileCoordinateYIndex = tileY - tileGridLayouts[zoom].getUpperLeft().getY();
		// check for valid range
		if (tileCoordinateXIndex < 0 || tileCoordinateYIndex < 0
				|| tileData[zoom].length <= tileCoordinateXIndex
				|| tileData[zoom][tileCoordinateXIndex].length <= tileCoordinateYIndex)
			return null;

		RAMTileData td = tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex];
		if (td == null) {
			td = new RAMTileData();
			tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex] = td;
		}

		return td;
	}

	@Override
	public Set<TDWay> getCoastLines(TileCoordinate tc) {
		if (tc.getZoomlevel() <= TileInfo.TILE_INFO_ZOOMLEVEL)
			return Collections.emptySet();
		TileCoordinate correspondingOceanTile = tc.translateToZoomLevel(TileInfo.TILE_INFO_ZOOMLEVEL).get(0);
		TLongHashSet coastlines = tilesToCoastlines.get(correspondingOceanTile);
		if (coastlines == null)
			return Collections.emptySet();

		final Set<TDWay> res = new HashSet<TileData.TDWay>();
		coastlines.forEach(new TLongProcedure() {
			@Override
			public boolean execute(long id) {
				TDWay way = ways.get(id);
				if (way != null) {
					res.add(way);
					return true;
				}
				return false;
			}
		});
		return res;
	}

	@Override
	public void release() {
		// nothing to do here
	}

	@Override
	public List<TDWay> getInnerWaysOfMultipolygon(long outerWayID) {
		TLongArrayList innerwayIDs = outerToInnerMapping.get(outerWayID);
		if (innerwayIDs == null)
			return null;
		return getInnerWaysOfMultipolygon(innerwayIDs.toArray());
	}

	private List<TDWay> getInnerWaysOfMultipolygon(long[] innerWayIDs) {
		if (innerWayIDs == null)
			return Collections.emptyList();
		List<TDWay> res = new ArrayList<RAMTileData.TDWay>();
		for (long id : innerWayIDs) {
			TDWay current = ways.get(id);
			if (current == null)
				continue;
			res.add(current);
		}

		return res;
	}

	@Override
	protected void handleVirtualOuterWay(TDWay virtualWay) {
		// nothing to do here
	}

	@Override
	protected void handleAdditionalRelationTags(TDWay virtualWay, TDRelation relation) {
		// nothing to do here
	}

	@Override
	protected void handleVirtualInnerWay(TDWay virtualWay) {
		ways.put(virtualWay.getId(), virtualWay);
	}

}
