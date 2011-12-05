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

import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import org.mapsforge.applications.debug.Serializer;
import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.core.Rect;
import org.mapsforge.storage.atoms.Way;
import org.mapsforge.storage.dataExtraction.MapFileMetaData;
import org.mapsforge.storage.poi.PoiPersistenceManager;
import org.mapsforge.storage.tile.PCTilePersistenceManager;
import org.mapsforge.storage.tile.TilePersistenceManager;

/**
 * This class reads map data atoms such as ways and POIs from the mapsforge tile format. Ways are read
 * from the map file. POIs are read via {@link PoiPersistenceManager}.
 * 
 * @author Karsten Groll
 * 
 */
public class MapDataProviderImpl implements MapDataProvider {

	/** The data tile provider. */
	private TilePersistenceManager tpm = null;
	private MapFileMetaData mfm = null;

	/**
	 * The constructor.
	 * 
	 * @param tpm
	 *            A {@link TilePersistenceManager} for retrieving tiles and meta information about tiles
	 *            and base zoom levels.
	 */
	public MapDataProviderImpl(TilePersistenceManager tpm) {
		this.tpm = tpm;
		this.mfm = this.tpm.getMetaData();
	}

	@Override
	public Collection<Way> getAllWaysInBoundingBox(Rect boundingBox) {
		// System.out.println("Getting all ways in (lat,lon) " + boundingBox);
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
					// System.out.println("extracting " + x + " " + y + " " + z);
					extractAndAddWaysToContainer(tile, ret, z);
				}
			}
		}

		// TODO How to handle PNG tiles?

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
		if (this.mfm.isDebugFlagSet()) {
			// System.out.println(s.getNextString(32));
			s.skip(32);
		}

		// Number of ways / POIs per zoom level
		short[] poisOnZoomLevel = new short[this.mfm.getMaximalZoomLevel()[baseZoomInterval]
				- this.mfm.getMinimalZoomLevel()[baseZoomInterval]
				+ 1];
		short[] waysOnZoomLevel = new short[this.mfm.getMaximalZoomLevel()[baseZoomInterval]
				- this.mfm.getMinimalZoomLevel()[baseZoomInterval]
				+ 1];

		// System.out.println("Covering zoom levels (" +
		// this.mfm.getMinimalZoomLevel()[baseZoomInterval] + ","
		// + this.mfm.getMaximalZoomLevel()[baseZoomInterval] + ")");

		// Zoom table (variable)
		int minZoomLevel = this.mfm.getMinimalZoomLevel()[baseZoomInterval];
		int maxZoomLevel = this.mfm.getMaximalZoomLevel()[baseZoomInterval];
		for (int row = minZoomLevel; row <= maxZoomLevel; row++) {
			poisOnZoomLevel[row - minZoomLevel] = s.getNextShort();
			waysOnZoomLevel[row - minZoomLevel] = s.getNextShort();

			// System.out.println(row + "|" + poisOnZoomLevel[row - minZoomLevel] + "|" +
			// waysOnZoomLevel[row - minZoomLevel]);
		}

		// First way offset (VBE-U)
		int firstWayOffset = s.getNextVBEUInt();
		// System.out.println("First way offset: " + firstWayOffset);
		s.skip(firstWayOffset);

		// Parse all ways
		Way w;
		for (int way = 0; way < waysOnZoomLevel[maxZoomLevel - minZoomLevel]; way++) {
			w = parseNextWay(s);
			// TODO Should duplicates be prevented?
			// if (!container.contains(w)) {
			container.add(w);
			// }
		}
	}

	private Way parseNextWay(Serializer s) {
		long[] wayPoints = null;
		String name = null;

		// Debug tag
		if (this.mfm.isDebugFlagSet()) {
			// System.out.println(s.getNextString(32));
			s.skip(32);
		}

		// Way data size
		int wayDataSize = s.getNextVBEUInt();
		// System.out.println("Way data size: " + wayDataSize);
		// s.skip(wayDataSize);

		// Sub tile bitmap
		s.skip(2);

		// Special byte
		byte specialByte = s.getNextByte();
		// System.out.println("Special Byte: " + specialByte);
		// System.out.println("Amount of tags: " + (specialByte & (byte) 0x0f));
		// Tag IDs
		for (byte pos = 0; pos < (specialByte & (byte) 0x0f); pos++) {
			// TODO create skip method
			// System.out.println("Tag: " + s.getNextVBEUInt());
			s.getNextVBEUInt();
		}

		// Flags
		byte flags = s.getNextByte();
		// System.out.println("Flags: " + flags);
		// System.out.println("Way name flag: " + ((flags & (byte) 0x80) != 0));
		// System.out.println("Reference flag: " + ((flags & (byte) 0x40) != 0));
		// System.out.println("Label position flag: " + ((flags & (byte) 0x20) != 0));
		// System.out.println("Way data blocks flag: " + ((flags & (byte) 0x10) != 0));
		// System.out.println("Double delta flag: " + ((flags & (byte) 0x08) != 0));

		// Way name
		if ((flags & (byte) 0x80) != 0) {
			name = s.getNextString();
			// System.out.println("Name: " + name);
		}

		// Reference
		if ((flags & (byte) 0x40) != 0) {
			// TODO create skip method
			s.getNextString();
		}

		// Label position
		if ((flags & (byte) 0x20) != 0) {
			// TODO create skip method
			s.getNextVBESInt();
			s.getNextVBESInt();
		}

		// Number of way data blocks
		// TODO Does the latest format still ignore this?
		byte numWayDataBlocks = 1;
		// if ((flags & (byte) 0x10) != 0) {
		numWayDataBlocks = s.getNextByte();
		// }

		byte numWayCoordinateBlocks;
		byte wayCoordinateBlock;
		int numWayNodes;
		int wayNode;
		for (byte wayDataBlock = 0; wayDataBlock < numWayDataBlocks; wayDataBlock++) {
			numWayCoordinateBlocks = s.getNextByte();

			// Read inner and outer ways
			for (wayCoordinateBlock = 0; wayCoordinateBlock < numWayCoordinateBlocks; wayCoordinateBlock++) {
				numWayNodes = s.getNextVBEUInt();

				if (wayCoordinateBlock == 0) {
					wayPoints = new long[numWayNodes * 2];
				}

				// Read way points
				for (wayNode = 0; wayNode < numWayNodes; wayNode++) {
					// TODO Handle multipolygons

					// Store values for outer polygon only
					if (wayCoordinateBlock == 0) {
						// TODO Decode values
						wayPoints[wayNode * 2] = s.getNextVBESInt();
						wayPoints[wayNode * 2 + 1] = s.getNextVBESInt();
					} else {
						// TODO create skip method
						// Skip inner polygon waypoints
						s.getNextVBESInt();
						s.getNextVBESInt();
					}
				}

			}
		}

		return new Way(wayPoints, name);
	}

	/**
	 * For debugging purposes only.
	 * 
	 * @param args
	 *            Not used.
	 */
	public static void main(String[] args) {
		TilePersistenceManager tpm = new PCTilePersistenceManager("/home/moep/maps/mapsforge/berlin.map");
		MapDataProvider mdp = new MapDataProviderImpl(tpm);

		// Berlin Mitte
		// 52.523219,13.394523
		// 52.511128,13.42186
		// This should affect 4 tiles on zoom level 14
		Collection<Way> ways = mdp.getAllWaysInBoundingBox(new Rect(13.394523, 13.42186, 52.511128, 52.523219));

		tpm.close();

		for (Way w : ways) {
			if (w.getName() != null) {
				System.out.println(w.getName() + " " + Arrays.toString(w.getCoordinates()));
			}
		}
		System.out.println("Ways found: " + ways.size());

	}

}
