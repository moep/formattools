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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.mapsforge.applications.debug.db.TileSQLiteWriter;
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

	private static void mapToSQLite(String mapsforeMapFilePath, String outputFilePath, boolean useCompression) {
		SimpleTileExtractor ste = null;
		MapFile mf = null;
		TileSQLiteWriter writer = null;
		byte[] zoomLevelConfiguration;
		byte[] tile;

		try {
			ste = new SimpleTileExtractor(mapsforeMapFilePath);
			mf = ste.getMapFile();

			// Get zoom level configuration
			zoomLevelConfiguration = mf.getBaseZoomLevel();

			// Write tile to DB
			writer = new TileSQLiteWriter(outputFilePath, zoomLevelConfiguration, useCompression);
			int added = 0;

			// Read tile
			for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						tile = ste.getTile(x, y, zoomInterval);

						if (tile != null) {
							writer.insert(tile, x, y, zoomInterval);
							++added;
						}

						if (added % 100 == 0) {
							System.out.printf("Added %7d tiles\r", added);
						}

					}
				}
			}

			// Write batches
			writer.finish();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TileIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}

	private static void mapToZip(String mapsforeMapFilePath, String outputFilePath) {
		SimpleTileExtractor ste = null;
		MapFile mf = null;
		byte[] tile;
		int added = 0;

		ZipArchiveOutputStream zos = null;
		ArchiveEntry e = null;

		try {
			zos = new ZipArchiveOutputStream(new FileOutputStream(outputFilePath));
			ste = new SimpleTileExtractor(mapsforeMapFilePath);
			mf = ste.getMapFile();

			File f = new File("/home/moep/dummy.txt");

			// Read tile
			for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals(); zoomInterval++) {
				for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
					for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
						tile = ste.getTile(x, y, zoomInterval);

						if (tile != null) {
							ZipArchiveEntry entry = new ZipArchiveEntry(zoomInterval + "/" + x + "/"
									+ y);
							// ArchiveEntry entry = zos.createArchiveEntry(f, zoomInterval + "/" + x +
							// "/" + y);
							zos.putArchiveEntry(entry);
							zos.write(tile);
							zos.closeArchiveEntry();

							++added;
						}

						if (added % 100 == 0) {
							System.out.printf("Added %7d tiles\r", added);
							System.out.flush();
						}

					}
				}
			}

			zos.finish();
			zos.close();

			System.out.println("\r\nDone.");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	// private static void mapToZip(String mapsforeMapFilePath, String outputFilePath) {
	// SimpleTileExtractor ste = null;
	// MapFile mf = null;
	// InputStream is = null;
	// byte[] tile = null;
	// int added = 0;
	//
	// ZipFile zf = null;
	// ZipParameters p = new ZipParameters();
	// // Tell zipper that input is not a file
	// p.setSourceExternalStream(true);
	// p.setCompressionLevel(Zip4jConstants.COMP_STORE);
	//
	// try {
	// ste = new SimpleTileExtractor(mapsforeMapFilePath);
	// mf = ste.getMapFile();
	// zf = new ZipFile(new File(outputFilePath));
	//
	// // Read tile
	// for (byte zoomInterval = 0; zoomInterval < ste.getMapFile().getAmountOfZoomIntervals();
	// zoomInterval++) {
	// for (int y = ste.getMinY(zoomInterval); y <= ste.getMaxY(zoomInterval); y++) {
	// for (int x = ste.getMinX(zoomInterval); x <= ste.getMaxX(zoomInterval); x++) {
	// tile = ste.getTile(x, y, zoomInterval);
	//
	// if (tile != null) {
	// p.setFileNameInZip(added + ".tile");
	// is = new FileInputStream("/home/moep/dummy.txt");
	// zf.addStream(is, p);
	// is.close();
	// ++added;
	// }
	//
	// if (added % 100 == 0) {
	// System.out.printf("Added %7d tiles\r", added);
	// System.out.flush();
	// }
	//
	// }
	// }
	// }
	//
	// System.out.println("\r\nDone.");
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// } catch (IOException e) {
	// e.printStackTrace();
	// } catch (TileIndexOutOfBoundsException e) {
	// e.printStackTrace();
	// } catch (ZipException e) {
	// e.printStackTrace();
	// } finally {
	//
	// if (is != null) {
	// try {
	// is.close();
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// }
	// }
	// }

	/**
	 * @param args
	 *            not used command line parameters.
	 * @throws IOException
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("Map2SQLite");
		mapToSQLite("/home/moep/germany-0.2.4.map", "/home/moep/germany-compressed.map.sqlite", true);
		// System.out.println("Map2Zip");
		// mapToZip("/home/moep/germany-0.2.4.map", "/home/moep/germany.map.zip");

		// System.out.println("Map2Zip");
		// mapToZip("/home/moep/berlin.map", "/home/moep/berlin.map.zip");

		// ZipFile zf = new ZipFile("/home/moep/germany.map.zip");
		// ZipArchiveEntry entry = zf.getEntry("1/8787/5364");
		// System.out.println(entry.getName());
		// zf.close();

		// net.lingala.zip4j.core.ZipFile zf2 = new
		// net.lingala.zip4j.core.ZipFile("/home/moep/bla/test.zip");
		// System.out.println(zf2.getFileHeaders().size());
		// System.out.println("Getting header");
		// FileHeader header = zf2.getFileHeader("1/8787/5364");
		// System.out.println("Header == null: " + (header == null));
		// System.out.println("Getting data");
		// System.out.println(header.getFileName());
		// System.out.println("Done");

		// ZipFile zf = new ZipFile("/home/moep/germany.map.zip");
		// ZipEntry e = zf.getEntry("1/8462/5485");
		// System.out.println("Size: " + e.getSize());
		// System.out.println("Compressed: " + e.getCompressedSize());
		// zf.close();

	}
}
