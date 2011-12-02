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
package org.mapsforge.storage;

import java.util.Collection;
import java.util.Vector;

import org.mapsforge.applications.debug.Serializer;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.storage.atoms.Way;
import org.mapsforge.storage.dataExtraction.MapFileMetaData;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.tile.PCTilePersistanceManager;
import org.mapsforge.storage.tile.TilePersistanceManager;

/**
 * This class reads map data atoms such as ways and POIs from the mapsforge tile format. Ways are read
 * from the map file. POIs are read via {@link PoiPersistenceManager}.
 * 
 * @author Karsten Groll
 * 
 */
public class MapDataProviderImpl implements MapDataProvider {

	/** The data tile provider. */
	private TilePersistanceManager tpm = null;
	private MapFileMetaData mfm = null;

	/**
	 * The constructor.
	 * 
	 * @param tpm
	 *            A {@link TilePersistanceManager} for retrieving tiles and meta information about tiles
	 *            and base zoom levels.
	 */
	public MapDataProviderImpl(TilePersistanceManager tpm) {
		this.tpm = tpm;
		this.mfm = this.tpm.getMetaData();
	}

	@Override
	public Collection<Way> getAllWaysInBoundingBox(Rect boundingBox) {
		System.out.println("Getting all ways in (lat,lon) " + boundingBox);
		Vector<Way> ret = new Vector<Way>();

		int minX;
		int minY;
		int maxX;
		int maxY;

		// Get all tiles needed for getting the data
		byte baseZoomLevel;
		for (byte z = 0; z < this.mfm.getAmountOfZoomIntervals(); z++) {
			baseZoomLevel = this.mfm.getBaseZoomLevel()[z];

			minX = (int) MercatorProjection.longitudeToTileX(
					boundingBox.getMinLongitudeE6() / GeoCoordinate.FACTOR_DOUBLE_TO_INT, baseZoomLevel);
			minY = (int) MercatorProjection.latitudeToTileY(boundingBox.getMaxLatitudeE6() / GeoCoordinate.FACTOR_DOUBLE_TO_INT,
					baseZoomLevel);
			maxX = (int) MercatorProjection.longitudeToTileX(
					boundingBox.getMaxLongitudeE6() / GeoCoordinate.FACTOR_DOUBLE_TO_INT, baseZoomLevel);
			maxY = (int) MercatorProjection.latitudeToTileY(boundingBox.getMinLatitudeE6() / GeoCoordinate.FACTOR_DOUBLE_TO_INT,
					baseZoomLevel);

			// System.out.println("Tile interval: [(" + minX + "," + minY + ") .. (" + maxX + "," + maxY
			// + ")]");
			byte[] tile;
			for (int y = minY; y <= maxY; y++) {
				for (int x = minX; x <= maxX; x++) {
					// For each tile: extract ways and ignore duplicates
					tile = this.tpm.getTileData(x, y, z);
					extractAndAddWaysToContainer(tile, ret, z);
				}
			}
		}

		// TODO Don't read PNG tiles.

		return ret;
	}

	/**
	 * Extracts all ways from a given data tile and stores them into a given container. Duplicate ways
	 * are ignored.
	 * 
	 * @param tile
	 *            The data tile containing all data.
	 * @param container
	 *            The container the extracted ways are put in.
	 * @param baseZoomInterval
	 *            The tile's base zoom interval.
	 */
	private void extractAndAddWaysToContainer(final byte[] tile, Collection<Way> container, byte baseZoomInterval) {
		if (tile == null) {
			return;
		}

		Serializer s = new Serializer(tile);

		// Tile signature (32B, optional)
		// ###TileStart
		if (this.mfm.isDebugFlagSet()) {
			System.out.println(s.getNextString(32));
			// s.skip(32);
		}

		// Number of ways / POIs per zoom level
		byte[] poisOnZoomLevel = new byte[this.mfm.getMaximalZoomLevel()[baseZoomInterval]
				- this.mfm.getMinimalZoomLevel()[baseZoomInterval]
				+ 1];
		byte[] waysOnZoomLevel = new byte[this.mfm.getMaximalZoomLevel()[baseZoomInterval]
				- this.mfm.getMinimalZoomLevel()[baseZoomInterval]
				+ 1];

		// Zoom table (variable)
		for (int row = this.mfm.getMinimalZoomLevel()[baseZoomInterval]; row <= this.mfm.getMaximalZoomLevel()[baseZoomInterval]; row++) {
			System.out.println(s.getNextShort() + " " + s.getNextShort());
		}

		// First way offset (VBE-U)
		int firstWayOffset = s.getNextVBEUInt();
		System.out.println("First way offset: " + firstWayOffset);
		s.skip(firstWayOffset);

		// TODO debug only
		System.out.println("Debug tag: " + s.getNextString(32));
	}

	/**
	 * For debugging purposes only.
	 * 
	 * @param args
	 *            Not used.
	 */
	public static void main(String[] args) {
		TilePersistanceManager tpm = new PCTilePersistanceManager("/home/moep/maps/mapsforge/berlin.map");
		MapDataProvider mdp = new MapDataProviderImpl(tpm);

		// Berlin Mitte
		// 52.523219,13.394523
		// 52.511128,13.42186
		// This should affect 4 tiles on zoom level 14
		mdp.getAllWaysInBoundingBox(new Rect(13.394523, 13.42186, 52.511128, 52.523219));

		tpm.close();
	}

}
