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

import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;
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
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.store.IndexedObjectStore;
import org.openstreetmap.osmosis.core.store.IndexedObjectStoreReader;
import org.openstreetmap.osmosis.core.store.NoSuchIndexElementException;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;

/**
 * A TileBasedDataStore that uses the hard disk as storage device for temporary data structures.
 * 
 * @author bross
 */
final class HDTileBasedDataStore extends BaseTileBasedDataStore {

	private final IndexedObjectStore<Node> indexedNodeStore;
	private final IndexedObjectStore<Way> indexedWayStore;
	private final SimpleObjectStore<Way> wayStore;
	private final SimpleObjectStore<Relation> relationStore;
	private final HDTileData[][][] tileData;

	protected final TLongObjectMap<TDWay> virtualWays;
	protected final TLongObjectMap<List<TDRelation>> additionalRelationTags;

	private IndexedObjectStoreReader<Node> nodeIndexReader;
	private IndexedObjectStoreReader<Way> wayIndexReader;

	private HDTileBasedDataStore(double minLat, double maxLat, double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		this(new Rect(minLon, maxLon, minLat, maxLat), zoomIntervalConfiguration, bboxEnlargement);
	}

	private HDTileBasedDataStore(Rect bbox, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		super(bbox, zoomIntervalConfiguration, bboxEnlargement);
		indexedNodeStore = new IndexedObjectStore<Node>(new SingleClassObjectSerializationFactory(Node.class),
				"idxNodes");
		indexedWayStore = new IndexedObjectStore<Way>(new SingleClassObjectSerializationFactory(Way.class),
				"idxWays");
		// indexedRelationStore = new IndexedObjectStore<Relation>(
		// new SingleClassObjectSerializationFactory(
		// Relation.class), "idxWays");
		wayStore = new SimpleObjectStore<Way>(new SingleClassObjectSerializationFactory(Way.class), "heapWays",
				true);
		relationStore = new SimpleObjectStore<Relation>(new SingleClassObjectSerializationFactory(
				Relation.class), "heapRelations", true);

		tileData = new HDTileData[zoomIntervalConfiguration.getNumberOfZoomIntervals()][][];
		for (int i = 0; i < zoomIntervalConfiguration.getNumberOfZoomIntervals(); i++) {
			this.tileData[i] = new HDTileData[tileGridLayouts[i].getAmountTilesHorizontal()][tileGridLayouts[i]
					.getAmountTilesVertical()];
		}
		virtualWays = new TLongObjectHashMap<RAMTileData.TDWay>();
		additionalRelationTags = new TLongObjectHashMap<List<TDRelation>>();
	}

	static HDTileBasedDataStore newInstance(Rect bbox, ZoomIntervalConfiguration zoomIntervalConfiguration,
			int bboxEnlargement) {
		return new HDTileBasedDataStore(bbox, zoomIntervalConfiguration, bboxEnlargement);
	}

	static HDTileBasedDataStore newInstance(double minLat, double maxLat, double minLon, double maxLon,
			ZoomIntervalConfiguration zoomIntervalConfiguration, int bboxEnlargement) {
		return new HDTileBasedDataStore(minLat, maxLat, minLon, maxLon, zoomIntervalConfiguration,
				bboxEnlargement);
	}

	static HDTileBasedDataStore getStandardInstance(double minLat, double maxLat, double minLon, double maxLon,
			int bboxEnlargement) {

		return new HDTileBasedDataStore(minLat, maxLat, minLon, maxLon,
				ZoomIntervalConfiguration.getStandardConfiguration(), bboxEnlargement);
	}

	@Override
	public boolean addNode(Node node) {
		indexedNodeStore.add(node.getId(), node);
		TDNode tdNode = TDNode.fromNode(node);
		addPOI(tdNode);
		return true;
	}

	@Override
	public boolean addWay(Way way) {
		wayStore.add(way);
		indexedWayStore.add(way.getId(), way);
		maxWayID = Math.max(way.getId(), maxWayID);
		return true;
	}

	@Override
	public void addRelation(Relation relation) {
		relationStore.add(relation);
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
			TDWay current = null;
			try {
				current = TDWay.fromWay(wayIndexReader.get(id), this);
			} catch (NoSuchIndexElementException e) {
				current = virtualWays.get(id);
				if (current == null) {
					LOGGER.fine("multipolygon with outer way id " + id + " references non-existing inner way "
							+ id);
					continue;
				}
			}

			res.add(current);
		}

