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

import gnu.trove.list.array.TShortArrayList;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

abstract class TileData {

	protected static final Logger LOGGER = Logger.getLogger(TileData.class.getName());

	abstract void addPOI(TDNode poi);

	abstract void addWay(TDWay way);

	abstract Map<Byte, List<TDNode>> poisByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel);

	abstract Map<Byte, List<TDWay>> waysByZoomlevel(byte minValidZoomlevel, byte maxValidZoomlevel);

	static class TDNode {

		private static final int MAX_ELEVATION = 9000;
		private static final byte ZOOM_HOUSENUMBER = (byte) 18;
		// private static final byte ZOOM_NAME = (byte) 16;

		private final long id;
		private final int latitude;
		private final int longitude;

		private final short elevation;
		private final String houseNumber;
		private final byte layer;
		private final String name;
		private short[] tags;

		static TDNode fromNode(Node node) {
			// special tags
			short elevation = 0;
			byte layer = 5;
			String name = null;
			String housenumber = null;

			OSMTag currentTag = null;
			TShortArrayList currentTags = new TShortArrayList();

			// Process Tags
			for (Tag tag : node.getTags()) {
				// test for special tags
				if (tag.getKey().equalsIgnoreCase("ele")) {
					try {
						elevation = (short) Double.parseDouble(tag.getValue());
						if (elevation > MAX_ELEVATION) {
							LOGGER.finer("invalid elevation " + elevation + " for node " + node.getId());
							elevation = 0;
						}

					} catch (NumberFormatException e) {
						// nothing to do here as elevation is initialized with 0
					}
				} else if (tag.getKey().equalsIgnoreCase("addr:housenumber")) {
					housenumber = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("name")) {
					name = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("layer")) {
					try {
						layer = Byte.parseByte(tag.getValue());
						if (layer >= -5 && layer <= 5)
							layer += 5;
					} catch (NumberFormatException e) {
						// nothing to do here as layer is initialized with 5
					}
				} else if ((currentTag = MapFileWriterTask.TAG_MAPPING.getPoiTag(tag.getKey(), tag.getValue())) != null) {
					currentTags.add(currentTag.getId());
				}
			}
			return new TDNode(node.getId(), GeoCoordinate.doubleToInt(node.getLatitude()),
					GeoCoordinate.doubleToInt(node.getLongitude()), elevation, layer, housenumber, name,
					currentTags.toArray());
		}

		TDNode(long id, int latitude, int longitude, short elevation, byte layer, String houseNumber,
				String name) {
			this.id = id;
			this.latitude = latitude;
			this.longitude = longitude;
			this.elevation = elevation;
			this.houseNumber = houseNumber;
			this.layer = layer;
			this.name = name;
		}

		TDNode(long id, int latitude, int longitude, short elevation, byte layer, String houseNumber,
				String name, short[] tags) {
			this.id = id;
			this.latitude = latitude;
			this.longitude = longitude;
			this.elevation = elevation;
			this.houseNumber = houseNumber;
			this.layer = layer;
			this.name = name;
			this.tags = tags;
		}

		boolean isPOI() {
			return houseNumber != null || elevation != 0 || tags.length > 0;
		}

		byte getZoomAppear() {
			if (tags == null || tags.length == 0) {
				if (houseNumber != null)
					return ZOOM_HOUSENUMBER;
				return Byte.MAX_VALUE;
			}
			return MapFileWriterTask.TAG_MAPPING.getZoomAppearPOI(tags);
		}

		short[] getTags() {
			return tags;
		}

		void setTags(short[] tags) {
			this.tags = tags;
		}

		long getId() {
			return id;
		}

		int getLatitude() {
			return latitude;
		}

		int getLongitude() {
			return longitude;
		}

		short getElevation() {
			return elevation;
		}

		String getHouseNumber() {
			return houseNumber;
		}

		byte getLayer() {
			return layer;
		}

		String getName() {
			return name;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TDNode other = (TDNode) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TDNode [id=" + id + ", latitude=" + latitude + ", longitude=" + longitude + ", name="
					+ name + ", tags=" + tags + "]";
		}

	}

	static class TDWay {
		static final byte LINE = 0x0;
		static final byte SIMPLE_POLYGON = 0x1;
		static final byte MULTI_POLYGON = 0x2;

		private final long id;
		private final byte layer;
		private String name;
		private String ref;
		private short[] tags;
		private byte shape;
		private final TDNode[] wayNodes;
		private boolean reversedInRelation;

		static TDWay fromWay(Way way, NodeResolver resolver) {
			if (way == null)
				return null;

			// special tags
			byte layer = 5;
			String name = null;
			String ref = null;

			TShortArrayList currentTags = new TShortArrayList();
			OSMTag currentTag = null;

			// Process Tags
			for (Tag tag : way.getTags()) {
				// test for special tags
				if (tag.getKey().equalsIgnoreCase("name")) {
					name = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("layer")) {
					try {
						layer = Byte.parseByte(tag.getValue());
						if (layer >= -5 && layer <= 5)
							layer += 5;
					} catch (NumberFormatException e) {
						// nothing to do here as layer is initialized with 5
					}
				} else if (tag.getKey().equalsIgnoreCase("ref")) {
					ref = tag.getValue();
				} else if ((currentTag = MapFileWriterTask.TAG_MAPPING.getWayTag(tag.getKey(), tag.getValue())) != null) {
					currentTags.add(currentTag.getId());
				}
			}

			// only ways with at least 2 way nodes are valid ways
			if (way.getWayNodes().size() >= 2) {

				boolean validWay = true;
				// retrieve way nodes from data store
				TDNode[] waynodes = new TDNode[way.getWayNodes().size()];
				int i = 0;
				for (WayNode waynode : way.getWayNodes()) {
					// TODO adjust interface to support a method getWayNodes()
					waynodes[i] = resolver.getNode(waynode.getNodeId());
					if (waynodes[i] == null) {
						validWay = false;
						LOGGER.finer("unknown way node: " + waynode.getNodeId() + " in way " + way.getId());
					}
					i++;
				}

				// for a valid way all way nodes must be existent in the input data
				if (validWay) {

					// mark the way as polygon if the first and the last way node are the same
					// and if the way has at least 4 way nodes
					byte shape = LINE;
					if (waynodes[0].getId() == waynodes[waynodes.length - 1].getId()) {
						if (waynodes.length >= GeoUtils.MIN_NODES_POLYGON) {
							shape = SIMPLE_POLYGON;
						} else {
							LOGGER.finer("Found closed polygon with fewer than 4 way nodes. Way-id: "
									+ way.getId());
							return null;
						}
					}

					return new TDWay(way.getId(), layer, name, ref, currentTags.toArray(), shape, waynodes);
				}
			}

			return null;
		}

		TDWay(long id, byte layer, String name, String ref, TDNode[] wayNodes) {
			this.id = id;
			this.layer = layer;
			this.name = name;
			this.ref = ref;
			this.wayNodes = wayNodes;
		}

		TDWay(long id, byte layer, String name, String ref, short[] tags, byte shape, TDNode[] wayNodes) {
			this.id = id;
			this.layer = layer;
			this.name = name;
			this.ref = ref;
			this.tags = tags;
			this.shape = shape;
			this.wayNodes = wayNodes;
		}

		double[] wayNodesAsArray() {
			if (wayNodes == null)
				return null;
			double[] ret = new double[wayNodes.length * 2];
			int i = 0;
			for (TDNode waynode : wayNodes) {
				ret[i++] = GeoCoordinate.intToDouble(waynode.getLongitude());
				ret[i++] = GeoCoordinate.intToDouble(waynode.getLatitude());
			}

			return ret;
		}

		void mergeRelationInformation(TDRelation relation) {
			if (relation.hasTags())
				addTags(relation.getTags());
			if (getName() == null && relation.getName() != null)
				setName(relation.getName());
			if (getRef() == null && relation.getRef() != null)
				setRef(relation.getRef());
		}

		List<GeoCoordinate> wayNodesAsCoordinateList() {
			List<GeoCoordinate> waynodeCoordinates = new ArrayList<GeoCoordinate>();
			for (TDNode waynode : wayNodes) {
				waynodeCoordinates.add(new GeoCoordinate(waynode.getLatitude(), waynode.getLongitude()));
			}

			return waynodeCoordinates;
		}

		byte getMinimumZoomLevel() {
			return MapFileWriterTask.TAG_MAPPING.getZoomAppearWay(tags);
		}

		long getId() {
			return id;
		}

		byte getLayer() {
			return layer;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		String getRef() {
			return ref;
		}

		public void setRef(String ref) {
			this.ref = ref;
		}

		short[] getTags() {
			return tags;
		}

		byte getShape() {
			return shape;
		}

		void setShape(byte shape) {
			this.shape = shape;
		}

		void setTags(short[] tags) {
			this.tags = tags;
		}

		boolean hasTags() {
			return tags != null && tags.length > 0;
		}

		boolean isRenderRelevant() {
			return hasTags() || getName() != null && !getName().isEmpty() || getRef() != null
					&& !getRef().isEmpty();
		}

		void addTags(short[] addendum) {
			if (tags == null)
				tags = addendum;
			else {
				TShortSet tags2 = new TShortHashSet();
				tags2.addAll(tags);
				tags2.addAll(addendum);
				tags = tags2.toArray();
			}

		}

		void removeTags(short[] substract) {
			TShortSet tags2 = new TShortHashSet();
			tags2.addAll(tags);
			tags2.removeAll(substract);
			tags = tags2.toArray();
		}

		boolean isPolygon() {
			return wayNodes != null && wayNodes.length >= GeoUtils.MIN_NODES_POLYGON
					&& wayNodes[0].getId() == wayNodes[wayNodes.length - 1].getId();
		}

		boolean isCoastline() {
			if (tags == null)
				return false;
			OSMTag tag;
			for (short tagID : tags) {
				tag = MapFileWriterTask.TAG_MAPPING.getWayTag(tagID);
				if (tag.isCoastline())
					return true;
			}

			return false;
		}

		TDNode[] getWayNodes() {
			return wayNodes;
		}

		public boolean isReversedInRelation() {
			return reversedInRelation;
		}

		public void setReversedInRelation(boolean reversedInRelation) {
			this.reversedInRelation = reversedInRelation;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TDWay other = (TDWay) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TDWay [id=" + id + ", name=" + name + ", tags=" + tags + ", polygon=" + shape + "]";
		}

	}

	// TODO adjust if more relations should be supported
	static boolean knownRelationType(String type) {
		return type != null && type.equals("multipolygon");
	}

	static class TDRelation {
		private final long id;
		private final byte layer;
		private final String name;
		private final String ref;
		private final short[] tags;
		private final TDWay[] memberWays;

		static TDRelation fromRelation(Relation way, WayResolver resolver) {
			if (way == null)
				return null;

			if (way.getMembers().size() == 0)
				return null;

			// special tags
			// TODO what about the layer of relations?
			byte layer = 5;
			String name = null;
			String ref = null;

			TShortArrayList currentTags = new TShortArrayList();
			OSMTag currentTag = null;
			String relationType = null;

			// Process Tags
			for (Tag tag : way.getTags()) {
				// test for special tags
				if (tag.getKey().equalsIgnoreCase("name")) {
					name = tag.getValue();
				} else if (tag.getKey().equals("boundary")) {
					return null;
				} else if (tag.getKey().equalsIgnoreCase("layer")) {
					try {
						layer = Byte.parseByte(tag.getValue());
						if (layer >= -5 && layer <= 5)
							layer += 5;
					} catch (NumberFormatException e) {
						// nothing to do here as layer is initialized with 5
					}
				} else if (tag.getKey().equalsIgnoreCase("ref")) {
					ref = tag.getValue();
				} else if (tag.getKey().equalsIgnoreCase("type")) {
					relationType = tag.getValue();
				} else if ((currentTag = MapFileWriterTask.TAG_MAPPING.getWayTag(tag.getKey(), tag.getValue())) != null) {
					currentTags.add(currentTag.getId());
				}
			}

			if (!knownRelationType(relationType))
				return null;

			List<RelationMember> members = way.getMembers();
			List<TDWay> wayMembers = new ArrayList<RAMTileData.TDWay>();
			for (RelationMember relationMember : members) {
				if (relationMember.getMemberType() != EntityType.Way)
					continue;
				TDWay member = resolver.getWay(relationMember.getMemberId());
				if (member == null) {
					// TODO logging
					continue;
				}
				wayMembers.add(member);
			}

			if (wayMembers.isEmpty()) {
				// TODO logging
				return null;
			}

			return new TDRelation(way.getId(), layer, name, ref, currentTags.toArray(),
					wayMembers.toArray(new TDWay[wayMembers.size()]));
		}

		TDRelation(long id, byte layer, String name, String ref, short[] tags, TDWay[] memberWays) {
			this.id = id;
			this.layer = layer;
			this.name = name;
			this.ref = ref;
			this.tags = tags;
			this.memberWays = memberWays;
		}

		long getId() {
			return id;
		}

		byte getLayer() {
			return layer;
		}

		String getName() {
			return name;
		}

		String getRef() {
			return ref;
		}

		short[] getTags() {
			return tags;
		}

		boolean hasTags() {
			return tags != null && tags.length > 0;
		}

		boolean isRenderRelevant() {
			return hasTags() || getName() != null && !getName().isEmpty() || getRef() != null
					&& !getRef().isEmpty();
		}

		TDWay[] getMemberWays() {
			return memberWays;
		}

		boolean isCoastline() {
			if (tags == null)
				return false;
			OSMTag tag;
			for (short tagID : tags) {
				tag = MapFileWriterTask.TAG_MAPPING.getWayTag(tagID);
				if (tag.isCoastline())
					return true;
			}

			return false;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (id ^ (id >>> 32));
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TDRelation other = (TDRelation) obj;
			if (id != other.id)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TDRelation [id=" + id + ", layer=" + layer + ", name=" + name + ", ref=" + ref + ", tags="
					+ Arrays.toString(tags) + "]";
		}

	}

}
