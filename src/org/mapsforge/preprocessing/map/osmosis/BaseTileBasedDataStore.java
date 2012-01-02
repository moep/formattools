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
import gnu.trove.map.hash.TShortIntHashMap;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDRelation;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

abstract class BaseTileBasedDataStore implements TileBasedDataStore, NodeResolver, WayResolver {

	protected static final Logger LOGGER =
			Logger.getLogger(BaseTileBasedDataStore.class.getName());

	protected final Rect boundingbox;
	protected TileGridLayout[] tileGridLayouts;
	protected final ZoomIntervalConfiguration zoomIntervalConfiguration;
	protected final int bboxEnlargement;

	protected final TLongObjectHashMap<TLongArrayList> outerToInnerMapping;
	protected final HashMap<TileCoordinate, TLongHashSet> tilesToCoastlines;

	// accounting
	protected float[] countWays;
	protected float[] countWayTileFactor;

	protected final TShortIntHashMap histogramPoiTags;
	protected final TShortIntHashMap histogramWayTags;
	protected long maxWayID = Long.MIN_VALUE;

	public BaseTileBasedDataStore(
			double minLat, double maxLat,
			double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration,
				bboxEnlargement);

	}

	public BaseTileBasedDataStore(Rect bbox,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		super();
		this.boundingbox = bbox;
		this.zoomIntervalConfiguration = zoomIntervalConfiguration;
		this.tileGridLayouts = new TileGridLayout[zoomIntervalConfiguration
				.getNumberOfZoomIntervals()];
		this.bboxEnlargement = bboxEnlargement;

		this.outerToInnerMapping = new TLongObjectHashMap<TLongArrayList>();
		tilesToCoastlines = new HashMap<TileCoordinate, TLongHashSet>();

		this.countWays = new float[zoomIntervalConfiguration.getNumberOfZoomIntervals()];
		this.countWayTileFactor = new float[zoomIntervalConfiguration
				.getNumberOfZoomIntervals()];

		this.histogramPoiTags = new TShortIntHashMap();
		this.histogramWayTags = new TShortIntHashMap();

		// compute horizontal and vertical tile coordinate offsets for all
		// base zoom levels
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			TileCoordinate upperLeft =
					new TileCoordinate((int) MercatorProjection.longitudeToTileX(
							GeoCoordinate.intToDouble(boundingbox.minLongitudeE6),
							zoomIntervalConfiguration.getBaseZoom(i)),
							(int) MercatorProjection.latitudeToTileY(
									GeoCoordinate.intToDouble(boundingbox.maxLatitudeE6),
									zoomIntervalConfiguration.getBaseZoom(i)),
							zoomIntervalConfiguration.getBaseZoom(i));
			this.tileGridLayouts[i] = new TileGridLayout(upperLeft,
					computeNumberOfHorizontalTiles(i), computeNumberOfVerticalTiles(i));
		}
	}

	@Override
	public Rect getBoundingBox() {
		return boundingbox;
	}

	@Override
	public TileGridLayout getTileGridLayout(int zoomIntervalIndex) {
		return tileGridLayouts[zoomIntervalIndex];
	}

	@Override
	public ZoomIntervalConfiguration getZoomIntervalConfiguration() {
		return zoomIntervalConfiguration;
	}

	@Override
	public long cumulatedNumberOfTiles() {
		long cumulated = 0;
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			cumulated += tileGridLayouts[i].getAmountTilesHorizontal() * tileGridLayouts[i]
					.getAmountTilesVertical();
		}
		return cumulated;
	}

	protected void countPoiTags(TDNode poi) {
		if (poi == null || poi.getTags() == null)
			return;
		for (short tag : poi.getTags()) {
			histogramPoiTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(TDWay way) {
		if (way == null || way.getTags() == null)
			return;
		for (short tag : way.getTags()) {
			histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void countWayTags(short[] tags) {
		if (tags == null)
			return;
		for (short tag : tags) {
			histogramWayTags.adjustOrPutValue(tag, 1, 1);
		}
	}

	protected void addPOI(TDNode poi) {
		if (!poi.isPOI())
			return;

		byte minZoomLevel = poi.getZoomAppear();
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {

			// is POI seen in a zoom interval?
			if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
				long tileCoordinateX = MercatorProjection.longitudeToTileX(
						GeoCoordinate.intToDouble(poi.getLongitude()),
						zoomIntervalConfiguration.getBaseZoom(i));
				long tileCoordinateY = MercatorProjection.latitudeToTileY(
						GeoCoordinate.intToDouble(poi.getLatitude()),
						zoomIntervalConfiguration.getBaseZoom(i));
				TileData tileData = getTileImpl(i, (int) tileCoordinateX, (int) tileCoordinateY);
				if (tileData != null) {
					tileData.addPOI(poi);
					countPoiTags(poi);
				}
			}
		}

	}

	protected void addWayToTiles(TDWay way, int enlargement) {
		int bboxEnlargementLocal = enlargement;
		if (way.isCoastline()) {
			// find matching tiles on zoom level 12
			bboxEnlargementLocal = 0;
			Set<TileCoordinate> coastLineTiles = GeoUtils.mapWayToTiles(way,
					TileInfo.TILE_INFO_ZOOMLEVEL, bboxEnlargementLocal);
			for (TileCoordinate tileCoordinate : coastLineTiles) {
				TLongHashSet coastlines = tilesToCoastlines.get(tileCoordinate);
				if (coastlines == null) {
					coastlines = new TLongHashSet();
					tilesToCoastlines.put(tileCoordinate, coastlines);
				}
				coastlines.add(way.getId());
			}
		}

		byte minZoomLevel = way.getMinimumZoomLevel();
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			// is way seen in a zoom interval?
			if (minZoomLevel <= zoomIntervalConfiguration.getMaxZoom(i)) {
				Set<TileCoordinate> matchedTiles = GeoUtils.mapWayToTiles(way,
						zoomIntervalConfiguration.getBaseZoom(i),
						bboxEnlargementLocal);
				boolean added = false;
				for (TileCoordinate matchedTile : matchedTiles) {
					TileData td = getTileImpl(i, matchedTile.getX(), matchedTile.getY());
					if (td != null) {
						countWayTags(way);
						countWayTileFactor[i]++;
						added = true;
						td.addWay(way);
					}
				}
				if (added)
					countWays[i]++;
			}
		}
	}

	abstract protected TileData getTileImpl(int zoom, int tileX, int tileY);

	abstract protected void handleVirtualOuterWay(TDWay virtualWay);

	abstract protected void handleAdditionalRelationTags(TDWay virtualWay,
			TDRelation relation);

	abstract protected void handleVirtualInnerWay(TDWay virtualWay);

	private int computeNumberOfHorizontalTiles(int zoomIntervalIndex) {
		long tileCoordinateLeft = MercatorProjection.longitudeToTileX(
				GeoCoordinate.intToDouble(boundingbox.getMinLongitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateRight = MercatorProjection.longitudeToTileX(
				GeoCoordinate.intToDouble(boundingbox.getMaxLongitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateLeft <= tileCoordinateRight;
		assert tileCoordinateLeft - tileCoordinateRight + 1 < Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex)
				+ "\t+n_horizontal: " + (tileCoordinateRight - tileCoordinateLeft + 1));

		return (int) (tileCoordinateRight - tileCoordinateLeft + 1);

	}

	private int computeNumberOfVerticalTiles(int zoomIntervalIndex) {
		long tileCoordinateBottom = MercatorProjection.latitudeToTileY(
				GeoCoordinate.intToDouble(boundingbox.getMinLatitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		long tileCoordinateTop = MercatorProjection.latitudeToTileY(
				GeoCoordinate.intToDouble(boundingbox.getMaxLatitudeE6()),
				zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex));

		assert tileCoordinateBottom >= tileCoordinateTop;
		assert tileCoordinateBottom - tileCoordinateTop + 1 <= Integer.MAX_VALUE;

		LOGGER.finer("basezoom: " + zoomIntervalConfiguration.getBaseZoom(zoomIntervalIndex)
				+ "\t+n_vertical: " + (tileCoordinateBottom - tileCoordinateTop + 1));

		return (int) (tileCoordinateBottom - tileCoordinateTop + 1);
	}

	protected class RelationHandler implements TObjectProcedure<TDRelation> {

		private final WayPolygonizer polygonizer = new WayPolygonizer();

		private List<Integer> inner;
		private List<Deque<TDWay>> extractedPolygons;
		private HashMap<Integer, List<Integer>> outerToInner;

		@Override
		public boolean execute(TDRelation relation) {
			if (relation == null)
				return false;

			extractedPolygons = null;
			outerToInner = null;

			TDWay[] members = relation.getMemberWays();
			polygonizer.polygonizeAndRelate(members);

			// skip invalid relations
			if (!polygonizer.getDangling().isEmpty()) {
				LOGGER.fine("relation contains dangling ways which could not be merged to polygons: "
						+ relation.getId());
				return true;
			}
			else if (!polygonizer.getIllegal().isEmpty()) {
				LOGGER.fine("relation contains illegal closed ways with fewer than 4 nodes: "
						+ relation.getId());
				return true;
			}

			extractedPolygons = polygonizer.getPolygons();
			outerToInner = polygonizer.getOuterToInner();

			for (Entry<Integer, List<Integer>> entry : outerToInner.entrySet()) {
				Deque<TDWay> outerPolygon = extractedPolygons.get(entry.getKey().intValue());
				inner = null;
				inner = entry.getValue();
				byte shape = TDWay.SIMPLE_POLYGON;
				// does it contain inner ways?
				if (inner != null && !inner.isEmpty())
					shape = TDWay.MULTI_POLYGON;

				TDWay outerWay = null;
				if (outerPolygon.size() > 1) {
					// we need to create a new way from a set of ways
					// collect the way nodes and use the tags of the relation
					// if one of the ways has its own tags, we should ignore them,
					// ways with relevant tags will be added separately later
					if (!relation.isRenderRelevant()) {
						LOGGER.fine("constructed outer polygon in relation has no known tags: "
								+ relation.getId());
						continue;
					}
					// merge way nodes from outer way segments
					List<TDNode> waynodeList = new ArrayList<RAMTileData.TDNode>();
					for (TDWay outerSegment : outerPolygon) {
						if (outerSegment.isReversedInRelation()) {
							for (int i = outerSegment.getWayNodes().length - 1; i >= 0; i--) {
								waynodeList.add(outerSegment.getWayNodes()[i]);
							}
						}
						else {
							for (TDNode tdNode : outerSegment.getWayNodes()) {
								waynodeList.add(tdNode);
							}
						}
					}
					TDNode[] waynodes = waynodeList.toArray(new TDNode[waynodeList.size()]);

					// create new virtual way which represents the outer way
					// use maxWayID counter to create unique id
					outerWay = new TDWay(++maxWayID, relation.getLayer(), relation.getName(),
							relation.getRef(),
							relation.getTags(), shape, waynodes);

					// add the newly created way to matching tiles
					addWayToTiles(outerWay, bboxEnlargement);
					handleVirtualOuterWay(outerWay);
					// adjust tag statistics, cannot be omitted!!!
					countWayTags(relation.getTags());
				}

				// the outer way consists of only one segment
				else {
					outerWay = outerPolygon.getFirst();

					// is it a polygon that we have seen already and which was
					// identified as a polgyon containing inner ways?
					if (outerToInnerMapping.contains(outerWay.getId())) {
						shape = TDWay.MULTI_POLYGON;
					}
					outerWay.setShape(shape);

					// we merge the name, ref, tag information of the relation to the outer way
					// TODO is this true?
					// a relation that addresses an already closed way, is normally used to add
					// additional information to the way
					outerWay.mergeRelationInformation(relation);
					// only consider the way, if it has tags, otherwise the renderer cannot interpret
					// the way
					if (outerWay.isRenderRelevant()) {
						// handle relation tags
						handleAdditionalRelationTags(outerWay, relation);
						addWayToTiles(outerWay, bboxEnlargement);
						countWayTags(outerWay.getTags());
					}
				}

				// relate inner ways to outer way
				addInnerWays(outerWay);
			}
			return true;
		}

		private void addInnerWays(TDWay outer) {
			if (inner != null && !inner.isEmpty()) {

				TLongArrayList innerList = outerToInnerMapping.get(outer.getId());
				if (innerList == null) {
					innerList = new TLongArrayList();
					outerToInnerMapping.put(outer.getId(), innerList);
				}

				for (Integer innerIndex : inner) {
					Deque<TDWay> innerSegments = extractedPolygons.get(innerIndex
							.intValue());
					TDWay innerWay = null;

					if (innerSegments.size() == 1) {
						innerWay = innerSegments.getFirst();
					}
					else {
						List<TDNode> waynodeList = new ArrayList<RAMTileData.TDNode>();
						for (TDWay innerSegment : innerSegments) {
							if (innerSegment.isReversedInRelation()) {
								for (int i = innerSegment.getWayNodes().length - 1; i >= 0; i--) {
									waynodeList.add(innerSegment.getWayNodes()[i]);
								}
							}
							else {
								for (TDNode tdNode : innerSegment.getWayNodes()) {
									waynodeList.add(tdNode);
								}
							}
						}
						TDNode[] waynodes = waynodeList.toArray(new TDNode[waynodeList
								.size()]);
						// TODO which layer?
						innerWay = new TDWay(++maxWayID, (byte) 0, null, null, waynodes);
						handleVirtualInnerWay(innerWay);
						// does not need to be added to corresponding tiles
						// virtual inner ways do not have any tags, they are holes in the outer polygon
					}
					innerList.add(innerWay.getId());
				}
			}
		}

	}

	protected class WayHandler implements TObjectProcedure<TDWay> {

		@Override
		public boolean execute(TDWay way) {

			if (way == null) {
				return true;
			}
			// we only consider ways that have tags and which have not already
			// added as outer way of a relation
			if (way.isRenderRelevant() && !outerToInnerMapping.contains(way.getId())) {
				addWayToTiles(way, bboxEnlargement);
			}

			return true;
		}

	}
}