		return res;
	}

	@Override
	public TileData getTile(int baseZoomIndex, int tileCoordinateX, int tileCoordinateY) {
		HDTileData hdt = getTileImpl(baseZoomIndex, tileCoordinateX, tileCoordinateY);
		if (hdt == null)
			return null;

		return fromHDTileData(hdt);
	}

	@Override
	public Set<TDWay> getCoastLines(TileCoordinate tc) {
		if (tc.getZoomlevel() <= TileInfo.TILE_INFO_ZOOMLEVEL)
			return Collections.emptySet();
		TileCoordinate correspondingOceanTile = tc.translateToZoomLevel(TileInfo.TILE_INFO_ZOOMLEVEL).get(0);

		if (wayIndexReader == null)
			throw new IllegalStateException("way store not accessible, call complete() first");

		TLongHashSet coastlines = tilesToCoastlines.get(correspondingOceanTile);
		if (coastlines == null)
			return Collections.emptySet();

		TLongIterator it = coastlines.iterator();
		HashSet<TDWay> coastlinesAsTDWay = new HashSet<RAMTileData.TDWay>(coastlines.size());
		while (it.hasNext()) {
			long id = it.next();
			TDWay tdWay = null;
			try {
				tdWay = TDWay.fromWay(wayIndexReader.get(id), this);
			} catch (NoSuchIndexElementException e) {
				LOGGER.finer("coastline way non-existing" + id);
			}
			if (tdWay != null)
				coastlinesAsTDWay.add(tdWay);
		}
		return coastlinesAsTDWay;
	}

	// TODO add accounting of average number of tiles per way
	@Override
	public void complete() {
		indexedNodeStore.complete();
		nodeIndexReader = indexedNodeStore.createReader();

		indexedWayStore.complete();
		wayIndexReader = indexedWayStore.createReader();

		// handle relations
		ReleasableIterator<Relation> relationReader = relationStore.iterate();
		RelationHandler relationHandler = new RelationHandler();
		while (relationReader.hasNext()) {
			Relation entry = relationReader.next();
			TDRelation tdRelation = TDRelation.fromRelation(entry, this);
			relationHandler.execute(tdRelation);
		}

		// handle ways
		ReleasableIterator<Way> wayReader = wayStore.iterate();
		WayHandler wayHandler = new WayHandler();
		while (wayReader.hasNext()) {
			TDWay way = TDWay.fromWay(wayReader.next(), this);
			if (way == null)
				continue;
			List<TDRelation> associatedRelations = additionalRelationTags.get(way.getId());
			if (associatedRelations != null) {
				for (TDRelation tileDataRelation : associatedRelations) {
					way.mergeRelationInformation(tileDataRelation);
				}
			}

			wayHandler.execute(way);
		}

		MapFileWriterTask.TAG_MAPPING.optimizePoiOrdering(histogramPoiTags);
		MapFileWriterTask.TAG_MAPPING.optimizeWayOrdering(histogramWayTags);
	}

	@Override
	protected void handleVirtualOuterWay(TDWay virtualWay) {
		virtualWays.put(virtualWay.getId(), virtualWay);
	}

	@Override
	protected void handleAdditionalRelationTags(TDWay way, TDRelation relation) {
		List<TDRelation> associatedRelations = additionalRelationTags.get(way.getId());
		if (associatedRelations == null) {
			associatedRelations = new ArrayList<TileData.TDRelation>();
			additionalRelationTags.put(way.getId(), associatedRelations);
		}
		associatedRelations.add(relation);
	}

	@Override
	protected void handleVirtualInnerWay(TDWay virtualWay) {
		virtualWays.put(virtualWay.getId(), virtualWay);
	}

	@Override
	public void release() {

		this.indexedNodeStore.release();
		this.indexedWayStore.release();
		this.wayStore.release();
		this.relationStore.release();
	}

	@Override
	public TDNode getNode(long id) {
		if (nodeIndexReader == null)
			throw new IllegalStateException("node store not accessible, call complete() first");

		try {
			return TDNode.fromNode(nodeIndexReader.get(id));
		} catch (NoSuchIndexElementException e) {
			LOGGER.finer("node cannot be found in index: " + id);
			return null;
		}
	}

	@Override
	public TDWay getWay(long id) {
		if (wayIndexReader == null)
			throw new IllegalStateException("way store not accessible, call complete() first");

		try {
			return TDWay.fromWay(wayIndexReader.get(id), this);
		} catch (NoSuchIndexElementException e) {
			LOGGER.finer("way cannot be found in index: " + id);
			return null;
		}
	}

	@Override
	protected HDTileData getTileImpl(int zoom, int tileX, int tileY) {
		int tileCoordinateXIndex = tileX - tileGridLayouts[zoom].getUpperLeft().getX();
		int tileCoordinateYIndex = tileY - tileGridLayouts[zoom].getUpperLeft().getY();
		// check for valid range
		if (tileCoordinateXIndex < 0 || tileCoordinateYIndex < 0
				|| tileData[zoom].length <= tileCoordinateXIndex
				|| tileData[zoom][tileCoordinateXIndex].length <= tileCoordinateYIndex)
			return null;

		HDTileData td = tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex];
		if (td == null) {
			td = new HDTileData();
			tileData[zoom][tileCoordinateXIndex][tileCoordinateYIndex] = td;
		}

		return td;
	}

	private RAMTileData fromHDTileData(HDTileData hdt) {
		final RAMTileData td = new RAMTileData();
		TLongIterator it = hdt.getPois().iterator();
		while (it.hasNext()) {
			td.addPOI(TDNode.fromNode(nodeIndexReader.get(it.next())));
		}

		it = hdt.getWays().iterator();
		while (it.hasNext()) {
			TDWay way = null;
			long id = it.next();
			try {
				way = TDWay.fromWay(wayIndexReader.get(id), this);
				td.addWay(way);
			} catch (NoSuchIndexElementException e) {
				// is it a virtual way?
				way = virtualWays.get(id);
				if (way != null)
					td.addWay(way);
				else
					LOGGER.finer("referenced way non-existing" + id);
			}

			if (way != null) {
				if (outerToInnerMapping.contains(way.getId()))
					way.setShape(TDWay.MULTI_POLYGON);

				List<TDRelation> associatedRelations = additionalRelationTags.get(id);
				if (associatedRelations != null) {
					for (TDRelation tileDataRelation : associatedRelations) {
						way.mergeRelationInformation(tileDataRelation);
					}
				}
			}
		}

		return td;
	}
}
