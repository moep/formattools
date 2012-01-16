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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.mapsforge.storage.MapDataProvider;
import org.mapsforge.storage.MapDataProviderImpl;
import org.mapsforge.storage.dataExtraction.MapFileMetaData;
import org.mapsforge.storage.tile.PCTilePersistenceManager;
import org.mapsforge.storage.tile.TileDataContainer;
import org.mapsforge.storage.tile.TilePersistenceManager;

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

		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * 
	 * @param mapsforgeMapFilePath
	 *            Path to mapsforge 0.3 map file.
	 * @param outputFilePath
	 *            Path to map file that will be generated.
	 * @param useCompression
	 *            Currently not used.
	 */
	private static void mapToSQLite(String mapsforgeMapFilePath, String outputFilePath, boolean useCompression) {
		SimpleTileExtractor ste = null;
		MapFile mf = null;

		// TileSQLiteWriter writer = null;
		PCTilePersistenceManager writer = null;

		byte[] zoomLevelConfiguration;
		byte[] tile;

		try {
			ste = new SimpleTileExtractor(mapsforgeMapFilePath);
			mf = ste.getMapFile();

			// Get zoom level configuration
			zoomLevelConfiguration = mf.getBaseZoomLevel();

			// Write tile to DB
			// writer = new TileSQLiteWriter(outputFilePath, zoomLevelConfiguration, useCompression);
			MapFileMetaData mfm = new MapFileMetaData();
			mfm.setFileVersion("0.4-experimental");
			mfm.setDateOfCreation(System.currentTimeMillis());
			mfm.setBoundingBox(mf.getMinLat(), mf.getMinLon(), mf.getMaxLat(), mf.getMaxLon());
			mfm.setTileSize(mf.getTileSize());
			mfm.setProjection(mf.getProjection());
			mfm.setLanguagePreference(mf.getLanguagePreference());
			mfm.setFlags(mf.getFlags());
			mfm.setMapStartPosition(mf.getMapStartLat(), mf.getMapStartLon());
			mfm.setComment(mf.getComment() + "(Converted with MapFileDebugger)");
			mfm.setAmountOfPOIMappings(mf.getAmountOfPOIMappings());
			mfm.preparePOIMappings();
			for (int i = 0; i < mf.getAmountOfPOIMappings(); i++) {
				mfm.setPOIMappings(mf.getPOIMappings());
			}
			mfm.setAmountOfWayTagMappings(mf.getAmountOfWayTagMappings());
			mfm.prepareWayTagMappings();
			for (int i = 0; i < mf.getAmountOfWayTagMappings(); i++) {
				mfm.setWayTagMappings(mf.getWayTagMappings());
			}
			mfm.setAmountOfZoomIntervals(mf.getAmountOfZoomIntervals());
			mfm.prepareZoomIntervalConfiguration();
			for (int i = 0; i < mf.getAmountOfZoomIntervals(); i++) {
				mfm.setZoomIntervalConfiguration(i, mf.getBaseZoomLevel()[i], mf.getMinimalZoomLevel()[i],
						mf.getMaximalZoomLevel()[i], TileDataContainer.TILE_TYPE_VECTOR);
			}

			writer = new PCTilePersistenceManager(outputFilePath, mfm);
			int added = 0;

			Vector<TileDataContainer> tiles = new Vector<TileDataContainer>();

			// Read tile
			for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						tile = ste.getTile(x, y, zoomInterval);

						if (tile != null) {
							// writer.insert(tile, x, y, zoomInterval);
							tiles.add(new TileDataContainer(tile, TileDataContainer.TILE_TYPE_VECTOR,
									x, y, zoomInterval));
							// writer.insertOrUpdateTile(tile, x, y, zoomInterval);

							++added;
						}

						if (added % 100 == 0) {
							writer.insertOrUpdateTiles(tiles);
							tiles.clear();
							System.out.printf("Added %7d tiles\r", added);
						}

					}
				}
			}

			// Write batches
			// writer.finish();
			writer.insertOrUpdateTiles(tiles);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}

	private static void countAndPrintNumberOfStreetEntries(String path) {
		SimpleTileExtractor ste = null;
		byte[] rawTile;
		Tile tile = null;
		List<org.mapsforge.applications.debug.Way> ways = null;
		HashMap<String, Integer> globalCount = new HashMap<String, Integer>();

		Integer count = null;

		// Total amount of street names
		int totalStreetNameCount = 0;
		// Street names that are in more than 1 tile
		int multipleOccurenceStreetNameCount = 0;
		// Exception values (#tiles > 100 for a given street name)
		List<Integer> exceptionalValues = new ArrayList<Integer>();

		// Test if tile coordinates are correct (NEEDS DEBUG FILE!)
		try {
			ste = new SimpleTileExtractor(path);

			for (byte zoomInterval = 1; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						rawTile = ste.getTile(x, y, zoomInterval);
						if (rawTile == null)
							continue;

						tile = TileFactory.getTileFromRawData(rawTile, zoomInterval, ste.getMapFile());

						// Get all way
						ways = tile.getWays();

						if (ways == null)
							continue;

						HashMap<String, Integer> localCount = new HashMap<String, Integer>();

						for (org.mapsforge.applications.debug.Way w : ways) {
							if (w.getName() == null)
								continue;

							if (localCount.containsKey(w.getName())) {
								count = Integer.valueOf(localCount.get(w.getName()).intValue() + 1);
								localCount.put(w.getName(), count);

							} else {
								localCount.put(w.getName(), new Integer(1));
							}

						}

						// System.out.println("This tile has " + localCount.size() + " street names.");
						for (String key : localCount.keySet()) {
							// if (localCount.get(key).intValue() > 1) {
							// System.out.println("  " + key + ": " + localCount.get(key));
							// }

							// Has the street name been discovered before?
							if (globalCount.containsKey(key)) {
								count = Integer.valueOf(globalCount.get(key).intValue() + 1);
								globalCount.put(key, count);
							} else {
								++totalStreetNameCount;
								globalCount.put(key, new Integer(1));
							}

						}

						tile = null;

					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			System.err.println(e.getMessage());
		}

		int[] streetNamesPerTile = new int[100];

		for (String key : globalCount.keySet()) {
			try {
				++streetNamesPerTile[globalCount.get(key).intValue()];
			} catch (ArrayIndexOutOfBoundsException e) {
				System.err.println(e.getMessage());

				try {
					exceptionalValues.add(Integer.parseInt(e.getMessage()));
				} catch (NumberFormatException e1) {
					System.err.println(e.getMessage());
				}

			}

			if (globalCount.get(key).intValue() > 1) {
				++multipleOccurenceStreetNameCount;
			}
		}

		System.out.println("----------");
		System.out.println("TileCount | # Street names");
		for (int i = 1; i < streetNamesPerTile.length; i++) {
			System.out.printf("    %3d   |      %4d      \r\n", i, streetNamesPerTile[i]);
		}

		System.out.println("----------");
		System.out.println("Outsiders: ");
		Collections.sort(exceptionalValues);
		for (Integer i : exceptionalValues) {
			System.out.print(i + ", ");
		}
		System.out.println();

		System.out.println("----------");
		System.out.println("Total street names: " + totalStreetNameCount);
		System.out.println("Streetnames in > 1 tiles: " + multipleOccurenceStreetNameCount);
		System.out.printf("Total: %2.2f%%\r\n", (100.0f * multipleOccurenceStreetNameCount / totalStreetNameCount));

	}

	private static void streetNamesToSQL(String path, String sqlDumpFilePath) {
		SimpleTileExtractor ste = null;
		byte[] rawTile;
		Tile tile = null;
		List<org.mapsforge.applications.debug.Way> ways = null;
		HashMap<String, Integer> globalCount = new HashMap<String, Integer>();
		TreeMap<Integer, List<String>> totalOccurenceMap = new TreeMap<Integer, List<String>>(Collections.reverseOrder());
		Integer count = null;

		// Test if tile coordinates are correct (NEEDS DEBUG FILE!)
		try {
			ste = new SimpleTileExtractor(path);

			for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						rawTile = ste.getTile(x, y, zoomInterval);
						if (rawTile == null)
							continue;

						tile = TileFactory.getTileFromRawData(rawTile, zoomInterval, ste.getMapFile());

						// Get all way
						ways = tile.getWays();

						if (ways == null)
							continue;

						HashMap<String, Integer> localCount = new HashMap<String, Integer>();

						for (org.mapsforge.applications.debug.Way w : ways) {
							if (w.getName() == null)
								continue;

							if (localCount.containsKey(w.getName())) {
								count = Integer.valueOf(localCount.get(w.getName()).intValue() + 1);
								localCount.put(w.getName(), count);

							} else {
								localCount.put(w.getName(), new Integer(1));
							}

						}

						for (String key : localCount.keySet()) {
							// Has the street name been discovered before?
							if (globalCount.containsKey(key)) {
								count = Integer.valueOf(globalCount.get(key).intValue() + 1);
								globalCount.put(key, count);
							} else {
								globalCount.put(key, new Integer(1));
							}

						}

						tile = null;

					}
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			e.printStackTrace();
		}

		// Order street names by occurrence

		for (String key : globalCount.keySet()) {
			if (totalOccurenceMap.get(globalCount.get(key)) == null) {
				totalOccurenceMap.put(new Integer(globalCount.get(key)), new LinkedList<String>());
			}
			totalOccurenceMap.get(globalCount.get(key)).add(key);
		}

		List<String> names;

		// Print result
		// System.out.println("#Occurences\tValues");
		// for (Integer key : totalOccurenceMap.keySet()) {
		// System.out.printf("%5d:", key);
		// names = totalOccurenceMap.get(key);
		// for (int i = 0; i < names.size(); i++) {
		// System.out.print(names.get(i) + " | ");
		// }
		// System.out.println();
		// }

		// Write SQL statement
		int index = 0;
		String name;
		final int BUFFER_SIZE = 1024 * 1000;
		try {
			System.out.println("Writing SQL dump file...");
			FileOutputStream fos = new FileOutputStream(sqlDumpFilePath);
			String queryString;
			byte[] queryStringByteArray;
			byte[] commitStringArray;
			byte[] buffer = new byte[BUFFER_SIZE];
			ByteBuffer bb = ByteBuffer.wrap(buffer);
			bb.put("BEGIN;\n".getBytes("UTF-8"));
			bb.put("DROP TABLE IF EXISTS way_names;\n".getBytes("UTF-8"));
			bb.put("CREATE TABLE way_names (id INTEGER PRIMARY KEY, name VARCHAR);\n".getBytes("UTF-8"));

			for (Integer key : totalOccurenceMap.keySet()) {
				names = totalOccurenceMap.get(key);
				for (int i = 0; i < names.size(); i++) {
					name = names.get(i);
					name = name.replaceAll("\"", "\"\"");

					// Name fits into buffer
					queryString = new String("INSERT INTO way_names (id, name) VALUES (" + index + ",\"" +
							name + "\");\n");
					System.out.print(queryString);
					queryStringByteArray = queryString.getBytes("UTF-8");
					if (queryStringByteArray.length <= bb.remaining()) {
						bb.put(queryStringByteArray);
					} else {
						fos.write(bb.array(), 0, bb.position());
						buffer = new byte[BUFFER_SIZE];
						bb = ByteBuffer.wrap(buffer);
						bb.put(queryStringByteArray);
					}

					++index;
				}
			}

			commitStringArray = "COMMIT;".getBytes("UTF-8");
			if (commitStringArray.length <= bb.remaining()) {
				bb.put(commitStringArray);
			} else {
				fos.write(bb.array(), 0, bb.position());
				buffer = new byte[BUFFER_SIZE];
				bb = ByteBuffer.wrap(buffer);
				bb.put(commitStringArray);
			}

			// Write remaining entries
			fos.write(bb.array(), 0, bb.position());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * @param args
	 *            not used command line parameters.
	 */
	public static void main(String[] args) throws Exception {
		/*
		 * Step 1: Convert old map format to new SQLite format
		 * 
		 * Input: Map file in old format, path to SQLite map file to be created, compression flag
		 * 
		 * Output: A SQLite based map file
		 */
		// mapToSQLite("/home/moep/maps/berlin.map", "/home/moep/maps/mapsforge/berlin.map", false);

		/*
		 * Step 2a: Create SQL statement for a global way name index.
		 * 
		 * Input: Map file in old format, path where SQL queries should be dumped to
		 * 
		 * Output: File containing SQL queries for creating a global street index
		 */
		// streetNamesToSQL("/home/moep/maps/berlin.map",
		// "/home/moep/maps/mapsforge/berlin_global_street_index.sql");

		/*
		 * Counts the amount of tiles a street name lies in. This is needed for statistical purposes for
		 * creating a way name index.
		 * 
		 * Input: Map file in old format
		 */
		// countAndPrintNumberOfStreetEntries("/home/moep/maps/china.map");

		/**
		 * This is needed to check if the parser can read the old map format.
		 * 
		 * Input: Old map format file compiled with 'debug-file=true'
		 */
		// checkIdexes("/home/moep/maps/bremen.map");

		/*
		 * Routine for extracting a single tile from the old format
		 */
		// SimpleTileExtractor ste = new
		// SimpleTileExtractor("/home/moep/maps/germany-0.3.0-SNAPSHOT.map");
		// System.out.println(ste.getMapFile());
		// byte[] tile = ste.getTile(8802, 5375, (byte) 1);
		// FileOutputStream os = new FileOutputStream("/home/moep/maps/debug.tile");
		// os.write(tile);
		// os.close();
		// TileFactory.getTileFromRawData(tile, (byte) 1, ste.getMapFile());

		System.out.println("Initializing map data provider");
		TilePersistenceManager tpm = new PCTilePersistenceManager("/home/moep/maps/mapsforge/berlin.map");
		MapDataProvider mdp = new MapDataProviderImpl(tpm, false);
		Collection<org.mapsforge.storage.atoms.Way> ways = mdp.getAllWays(8802, 5373, (byte) 1);

		tpm.close();

	}
}
