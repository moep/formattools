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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

final class TileInfo {

	private static final Logger LOGGER =
			Logger.getLogger(TileInfo.class.getName());

	private static final byte SEA = 0x2;

	static final byte TILE_INFO_ZOOMLEVEL = 0xC;

	private static final byte BITMASK = 0x3;

	// 4096 * 4096 / 4 (2 bits for each tile)
	private static final int N_BYTES = 0x400000;
	// 4096 * 4096 = number of tiles on zoom level 12
	private static final int N_BITS = 0x1000000;

	private final BitSet seaTileInfo = new BitSet(N_BITS);

	private TileInfo(String strInputFile) {
		try {

			DataInputStream dis = new DataInputStream(
					TileInfo.class.getClassLoader().getResourceAsStream(strInputFile));
			byte currentByte;

			long start = System.currentTimeMillis();
			for (int i = 0; i < N_BYTES; i++) {
				currentByte = dis.readByte();
				if (((currentByte >> 6) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4);
				}
				if (((currentByte >> 4) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 1);
				}
				if (((currentByte >> 2) & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 2);
				}
				if ((currentByte & BITMASK) == SEA) {
					seaTileInfo.set(i * 4 + 3);
				}
			}
			LOGGER.fine("loading of tile info data took "
					+ (System.currentTimeMillis() - start)
					+ " ms");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "error loading tile info from file " + strInputFile);
		}
	}

	static TileInfo getInstance() {
		return new TileInfo("org/mapsforge/preprocessing/map/osmosis/oceantiles_12.dat");
	}

	/**
	 * Checks if a tile is completely covered by water. <b>Important notice:</b> The method may
	 * produce false negatives on higher zoom levels than 12.
	 * 
	 * @param tc
	 *            tile given as TileCoordinate
	 * @return true if the tile is completely covered by water, false if the associated tile(s)
	 *         on zoom level 12 is(are) not completely covered by water.
	 */
	boolean isWaterTile(TileCoordinate tc) {
		List<TileCoordinate> tiles = tc.translateToZoomLevel(TILE_INFO_ZOOMLEVEL);
		for (TileCoordinate tile : tiles) {
			if (!seaTileInfo.get(tile.getY() * 4096 + tile.getX()))
				return false;
		}
		return true;
	}
}
