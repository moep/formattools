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

import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.TopologyException;
import com.vividsolutions.jts.simplify.TopologyPreservingSimplifier;

/**
 * Provides utility functions for the maps preprocessing.
 * 
 * @author bross
 */
final class GeoUtils {
	private static final double DOUGLAS_PEUCKER_SIMPLIFICATION_TOLERANCE = 0.0000188;
	// private static final double DOUGLAS_PEUCKER_SIMPLIFICATION_TOLERANCE = 0.00003;
	static final int MIN_NODES_POLYGON = 4;
	static final int MIN_COORDINATES_POLYGON = 8;
	private static final byte SUBTILE_ZOOMLEVEL_DIFFERENCE = 2;
	private static final double[] EPSILON_ZERO = new double[] { 0, 0 };
	private static final Logger LOGGER = Logger.getLogger(GeoUtils.class.getName());

	private static final int[] TILE_BITMASK_VALUES = new int[] { 32768, 16384, 8192, 4096, 2048, 1024, 512,
			256, 128, 64, 32, 16, 8, 4, 2, 1 };

	// JTS
	private static final GeometryFactory gf = new GeometryFactory();

	// **************** WAY OR POI IN TILE *****************

	/**
	 * Computes which tiles on the given base zoom level need to include the given way (which may be a polygon).
	 * 
	 * @param way
	 *            the way that is mapped to tiles
	 * @param baseZoomLevel
	 *            the base zoom level which is used in the mapping
	 * @param enlargementInMeter
	 *            amount of pixels that is used to enlarge the bounding box of the way and the tiles in the
	 *            mapping process
	 * @return all tiles on the given base zoom level that need to include the given way, an empty set if no
	 *         tiles are matched
	 */
	static Set<TileCoordinate> mapWayToTiles(final TDWay way, final byte baseZoomLevel,
			final int enlargementInMeter) {
		if (way == null)
			throw new IllegalArgumentException("parameter way is null");

		HashSet<TileCoordinate> matchedTiles = new HashSet<TileCoordinate>();
		double[] waynodes = way.wayNodesAsArray();
		if (waynodes == null) {
			return matchedTiles;
		}
		// check for valid closed polygon
		if (way.isPolygon() && waynodes.length < MIN_COORDINATES_POLYGON) {
			LOGGER.finer("found closed polygon with fewer than 4 nodes, ignoring this way, way-id: "
					+ way.getId());
			return matchedTiles;
		}

		TileCoordinate[] bbox = getWayBoundingBox(way, baseZoomLevel, enlargementInMeter);
		// calculate the tile coordinates and the corresponding bounding boxes
		for (int k = bbox[0].getX(); k <= bbox[1].getX(); k++) {
			for (int l = bbox[0].getY(); l <= bbox[1].getY(); l++) {
				double[] currentBBox = getBoundingBoxAsArray(k, l, baseZoomLevel, enlargementInMeter);
				if (!way.isPolygon()) {
					if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				} else {
					if (CohenSutherlandClipping.intersectsClippingRegion(waynodes, currentBBox)
							|| SutherlandHodgmanClipping.accept(waynodes, currentBBox, false)) {
						matchedTiles.add(new TileCoordinate(k, l, baseZoomLevel));
					}
				}
			}
		}

		return matchedTiles;
	}

