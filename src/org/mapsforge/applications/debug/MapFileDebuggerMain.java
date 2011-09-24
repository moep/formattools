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
package org.mapsforge.applications.debug;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.mapsforge.applications.debug.db.TileSizeSQLiteWriter;

/**
 * 
 * @author Karsten Groll
 * 
 *         Main class for executing various tests helping understanding the map file format.
 */

public class MapFileDebuggerMain {
	private static String getBaseName(String path) {
		return new File(path).getName().split("\\.(?=[^\\.]+$)")[0];
	}

	private static void writeTileSizesToDB(String path, byte zoomInterval) {
		System.out.println("Writing information (x, y, tileSize) to 'db/" + getBaseName(path)
				+ "-bzs" + zoomInterval + "-tileSizes.sqlite'...");

		System.out.println("Parsing " + path + " ...");
		SimpleTileExtractor ste = null;
		TileSizeSQLiteWriter db = new TileSizeSQLiteWriter("db/" + getBaseName(path) + "-bzs"
				+ zoomInterval + "-tileSizes.sqlite");
		byte[] tile;

		// Parse tiles
		int tilesWritten = 0;
		try {
			ste = new SimpleTileExtractor(path);
			int i = 0;
			for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
				for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
					tile = ste.getTile(x, y, zoomInterval);

					// Write non-empty tiles to db
					if (tile != null) {
						db.insertData(tile, x, y);
						++tilesWritten;
						if (tilesWritten % 1000 == 0) {
							db.commit();
						}
					}
					++i;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			System.err.println(e.getMessage());
		}

		db.commit();
		db.close();

		System.out.println(tilesWritten + " tiles have been written to db.");

	}

	private static void checkIdexes(String path) {
		SimpleTileExtractor ste = null;
		byte[][] tiles;
		tiles = new byte[200000][];

		// Test if tile coordinates are correct (NEEDS DEBUG FILE!)
		try {
			ste = new SimpleTileExtractor(path);
			// Expected and given signature
			String expected;
			String given;
			int i = 0;
			for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						tiles[i] = ste.getTile(x, y, zoomInterval);
						if (tiles[i] == null)
							continue;

						expected = "###TileStart" + x + "," + y + "###";
						given = new String(tiles[i], 0, 32);
						if (!given.startsWith(expected)) {
							System.out.println("ERROR @ (" + x + ", " + y + ") given: " + given
									+ " expected: " + expected);
						}
					}
				}
			}

			// Parse a single tile from RAM
			byte[] rawTile = ste.getTile(8801, 5373, (byte) 1);
			TileFactory.getTileFromRawData(rawTile, (byte) 1, ste.getMapFile());
			// t.getWays();
			// t.getPois();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @param args
	 *            not used command line parameters.
	 */
	public static void main(String[] args) {
		// writeTileSizesToDB("/home/moep/libya.map", (byte) 0);
		// writeTileSizesToDB("/home/moep/libya.map", (byte) 1);
		System.out.println("java.library.path: " + System.getProperty("java.library.path"));
		Connection conn = null;
		SQLite.Database db = null;
		try {
			Class.forName("SQLite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite://home/moep/test.sqlite");
			Statement stmt = conn.createStatement();
			stmt.executeUpdate("CREATE VIRTUAL TABLE demo_index USING rtree(id, minX, maxX, minY, maxY);");
			stmt.executeUpdate("INSERT INTO demo_index VALUES(1, 80.0, 90.0, 80.0, 90.0);");
			conn.commit();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
