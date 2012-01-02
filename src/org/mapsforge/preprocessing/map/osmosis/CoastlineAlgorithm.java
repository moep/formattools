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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * The CoastlineAlgorithm generates closed polygons from disjoint coastline segments. The
 * algorithm is based on the close-areas.pl script, written by Frederik Ramm for the Osmarender
 * program. This implementation is optimized for high performance and memory reusing.
 */
class CoastlineAlgorithm {
	/**
	 * An immutable container for the two endpoints of a coastline segment.
	 */
	private static class EndPoints {
		private final ImmutablePoint end;
		private final int hashCode;
		private EndPoints other;
		private final ImmutablePoint start;

		/**
		 * Creates a new EndPoints instance with the given points.
		 * 
		 * @param start
		 *            the start point.
		 * @param end
		 *            the end point.
		 */
		EndPoints(ImmutablePoint start, ImmutablePoint end) {
			this.start = start;
			this.end = end;
			this.hashCode = calculateHashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (!(obj instanceof EndPoints)) {
				return false;
			} else {
				this.other = (EndPoints) obj;
				if (!this.start.equals(this.other.start)) {
					return false;
				} else if (!this.end.equals(this.other.end)) {
					return false;
				}
				return true;
			}
		}

		@Override
		public int hashCode() {
			return this.hashCode;
		}

