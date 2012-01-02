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
import java.util.List;
import java.util.Set;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.map.osmosis.CoastlineAlgorithm.ClosedPolygonHandler;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;

class CoastlineHandler implements ClosedPolygonHandler {
	private final CoastlineAlgorithm coastlineAlgorithm = new CoastlineAlgorithm();
	private boolean isWaterTile = false;
	private final List<float[]> islandPolygons = new ArrayList<float[]>();

	boolean isWaterTile(TileCoordinate tc, Set<TDWay> coastlines) {

		TileCoordinate coordinateOnTileInfoZoomlevel = tc.translateToZoomLevel(TileInfo.TILE_INFO_ZOOMLEVEL)
				.get(0);
		coastlineAlgorithm.clearCoastlineSegments();
		coastlineAlgorithm.setTiles(new Tile(coordinateOnTileInfoZoomlevel.getX(),
				coordinateOnTileInfoZoomlevel.getY(), coordinateOnTileInfoZoomlevel.getZoomlevel()), new Tile(
				tc.getX(), tc.getY(), tc.getZoomlevel()));

		islandPolygons.clear();
		isWaterTile = false;

		double pixelX = MercatorProjection.tileXToPixelX(tc.getX());
		double pixelY = MercatorProjection.tileYToPixelY(tc.getY());

		float[] segment = null;
		for (TDWay coastline : coastlines) {
			segment = convertToCoastlineSegmentRelativeToTile(pixelX, pixelY, tc.getZoomlevel(), coastline);
			if (segment != null)
				coastlineAlgorithm.addCoastlineSegment(segment);
		}

		coastlineAlgorithm.generateClosedPolygons(this);

		if (isWaterTile) {
			for (float[] islandPolygon : islandPolygons) {
				if (GeoUtils.intersects(islandPolygon, true)) {
					return false;
				}
			}
		}

		return isWaterTile;
	}

	private static float[] convertToCoastlineSegmentRelativeToTile(double tilePixelX, double tilePixelY,
			byte zoom, TDWay coastline) {
		TDNode[] waynodes = coastline.getWayNodes();
		if (waynodes == null || waynodes.length < 2)
			return null;
		float[] segment = new float[waynodes.length * 2];
		int i = 0;
		for (TDNode waynode : waynodes) {
			segment[i] = (float) (MercatorProjection.longitudeToPixelX(
					GeoCoordinate.intToDouble(waynode.getLongitude()), zoom) - tilePixelX);
			segment[i + 1] = (float) (MercatorProjection.latitudeToPixelY(
					GeoCoordinate.intToDouble(waynode.getLatitude()), zoom) - tilePixelY);
			i += 2;
		}

		return segment;
	}

	@Override
	public void onInvalidCoastlineSegment(float[] coastline) {
		// System.out.println("onInvalid");
		// nothing to do here

	}

	@Override
	public void onIslandPolygon(float[] coastline) {
		islandPolygons.add(coastline);
	}

	@Override
	public void onWaterPolygon(float[] coastline) {
		if (GeoUtils.covers(coastline, true))
			isWaterTile = true;
	}

	@Override
	public void onWaterTile() {
		isWaterTile = true;

	}

	@Override
	public void onValidCoastlineSegment(float[] coastline) {
		// nothing to do here
	}

}