	static boolean pointInTile(GeoCoordinate point, TileCoordinate tile) {
		if (point == null || tile == null)
			return false;

		int lon1 = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(tile.getX(),
				tile.getZoomlevel()));
		int lon2 = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(tile.getX() + 1,
				tile.getZoomlevel()));
		int lat1 = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(tile.getY(),
				tile.getZoomlevel()));
		int lat2 = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(tile.getY() + 1,
				tile.getZoomlevel()));
		return point.getLatitudeE6() <= lat1 && point.getLatitudeE6() >= lat2 && point.getLongitudeE6() >= lon1
				&& point.getLongitudeE6() <= lon2;
	}

	// *********** PREPROCESSING OF WAYS **************

	static Geometry preprocessWay(final TDWay way, final List<TDWay> innerWays, boolean clipPolygons,
			boolean clipWays, boolean simplify, final TileCoordinate tile, int enlargementInMeters) {

		Geometry geometry = toJtsGeometry(way, innerWays);
		if (geometry == null) {
			return null;
		}

		// clip geometry?
		Geometry tileBBJTS = null;
		if ((geometry instanceof Polygon || geometry instanceof LinearRing) && clipPolygons
				|| geometry instanceof LineString && clipWays) {

			// create tile bounding box
			tileBBJTS = tileToJTSGeometry(tile.getX(), tile.getY(), tile.getZoomlevel(), enlargementInMeters);

			// clip the polygon/ring by intersection with the bounding box of the tile
			// may throw a TopologyException
			try {
				// geometry = OverlayOp.overlayOp(tileBBJTS, geometry, OverlayOp.INTERSECTION);
				geometry = tileBBJTS.intersection(geometry);
			} catch (TopologyException e) {
				LOGGER.log(Level.FINE, "JTS cannot clip outer way: " + way.getId(), e);
				return null;
			}

		}

		if (simplify) {
			// TODO is this the right place to simplify, is better after clipping?
			// tolerate up to 2 meters
			geometry = TopologyPreservingSimplifier
					.simplify(geometry, DOUGLAS_PEUCKER_SIMPLIFICATION_TOLERANCE);
		}

		return geometry;
	}

	/**
	 * A tile on zoom level <i>z</i> has exactly 16 sub tiles on zoom level <i>z+2</i>. For each of these 16 sub
	 * tiles it is analyzed if the given way needs to be included. The result is represented as a 16 bit short
	 * value. Each bit represents one of the 16 sub tiles. A bit is set to 1 if the sub tile needs to include
	 * the way. Representation is row-wise.
	 * 
	 * @param geometry
	 *            the geometry which is analyzed
	 * @param tile
	 *            the tile which is split into 16 sub tiles
	 * @param enlargementInMeter
	 *            amount of pixels that is used to enlarge the bounding box of the way and the tiles in the
	 *            mapping process
	 * @return a 16 bit short value that represents the information which of the sub tiles needs to include the
	 *         way
	 */
	static short computeBitmask(final Geometry geometry, final TileCoordinate tile, final int enlargementInMeter) {
		List<TileCoordinate> subtiles = tile
				.translateToZoomLevel((byte) (tile.getZoomlevel() + SUBTILE_ZOOMLEVEL_DIFFERENCE));

		short bitmask = 0;
		int tileCounter = 0;
		for (TileCoordinate subtile : subtiles) {
			Geometry bbox = tileToJTSGeometry(subtile.getX(), subtile.getY(), subtile.getZoomlevel(),
					enlargementInMeter);
			if (bbox.intersects(geometry)) {
				bitmask |= TILE_BITMASK_VALUES[tileCounter];
			}
			tileCounter++;
		}
		return bitmask;
	}

	static boolean coveredByTile(final Geometry geometry, final TileCoordinate tile,
			final int enlargementInMeter) {
		Geometry bbox = tileToJTSGeometry(tile.getX(), tile.getY(), tile.getZoomlevel(), enlargementInMeter);
		if (bbox.covers(geometry)) {
			return true;
		}

		return false;
	}

	static GeoCoordinate computeCentroid(Geometry geometry) {
		Point centroid = geometry.getCentroid();
		if (centroid != null)
			return new GeoCoordinate(centroid.getCoordinate().y, centroid.getCoordinate().x);

		return null;
	}

	/**
	 * Convert a JTS Geometry to a WayDataBlock list.
	 * 
	 * @param geometry
	 *            a geometry object which should be converted
	 * @return a list of WayBlocks which you can use to save the way.
	 */
	static List<WayDataBlock> toWayDataBlockList(Geometry geometry) {
		List<WayDataBlock> res = new ArrayList<WayDataBlock>();
		if (geometry instanceof MultiPolygon) {
			MultiPolygon mp = (MultiPolygon) geometry;
			for (int i = 0; i < mp.getNumGeometries(); i++) {
				Polygon p = (Polygon) mp.getGeometryN(i);
				List<Integer> outer = toCoordinateList(p.getExteriorRing());
				List<List<Integer>> inner = new ArrayList<List<Integer>>();
				for (int j = 0; j < p.getNumInteriorRing(); j++) {
					inner.add(toCoordinateList(p.getInteriorRingN(j)));
				}
				res.add(new WayDataBlock(outer, inner));
			}
		} else if (geometry instanceof Polygon) {
			Polygon p = (Polygon) geometry;
			List<Integer> outer = toCoordinateList(p.getExteriorRing());
			List<List<Integer>> inner = new ArrayList<List<Integer>>();
			for (int i = 0; i < p.getNumInteriorRing(); i++) {
				inner.add(toCoordinateList(p.getInteriorRingN(i)));
			}
			res.add(new WayDataBlock(outer, inner));
		} else if (geometry instanceof MultiLineString) {
			MultiLineString ml = (MultiLineString) geometry;
			for (int i = 0; i < ml.getNumGeometries(); i++) {
				LineString l = (LineString) ml.getGeometryN(i);
				res.add(new WayDataBlock(toCoordinateList(l), null));
			}
		} else if (geometry instanceof LinearRing || geometry instanceof LineString) {
			res.add(new WayDataBlock(toCoordinateList(geometry), null));
		} else if (geometry instanceof GeometryCollection) {
			GeometryCollection gc = (GeometryCollection) geometry;
			for (int i = 0; i < gc.getNumGeometries(); i++) {
				List<WayDataBlock> recursiveResult = toWayDataBlockList(gc.getGeometryN(i));
				for (WayDataBlock wayDataBlock : recursiveResult) {
					res.add(wayDataBlock);
				}
			}
		}

		return res;
	}

	// **************** JTS CONVERSIONS *********************

	private static Geometry toJtsGeometry(TDWay way, List<TDWay> innerWays) {

		Geometry wayGeometry = toJTSGeometry(way, true);
		if (wayGeometry == null)
			return null;

		if (innerWays != null) {
			List<LinearRing> innerWayGeometries = new ArrayList<LinearRing>();
			if (!(wayGeometry instanceof Polygon)) {
				LOGGER.warning("outer way of multi polygon is not a polygon, skipping it: " + way.getId());
				return null;
			}
			Polygon outerPolygon = (Polygon) wayGeometry;

			for (TDWay innerWay : innerWays) {
				// in order to build the polygon with holes, we want to create
				// linear rings of the inner ways
				Geometry innerWayGeometry = toJTSGeometry(innerWay, false);
				if (innerWayGeometry == null)
					continue;

				if (!(innerWayGeometry instanceof LinearRing)) {
					LOGGER.warning("inner way of multi polygon is not a polygon, skipping it, inner id: "
							+ innerWay.getId() + ", outer id: " + way.getId());
					continue;
				}

				LinearRing innerRing = (LinearRing) innerWayGeometry;

				// check if inner way is completely contained in outer way
				if (outerPolygon.covers(innerRing)) {
					innerWayGeometries.add(innerRing);
				} else {
					LOGGER.warning("inner way is not contained in outer way, skipping inner way, inner id: "
							+ innerWay.getId() + ", outer id: " + way.getId());
				}
			}

			if (!innerWayGeometries.isEmpty()) {
				// make wayGeometry a new Polygon that contains inner ways as holes
				LinearRing[] holes = innerWayGeometries.toArray(new LinearRing[innerWayGeometries.size()]);
				LinearRing exterior = gf.createLinearRing(outerPolygon.getExteriorRing().getCoordinates());
				wayGeometry = new Polygon(exterior, holes, gf);
			}

		}

		return wayGeometry;
	}

	/**
	 * Internal conversion method to convert our internal data structure for ways to geometry objects in JTS. It
	 * will care about ways and polygons and will create the right JTS onjects.
	 * 
	 * @param way
	 *            TDway which will be converted. Null if we were not able to convert the way to a Geometry
	 *            object.
	 * @param area
	 *            true, if the way represents an area, i.e. a polygon instead of a linear ring
	 * @return return Converted way as JTS object.
	 */
	private static Geometry toJTSGeometry(TDWay way, boolean area) {
		if (way.getWayNodes().length < 2) {
			LOGGER.fine("way has fewer than 2 nodes: " + way.getId());
			return null;
		}

		Coordinate[] coordinates = new Coordinate[way.getWayNodes().length];
		for (int i = 0; i < coordinates.length; i++) {
			TDNode currentNode = way.getWayNodes()[i];
			coordinates[i] = new Coordinate(GeoCoordinate.intToDouble(currentNode.getLongitude()),
					GeoCoordinate.intToDouble(currentNode.getLatitude()));
		}

		Geometry res = null;

		try {
			// check for closed polygon
			if (way.isPolygon()) {
				if (area)
					// polygon
					res = gf.createPolygon(gf.createLinearRing(coordinates), null);
				else
					// linear ring
					res = gf.createLinearRing(coordinates);
			} else
				res = gf.createLineString(coordinates);
		} catch (TopologyException e) {
			LOGGER.log(Level.FINE, "error creating JTS geometry from way: " + way.getId(), e);
			return null;
		}
		return res;
	}

	private static List<Integer> toCoordinateList(Geometry jtsGeometry) {

		Coordinate[] jtsCoords = jtsGeometry.getCoordinates();

		ArrayList<Integer> result = new ArrayList<Integer>();

		for (int j = 0; j < jtsCoords.length; j++) {
			GeoCoordinate geoCoord = new GeoCoordinate(jtsCoords[j].y, jtsCoords[j].x);
			result.add(Integer.valueOf(geoCoord.getLatitudeE6()));
			result.add(Integer.valueOf(geoCoord.getLongitudeE6()));
		}

		return result;

	}

	private static double[] computeTileEnlargement(long tileY, byte zoom, int enlargementInMeter) {
		if (enlargementInMeter == 0)
			return EPSILON_ZERO;

		double[] epsilons = new double[2];
		double lat = MercatorProjection.tileYToLatitude(tileY, zoom);
		epsilons[0] = GeoCoordinate.latitudeDistance(enlargementInMeter);
		epsilons[1] = GeoCoordinate.longitudeDistance(enlargementInMeter, lat);

		return epsilons;
	}

	private static double[] computeTileEnlargement(double lat, int enlargementInPixel) {

		if (enlargementInPixel == 0)
			return EPSILON_ZERO;

		double[] epsilons = new double[2];

		epsilons[0] = GeoCoordinate.latitudeDistance(enlargementInPixel);
		epsilons[1] = GeoCoordinate.longitudeDistance(enlargementInPixel, lat);

		return epsilons;
	}

	private static double[] bufferInDegrees(long tileY, byte zoom, int enlargementInMeter) {
		if (enlargementInMeter == 0)
			return EPSILON_ZERO;

		double[] epsilons = new double[2];
		double lat = MercatorProjection.tileYToLatitude(tileY, zoom);
		epsilons[0] = GeoCoordinate.latitudeDistance(enlargementInMeter);
		epsilons[1] = GeoCoordinate.longitudeDistance(enlargementInMeter, lat);

		return epsilons;
	}

	private static Geometry tileToJTSGeometry(long tileX, long tileY, byte zoom, int enlargementInMeter) {
		double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		double[] epsilons = bufferInDegrees(tileY, zoom, enlargementInMeter);

		minLon -= epsilons[1];
		minLat -= epsilons[0];
		maxLon += epsilons[1];
		maxLat += epsilons[0];

		Coordinate bottomLeft = new Coordinate(minLon, minLat);
		Coordinate topRight = new Coordinate(maxLon, maxLat);

		return gf.createLineString(new Coordinate[] { bottomLeft, topRight }).getEnvelope();
	}

	static boolean intersects(float[] polygon, boolean coordinateSystemUpperLeft) {
		if (polygon == null) {
			throw new IllegalArgumentException("polygon is null");
		}

		if (polygon.length < MIN_COORDINATES_POLYGON)
			throw new IllegalArgumentException("a valid closed polygon must have at least 4 points");

		double[] polygonAsDoubleArray = new double[polygon.length];
		for (int i = 0; i < polygon.length; i++) {
			polygonAsDoubleArray[i] = polygon[i];
		}

		// double[] bbox = new double[] { 0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE,
		// Tile.TILE_SIZE, 0, 0, 0, 0, Tile.TILE_SIZE };
		double[] bbox = new double[] { 0, Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };
		double[] clippedPolygon = SutherlandHodgmanClipping.clipPolygon(polygonAsDoubleArray, bbox,
				coordinateSystemUpperLeft);

		if (clippedPolygon == null || clippedPolygon.length == 0
				|| clippedPolygon.length <= MIN_COORDINATES_POLYGON) {
			return false;
		}

		// double[] bboxWay = new double[] { 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE,
		// Tile.TILE_SIZE, 0, Tile.TILE_SIZE, 0, 0 };
		// return fuzzyMatch(bboxWay, clippedPolygon, 0.1d);
		return true;

	}

	private static double[] getBoundingBoxAsArray(long tileX, long tileY, byte zoom, int enlargementInMeter) {
		double minLat = MercatorProjection.tileYToLatitude(tileY + 1, zoom);
		double maxLat = MercatorProjection.tileYToLatitude(tileY, zoom);
		double minLon = MercatorProjection.tileXToLongitude(tileX, zoom);
		double maxLon = MercatorProjection.tileXToLongitude(tileX + 1, zoom);

		double[] epsilons = computeTileEnlargement(tileY, zoom, enlargementInMeter);

		return new double[] { minLon - epsilons[1], minLat - epsilons[0], maxLon + epsilons[1],
				maxLat + epsilons[0] };
	}

	private static TileCoordinate[] getWayBoundingBox(final TDWay way, byte zoomlevel, int enlargementInPixel) {
		double maxx = Double.NEGATIVE_INFINITY, maxy = Double.NEGATIVE_INFINITY, minx = Double.POSITIVE_INFINITY, miny = Double.POSITIVE_INFINITY;
		for (TDNode coordinate : way.getWayNodes()) {
			maxy = Math.max(maxy, GeoCoordinate.intToDouble(coordinate.getLatitude()));
			miny = Math.min(miny, GeoCoordinate.intToDouble(coordinate.getLatitude()));
			maxx = Math.max(maxx, GeoCoordinate.intToDouble(coordinate.getLongitude()));
			minx = Math.min(minx, GeoCoordinate.intToDouble(coordinate.getLongitude()));
		}

		double[] epsilonsTopLeft = computeTileEnlargement(maxy, enlargementInPixel);
		double[] epsilonsBottomRight = computeTileEnlargement(miny, enlargementInPixel);

		TileCoordinate[] bbox = new TileCoordinate[2];
		bbox[0] = new TileCoordinate((int) MercatorProjection.longitudeToTileX(minx - epsilonsTopLeft[1],
				zoomlevel), (int) MercatorProjection.latitudeToTileY(maxy + epsilonsTopLeft[0], zoomlevel),
				zoomlevel);
		bbox[1] = new TileCoordinate((int) MercatorProjection.longitudeToTileX(maxx + epsilonsBottomRight[1],
				zoomlevel), (int) MercatorProjection.latitudeToTileY(miny - epsilonsBottomRight[0], zoomlevel),
				zoomlevel);

		return bbox;
	}

	static boolean covers(float[] polygon, boolean coordinateSystemOriginUpperLeft) {
		if (polygon == null) {
			throw new IllegalArgumentException("polygon is null");
		}

		if (polygon.length < MIN_COORDINATES_POLYGON)
			throw new IllegalArgumentException("a valid closed polygon must have at least 4 points");

		double[] polygonAsDoubleArray = new double[polygon.length];
		for (int i = 0; i < polygon.length; i++) {
			polygonAsDoubleArray[i] = polygon[i];
		}

		double[] bbox = new double[] { 0, Tile.TILE_SIZE, Tile.TILE_SIZE, 0 };
		double[] clippedPolygon = SutherlandHodgmanClipping.clipPolygon(polygonAsDoubleArray, bbox,
				coordinateSystemOriginUpperLeft);

		if (clippedPolygon == null || clippedPolygon.length == 0
				|| clippedPolygon.length <= MIN_COORDINATES_POLYGON) {
			return false;
		}

		double[] bboxWay = new double[] { 0, 0, Tile.TILE_SIZE, 0, Tile.TILE_SIZE, Tile.TILE_SIZE, 0,
				Tile.TILE_SIZE, 0, 0 };
		return fuzzyMatch(bboxWay, clippedPolygon, 0.1d);

	}

	private static boolean fuzzyMatch(double[] d1, double[] d2, double epsilon) {
		if (d1.length != d2.length)
			return false;

		for (int i = 0; i < d1.length - 1; i += 2) {
			boolean matched = false;
			for (int j = 0; j < d2.length - 1; j += 2) {
				if (Math.abs(d1[i] - d2[j]) <= epsilon && Math.abs(d1[i + 1] - d2[j + 1]) <= epsilon) {
					matched = true;
				}
			}
			if (!matched)
				return false;
		}

		return true;
	}

	static String toGPX(final List<GeoCoordinate> g) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>").append("\n");
		sb.append(
				"<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"byHand\" "
						+ "version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
						+ "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 "
						+ "http://www.topografix.com/GPX/1/1/gpx.xsd\">").append("\n");
		for (GeoCoordinate c : g) {
			sb.append("\t<wpt ").append("lat=\"").append(c.getLatitude()).append("\" ");
			sb.append("lon=\"").append(c.getLongitude()).append("\"/>");
			sb.append("\n");
		}
		sb.append("</gpx>");

		return sb.toString();
	}

	static String arraysSVG(List<float[]> closedPolygons) {
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>").append("\n");
		sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" "
				+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
				+ "xmlns:ev=\"http://www.w3.org/2001/xml-events\" "
				+ "version=\"1.1\" baseProfile=\"full\" width=\"800mm\" height=\"600mm\">");

		for (float[] fs : closedPolygons) {
			sb.append("<polygon points=\"");
			for (float f : fs) {
				sb.append(f).append(" ");
			}
			sb.append("\" />");
		}

		sb.append("</svg>");

		return sb.toString();
	}

	/**
	 * Implements the Sutherland-Hodgman algorithm for clipping a polygon.
	 * 
	 * @author bross
	 */
	static class SutherlandHodgmanClipping {

		/**
		 * Check whether the polygon "touches" the clipping region (that includes "covers"), or whether the
		 * polygon is completely outside of the clipping region.
		 * 
		 * @param polygon
		 *            A closed polygon as a double array in the form [x1,y1,x2,y2,x3,y3,...,x1,y1]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin, ymin, xmax, ymax]
		 * @param coordinateSystemOriginUpperLeft
		 *            set flag if the origin of the coordinate system is in the upper left, otherwise the origin
		 *            is assumed to be in the bottom left
		 * @return true if the polygon "touches" (includes "covers") the clipping region, false otherwise
		 */
		static boolean accept(final double[] polygon, final double[] rectangle,
				boolean coordinateSystemOriginUpperLeft) {
			double[] clipped = clipPolygon(polygon, rectangle, coordinateSystemOriginUpperLeft);
			return clipped != null && clipped.length > 0;
		}

		/**
		 * Clips a closed polygon to a rectangular clipping region.
		 * 
		 * @param polygon
		 *            A closed polygon as a double array in the form [x1,y1,x2,y2,x3,y3,...,x1,y1]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin, ymin, xmax, ymax]
		 * @param coordinateSystemOriginUpperLeft
		 *            set flag if the origin of the coordinate system is in the upper left, otherwise the origin
		 *            is assumed to be in the bottom left
		 * @return the clipped polygon if the polygon "touches" the clipping region, an empty array otherwise
		 */
		static double[] clipPolygon(final double[] polygon, final double[] rectangle,
				boolean coordinateSystemOriginUpperLeft) {
			if (polygon == null) {
				throw new IllegalArgumentException("polygon is null");
			}

			if (polygon.length < MIN_COORDINATES_POLYGON)
				throw new IllegalArgumentException("a valid closed polygon must have at least 4 points");

			// bottom edge
			double[] clippedPolygon = clipPolygonToEdge(polygon, new double[] { rectangle[0], rectangle[1],
					rectangle[2], rectangle[1] }, coordinateSystemOriginUpperLeft);
			// right edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[2], rectangle[1],
					rectangle[2], rectangle[3] }, coordinateSystemOriginUpperLeft);
			// top edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[2], rectangle[3],
					rectangle[0], rectangle[3] }, coordinateSystemOriginUpperLeft);
			// left edge
			clippedPolygon = clipPolygonToEdge(clippedPolygon, new double[] { rectangle[0], rectangle[3],
					rectangle[0], rectangle[1] }, coordinateSystemOriginUpperLeft);

			return clippedPolygon;
		}

		private static boolean inside(double x, double y, double[] edge, boolean coordinateSystemOriginUpperLeft) {

			if (edge[0] < edge[2]) {
				// bottom edge
				return coordinateSystemOriginUpperLeft ? y <= edge[1] : y >= edge[1];
			} else if (edge[0] > edge[2]) {
				// top edge
				return coordinateSystemOriginUpperLeft ? y >= edge[1] : y <= edge[1];
			} else if (edge[1] < edge[3]) {
				// right edge if !coordinateSystemOriginUpperLeft, left edge otherwise
				return coordinateSystemOriginUpperLeft ? x >= edge[0] : x <= edge[0];
			} else if (edge[1] > edge[3]) {
				// left edge if !coordinateSystemOriginUpperLeft, right edge otherwise
				return coordinateSystemOriginUpperLeft ? x <= edge[0] : x >= edge[0];
			} else
				throw new IllegalArgumentException();
		}

		private static double[] clipPolygonToEdge(final double[] polygon, double[] edge,
				boolean coordinateSystemOriginUpperLeft) {
			TDoubleArrayList clippedPolygon = new TDoubleArrayList();

			if (polygon.length < MIN_COORDINATES_POLYGON)
				return polygon;

			// polygon not closed
			if (polygon[0] != polygon[polygon.length - 2] || polygon[1] != polygon[polygon.length - 1]) {
				throw new IllegalArgumentException("polygon must be closed");
			}

			double x1, y1, x2, y2;
			boolean startPointInside = false, endPointInside = false;
			for (int i = 0; i < polygon.length - 2; i += 2) {
				// line starts with previous point
				x1 = polygon[i];
				y1 = polygon[i + 1];
				x2 = polygon[i + 2];
				y2 = polygon[i + 3];
				startPointInside = inside(x1, y1, edge, coordinateSystemOriginUpperLeft);
				endPointInside = inside(x2, y2, edge, coordinateSystemOriginUpperLeft);
				if (startPointInside) {
					if (endPointInside) {
						clippedPolygon.add(x2);
						clippedPolygon.add(y2);
					} else {
						double[] intersection = computeIntersection(edge, x1, y1, x2, y2);
						clippedPolygon.add(intersection);
					}
				} else if (endPointInside) {
					double[] intersection = computeIntersection(edge, x1, y1, x2, y2);
					clippedPolygon.add(intersection);
					clippedPolygon.add(x2);
					clippedPolygon.add(y2);
				}
			}

			// if clipped polygon is not closed, add the start point to the end
			if (clippedPolygon.size() > 0
					&& (clippedPolygon.get(0) != clippedPolygon.get(clippedPolygon.size() - 2) || clippedPolygon
							.get(1) != clippedPolygon.get(clippedPolygon.size() - 1))) {
				clippedPolygon.add(clippedPolygon.get(0));
				clippedPolygon.add(clippedPolygon.get(1));
			}

			return clippedPolygon.toArray();
		}

		private static double[] computeIntersection(double[] edge, double x1, double y1, double x2, double y2) {
			double[] ret = new double[2];

			if (edge[1] == edge[3]) {
				// horizontal edge
				ret[1] = edge[1];
				ret[0] = x1 + (edge[1] - y1) * ((x2 - x1) / (y2 - y1));

			} else {
				// vertical edge
				ret[1] = y1 + (edge[0] - x1) * ((y2 - y1) / (x2 - x1));
				ret[0] = edge[0];
			}
			return ret;

		}
	}

	/**
	 * Clips a line to a clipping region using the Cohen-Sutherland algorithm.
	 * 
	 * @author bross
	 */
	static class CohenSutherlandClipping {

		private static final int MIN_COORDINATES_LINE = 4;
		private static final byte INSIDE = 0;
		private static final byte LEFT = 1;
		private static final byte RIGHT = 2;
		private static final byte BOTTOM = 4;
		private static final byte TOP = 8;

		/**
		 * Checks whether a given line intersects the given clipping region.
		 * 
		 * @param line
		 *            A line as a double array in the form [x1,y1,x2,y2,x3,y3,...,xn,yn]
		 * @param rectangle
		 *            A rectangle defined by the bottom/left and top/right point, i.e. [xmin, ymin, xmax, ymax]
		 * @return true if any line segments intersects the clipping region, false otherwise
		 */
		static boolean intersectsClippingRegion(final double[] line, final double[] rectangle) {
			if (line.length < MIN_COORDINATES_LINE)
				throw new IllegalArgumentException("line must have at least 2 points");
			if (rectangle.length != 4)
				throw new IllegalArgumentException("clipping rectangle must be defined by exactly 2 points");

			double[] clippedSegment;
			for (int i = 0; i < line.length - 2; i += 2) {
				clippedSegment = clipLine(line[i], line[i + 1], line[i + 2], line[i + 3], rectangle);
				if (clippedSegment != null)
					return true;
			}

			return false;
		}

		private static byte outCode(double x, double y, double xMin, double xMax, double yMin, double yMax) {
			byte outcode = INSIDE;
			if (x < xMin)
				outcode |= LEFT;
			else if (x > xMax)
				outcode |= RIGHT;
			if (y < yMin)
				outcode |= BOTTOM;
			else if (y > yMax)
				outcode |= TOP;

			return outcode;
		}

		private static double[] clipLine(double x1, double y1, double x2, double y2, double[] rectangle) {
			double x1Copy = x1, y1Copy = y1, x2Copy = x2, y2Copy = y2;

			byte outcode1 = outCode(x1Copy, y1Copy, rectangle[0], rectangle[2], rectangle[1], rectangle[3]);
			byte outcode2 = outCode(x2Copy, y2Copy, rectangle[0], rectangle[2], rectangle[1], rectangle[3]);

			while (true) {

				if ((outcode1 | outcode2) == 0) {
					// both are inside
					return new double[] { x1Copy, y1Copy, x2Copy, y2Copy };
				} else if ((outcode1 & outcode2) != 0) {
					// both are outside and in the same region
					return null;
				} else {
					// at least one is outside
					byte outcodeOut = outcode1 > 0 ? outcode1 : outcode2;
					double x = 0, y = 0;
					// Now find the intersection point;
					// use formulas y = y0 + slope * (x - x0), x = x0 + (1 / slope) * (y - y0)
					if ((outcodeOut & TOP) != 0) { // point is above the clip rectangle
						x = x1Copy + (x2Copy - x1Copy) * (rectangle[3] - y1Copy) / (y2Copy - y1Copy);
						y = rectangle[3];
					} else if ((outcodeOut & BOTTOM) != 0) { // point is below the clip
						// rectangle
						x = x1Copy + (x2Copy - x1Copy) * (rectangle[1] - y1Copy) / (y2Copy - y1Copy);
						y = rectangle[1];
					} else if ((outcodeOut & RIGHT) != 0) { // point is to the right of clip
						// rectangle
						y = y1Copy + (y2Copy - y1Copy) * (rectangle[2] - x1Copy) / (x2Copy - x1Copy);
						x = rectangle[2];
					} else if ((outcodeOut & LEFT) != 0) { // point is to the left of clip
						// rectangle
						y = y1Copy + (y2Copy - y1Copy) * (rectangle[0] - x1Copy) / (x2Copy - x1Copy);
						x = rectangle[0];
					}

					// Now we move outside point to intersection point to clip
					// and get ready for next pass.
					if (outcodeOut == outcode1) {
						x1Copy = x;
						y1Copy = y;
						outcode1 = outCode(x1Copy, y1Copy, rectangle[0], rectangle[2], rectangle[1],
								rectangle[3]);
					} else {
						x2Copy = x;
						y2Copy = y;
						outcode2 = outCode(x2Copy, y2Copy, rectangle[0], rectangle[2], rectangle[1],
								rectangle[3]);
					}
				}
			}
		}
	}

}