		/**
		 * Calculates the hash value of this object.
		 * 
		 * @return the hash value of this object.
		 */
		private int calculateHashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.start == null) ? 0 : this.start.hashCode());
			result = prime * result + ((this.end == null) ? 0 : this.end.hashCode());
			return result;
		}
	}

	/**
	 * A HelperPoint represents one of the four corners of the virtual tile.
	 */
	private static class HelperPoint {
		int x;
		int y;

		HelperPoint() {
			// do nothing
		}
	}

	/**
	 * Interface which must be implemented to handle all events during polygon creation.
	 */
	interface ClosedPolygonHandler {
		/**
		 * Called when an invalid coastline segment has been detected.
		 * 
		 * @param coastline
		 *            the coordinates of the invalid coastline segment.
		 */
		void onInvalidCoastlineSegment(float[] coastline);

		/**
		 * Called when a closed island polygon has been generated.
		 * 
		 * @param coastline
		 *            the coordinates of the closed island polygon.
		 */
		void onIslandPolygon(float[] coastline);

		/**
		 * Called when a valid coastline segment has been detected.
		 * 
		 * @param coastline
		 *            the coordinates of the valid coastline segment.
		 */
		void onValidCoastlineSegment(float[] coastline);

		/**
		 * Called when a closed water polygon has been generated.
		 * 
		 * @param coastline
		 *            the coordinates of the closed water polygon.
		 */
		void onWaterPolygon(float[] coastline);

		/**
		 * Called when a water tile has been detected.
		 */
		void onWaterTile();
	}

	private final ArrayList<HelperPoint> additionalCoastlinePoints;
	private CoastlineWay coastlineEnd;
	private int coastlineEndLength;
	private ImmutablePoint coastlineEndPoint;
	private final TreeMap<ImmutablePoint, float[]> coastlineEnds;
	private final ArrayList<float[]> coastlineSegments;
	private CoastlineWay coastlineStart;
	private int coastlineStartLength;
	private ImmutablePoint coastlineStartPoint;
	private final TreeMap<ImmutablePoint, float[]> coastlineStarts;
	private final Comparator<CoastlineWay> coastlineWayComparator;
	private final ArrayList<CoastlineWay> coastlineWays;
	private float[] coordinates;
	private int currentSide;
	private EndPoints endPoints;
	private final HashSet<EndPoints> handledCoastlineSegments;
	private final HelperPoint[] helperPoints;
	private boolean invalidCoastline;
	private boolean islandSituation;
	private float[] matchPath;
	private boolean needHelperPoint;
	private float[] newPath;
	private float[] nodesSequence;
	private int relativeX1;
	private int relativeX2;
	private int relativeY1;
	private int relativeY2;
	private final int[] virtualTileBoundaries;
	private int virtualTileSize;
	private boolean waterBackground;
	private int zoomLevelDifference;

	/**
	 * Constructs a new CoastlineAlgorithm instance to generate closed polygons.
	 */
	CoastlineAlgorithm() {
		// set up the comparator for coastline segments entering the tile
		this.coastlineWayComparator = new Comparator<CoastlineWay>() {
			@Override
			public int compare(CoastlineWay o1, CoastlineWay o2) {
				if (o1.entryAngle > o2.entryAngle) {
					return 1;
				}
				return -1;
			}
		};

		// create the four helper points at the tile corners
		this.helperPoints = new HelperPoint[4];
		this.helperPoints[0] = new HelperPoint();
		this.helperPoints[1] = new HelperPoint();
		this.helperPoints[2] = new HelperPoint();
		this.helperPoints[3] = new HelperPoint();

		this.additionalCoastlinePoints = new ArrayList<HelperPoint>(4);
		this.coastlineWays = new ArrayList<CoastlineWay>(4);

		// create the data structures for the coastline segments
		this.coastlineSegments = new ArrayList<float[]>(8);
		this.coastlineEnds = new TreeMap<ImmutablePoint, float[]>();
		this.coastlineStarts = new TreeMap<ImmutablePoint, float[]>();
		this.handledCoastlineSegments = new HashSet<EndPoints>(64);

		this.virtualTileBoundaries = new int[4];
	}

	/**
	 * Adds a coastline segment to the internal data structures. Coastline segments are
	 * automatically merged into longer parts when they share the same start or end point.
	 * Adding the same coastline segment more than once has no effect.
	 * 
	 * @param coastline
	 *            the coordinates of the coastline segment.
	 */
	void addCoastlineSegment(float[] coastline) {
		// all coastline segments are accumulated and merged together if possible
		this.nodesSequence = coastline;
		this.coastlineStartPoint = new ImmutablePoint(this.nodesSequence[0],
				this.nodesSequence[1]);
		this.coastlineEndPoint = new ImmutablePoint(
				this.nodesSequence[this.nodesSequence.length - 2],
				this.nodesSequence[this.nodesSequence.length - 1]);
		this.endPoints = new EndPoints(this.coastlineStartPoint, this.coastlineEndPoint);

		// check for an already closed coastline segment
		if (this.coastlineStartPoint.equals(this.coastlineEndPoint)) {
			this.coastlineSegments.add(this.nodesSequence);
			return;
		}

		// check to avoid duplicate coastline segments
		if (!this.handledCoastlineSegments.contains(this.endPoints)) {
			// update the set of handled coastline segments
			this.handledCoastlineSegments.add(new EndPoints(this.coastlineStartPoint,
					this.coastlineEndPoint));

			// check if a data way starts with the last point of the current way
			if (this.coastlineStarts.containsKey(this.coastlineEndPoint)) {
				// merge both way segments
				this.matchPath = this.coastlineStarts.remove(this.coastlineEndPoint);
				this.newPath = new float[this.nodesSequence.length + this.matchPath.length - 2];
				System.arraycopy(this.nodesSequence, 0, this.newPath, 0,
						this.nodesSequence.length - 2);
				System.arraycopy(this.matchPath, 0, this.newPath,
						this.nodesSequence.length - 2, this.matchPath.length);
				this.nodesSequence = this.newPath;
				this.coastlineEndPoint = new ImmutablePoint(
						this.nodesSequence[this.nodesSequence.length - 2],
						this.nodesSequence[this.nodesSequence.length - 1]);
			}

			// check if a data way ends with the first point of the current way
			if (this.coastlineEnds.containsKey(this.coastlineStartPoint)) {
				this.matchPath = this.coastlineEnds.remove(this.coastlineStartPoint);
				// check if the merged way is already a circle
				if (!this.coastlineStartPoint.equals(this.coastlineEndPoint)) {
					// merge both way segments
					this.newPath = new float[this.nodesSequence.length + this.matchPath.length
							- 2];
					System.arraycopy(this.matchPath, 0, this.newPath, 0,
							this.matchPath.length - 2);
					System.arraycopy(this.nodesSequence, 0, this.newPath,
							this.matchPath.length - 2, this.nodesSequence.length);
					this.nodesSequence = this.newPath;
					this.coastlineStartPoint = new ImmutablePoint(this.nodesSequence[0],
							this.nodesSequence[1]);
				}
			}

			this.coastlineStarts.put(this.coastlineStartPoint, this.nodesSequence);
			this.coastlineEnds.put(this.coastlineEndPoint, this.nodesSequence);
		}
	}

	/**
	 * Clears the internal data structures. Must be called between tiles.
	 */
	void clearCoastlineSegments() {
		this.coastlineSegments.clear();
		this.coastlineStarts.clear();
		this.coastlineEnds.clear();
		this.handledCoastlineSegments.clear();
		this.coastlineWays.clear();
	}

	/**
	 * Generates closed water and land polygons from unconnected coastline segments. Closed
	 * segments are handled either as water or islands, depending on their orientation.
	 * 
	 * @param closedPolygonHandler
	 *            the implementation which will be called to handle the generated polygons.
	 */
	void generateClosedPolygons(ClosedPolygonHandler closedPolygonHandler) {
		this.coastlineSegments.addAll(this.coastlineStarts.values());

		// check if there are any coastline segments
		if (this.coastlineSegments.isEmpty()) {
			return;
		}

		this.islandSituation = false;
		this.waterBackground = true;
		this.invalidCoastline = false;
		for (float[] coastline : this.coastlineSegments) {
			// is the current segment already closed?
			if (CoastlineWay.isClosed(coastline)) {
				// depending on the orientation we have either water or an island
				if (CoastlineWay.isClockWise(coastline)) {
					// water
					this.waterBackground = false;
					closedPolygonHandler.onWaterPolygon(coastline);
				} else {
					// island
					this.islandSituation = true;
					closedPolygonHandler.onIslandPolygon(coastline);
				}
			} else if (CoastlineWay.isValid(coastline, this.virtualTileBoundaries)) {
				coastline = SutherlandHodgmanClipping.clipPolyline(coastline,
						this.virtualTileBoundaries);
				if (coastline != null) {
					this.coastlineWays.add(new CoastlineWay(coastline,
							this.virtualTileBoundaries, this.virtualTileSize));
				}
			} else {
				this.invalidCoastline = true;
				closedPolygonHandler.onInvalidCoastlineSegment(coastline);
			}
		}

		if (this.invalidCoastline) {
			// do not create any closed polygons, just draw the coastline segments
			for (CoastlineWay coastlineWay : this.coastlineWays) {
				closedPolygonHandler.onValidCoastlineSegment(coastlineWay.data);
			}
			return;
		}

		// check if there are no errors and the tile needs a water background
		if (this.islandSituation && this.waterBackground && this.coastlineWays.isEmpty()) {
			// add a water polygon for the whole tile
			closedPolygonHandler.onWaterTile();
			return;
		}

		// order all coastline segments ascending by their entering angle
		Collections.sort(this.coastlineWays, this.coastlineWayComparator);

		// join coastline segments to create closed water segments
		while (!this.coastlineWays.isEmpty()) {
			this.coastlineStart = this.coastlineWays.get(0);
			this.coastlineEnd = null;
			// try to find a matching coastline segment
			for (CoastlineWay coastline : this.coastlineWays) {
				if (coastline.entryAngle > this.coastlineStart.exitAngle) {
					this.coastlineEnd = coastline;
					break;
				}
			}
			if (this.coastlineEnd == null) {
				// no coastline segment was found, take the first one
				this.coastlineEnd = this.coastlineWays.get(0);
			}
			this.coastlineWays.remove(0);

			// if the segment orientation is clockwise, we need at least one helper point
			if (this.coastlineEnd.entrySide == 0 && this.coastlineStart.exitSide == 0) {
				this.needHelperPoint = (this.coastlineStart.exitAngle > this.coastlineEnd.entryAngle && (this.coastlineStart.exitAngle - this.coastlineEnd.entryAngle) < Math.PI)
						|| (this.coastlineStart.exitAngle < Math.PI && this.coastlineEnd.entryAngle > Math.PI);
			} else {
				this.needHelperPoint = this.coastlineStart.exitAngle > this.coastlineEnd.entryAngle;
			}

			this.additionalCoastlinePoints.clear();
			this.currentSide = this.coastlineStart.exitSide;

			// walk around the tile and add additional points to the list
			while (this.currentSide != this.coastlineEnd.entrySide || this.needHelperPoint) {
				this.needHelperPoint = false;
				this.additionalCoastlinePoints.add(this.helperPoints[this.currentSide]);
				this.currentSide = (this.currentSide + 1) % 4;
			}

			// check if the start segment is also the end segment
			if (this.coastlineStart == this.coastlineEnd) {
				// calculate the length of the new way
				this.coastlineStartLength = this.coastlineStart.data.length;
				this.coordinates = new float[this.coastlineStartLength
						+ this.additionalCoastlinePoints.size() * 2 + 2];

				// copy the start segment
				System.arraycopy(this.coastlineStart.data, 0, this.coordinates, 0,
						this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					this.coordinates[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints
							.get(i).x;
					this.coordinates[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints
							.get(i).y;
				}

				// close the way
				this.coordinates[this.coordinates.length - 2] = this.coordinates[0];
				this.coordinates[this.coordinates.length - 1] = this.coordinates[1];

				// add the now closed way as a water polygon to the way list
				closedPolygonHandler.onWaterPolygon(this.coordinates);
			} else {
				// calculate the length of the new coastline segment
				this.coastlineStartLength = this.coastlineStart.data.length;
				this.coastlineEndLength = this.coastlineEnd.data.length;
				float[] newSegment = new float[this.coastlineStartLength
						+ this.additionalCoastlinePoints.size() * 2 + this.coastlineEndLength];

				// copy the start segment
				System.arraycopy(this.coastlineStart.data, 0, newSegment, 0,
						this.coastlineStartLength);

				// copy the additional points
				for (int i = 0; i < this.additionalCoastlinePoints.size(); ++i) {
					newSegment[this.coastlineStartLength + 2 * i] = this.additionalCoastlinePoints
							.get(i).x;
					newSegment[this.coastlineStartLength + 2 * i + 1] = this.additionalCoastlinePoints
							.get(i).y;
				}

				// copy the end segment
				System.arraycopy(this.coastlineEnd.data, 0, newSegment,
						this.coastlineStartLength + this.additionalCoastlinePoints.size() * 2,
						this.coastlineEndLength);

				// replace the end segment in the list with the new segment
				this.coastlineWays.remove(this.coastlineEnd);
				newSegment = SutherlandHodgmanClipping.clipPolyline(newSegment,
						this.virtualTileBoundaries);
				if (newSegment != null) {
					this.coastlineWays.add(new CoastlineWay(newSegment,
							this.virtualTileBoundaries, this.virtualTileSize));
					Collections.sort(this.coastlineWays, this.coastlineWayComparator);
				}
			}
		}
	}

	/**
	 * Sets the tiles on which the coastline algorithm should work.
	 * 
	 * @param readCoastlineTile
	 *            the tile whose coastline segments have been read.
	 * @param currentTile
	 *            the tile for which the coastline coordinates are relative to.
	 */
	void setTiles(Tile readCoastlineTile, Tile currentTile) {
		if (readCoastlineTile.zoomLevel < currentTile.zoomLevel) {
			// calculate the virtual tile dimensions
			this.zoomLevelDifference = currentTile.zoomLevel - readCoastlineTile.zoomLevel;
			this.virtualTileSize = Tile.TILE_SIZE << this.zoomLevelDifference;
			this.relativeX1 = (int) ((readCoastlineTile.pixelX << this.zoomLevelDifference) - currentTile.pixelX);
			this.relativeY1 = (int) ((readCoastlineTile.pixelY << this.zoomLevelDifference) - currentTile.pixelY);
			this.relativeX2 = this.relativeX1 + this.virtualTileSize;
			this.relativeY2 = this.relativeY1 + this.virtualTileSize;

			this.virtualTileBoundaries[0] = this.relativeX1;
			this.virtualTileBoundaries[1] = this.relativeY1;
			this.virtualTileBoundaries[2] = this.relativeX2;
			this.virtualTileBoundaries[3] = this.relativeY2;
		} else {
			// use the standard tile dimensions
			this.virtualTileSize = Tile.TILE_SIZE;

			this.virtualTileBoundaries[0] = 0;
			this.virtualTileBoundaries[1] = 0;
			this.virtualTileBoundaries[2] = Tile.TILE_SIZE;
			this.virtualTileBoundaries[3] = Tile.TILE_SIZE;
		}

		// bottom-right
		this.helperPoints[0].x = this.virtualTileBoundaries[2];
		this.helperPoints[0].y = this.virtualTileBoundaries[3];

		// bottom-left
		this.helperPoints[1].x = this.virtualTileBoundaries[0];
		this.helperPoints[1].y = this.virtualTileBoundaries[3];

		// top-left
		this.helperPoints[2].x = this.virtualTileBoundaries[0];
		this.helperPoints[2].y = this.virtualTileBoundaries[1];

		// top-right
		this.helperPoints[3].x = this.virtualTileBoundaries[2];
		this.helperPoints[3].y = this.virtualTileBoundaries[1];
	}
}