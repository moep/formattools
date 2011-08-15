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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.mapsforge.core.MercatorProjection;

/**
 * A simple Parser that parses the tiles as raw data. Unlike {@link MapFormatReader} this parser does
 * not read all at once. Instead each tile has to be pulled by the {@link #getNextTile()} method.
 * 
 * @author Karsten Groll
 * 
 */
public class SimpleTileExtractor {
	private static final double COORDINATES_FACTOR = 1000000.0;
	private byte zoomInterval;
	private RandomAccessFile f;
	private MapFile mapFile;
	private long offset;
	private byte[] buffer;
	private long[][] tileOffset;
	private Rect[] tileBoundingBox;

	public SimpleTileExtractor(String path) throws IOException {
		this.zoomInterval = zoomInterval;
		this.f = new BufferedRandomAccessFile(path, "r", 1024 * 10);
		this.buffer = new byte[1024 * 100];
		parseMapFileHeader();
		this.tileBoundingBox = new Rect[this.mapFile.getAmountOfZoomIntervals()];
		this.tileOffset = new long[this.mapFile.getAmountOfZoomIntervals()][];
		parseSubFileHeaders();
	}

	private void parseMapFileHeader() throws IOException {
		this.mapFile = new MapFile();
		this.offset = 0;
		byte[] magicByte;

		// Magic bytes (20B)
		magicByte = new byte[20];
		this.f.read(magicByte, 0, 20);
		this.offset += 20;
		this.mapFile.setMagicByte(magicByte);

		// Header size (4B)
		this.mapFile.setHeaderSize(getNextInt());

		// File version (4B)
		this.mapFile.setFileVersion(getNextInt());

		// Flags (1B)
		this.mapFile.setFlags(getNextByte());

		// Number of zoom intervals (1B)
		this.mapFile.setAmountOfZoomIntervals(getNextByte());

		// Projection name (variable)
		this.mapFile.setProjection(getNextString());

		// Tile size (2B)
		this.mapFile.setTileSize(getNextDword());

		// Bounding box (4*4B)
		this.mapFile.setBoundingBox(getNextInt(), getNextInt(), getNextInt(), getNextInt());

		// Map start position (8B)
		if (this.mapFile.isMapStartPositionFlagSet()) {
			this.mapFile.setMapStartPosition(getNextInt(), getNextInt());
		}

		// Date of creation (8B)
		this.mapFile.setDateOfCreation(getNextLong());

		// POI tag mapping (variable)
		// amount of mappings (2B)
		this.mapFile.setAmountOfPOIMappings(getNextDword());
		this.mapFile.preparePOIMappings();

		// POI tag mapping (variable)
		int tagID;
		String tagName;
		for (int i = 0; i < this.mapFile.getAmountOfPOIMappings(); i++) {

			// Tag name (variable)
			tagName = getNextString();

			// tag ID (2B)
			tagID = getNextDword();
			this.mapFile.getPOIMappings()[tagID] = tagName;
		}

		// Way tag mapping (variable)
		// amount of mappings (2B)
		this.mapFile.setAmountOfWayTagMappings(getNextDword());
		this.mapFile.prepareWayTagMappings();

		// Way tag mapping (variable)
		for (int i = 0; i < this.mapFile.getAmountOfWayTagMappings(); i++) {
			// tag name (variable)
			tagName = getNextString();

			// tag ID (2B)
			tagID = getNextDword();
			this.mapFile.getWayTagMappings()[tagID] = tagName;
		}

		// Comment (variable)
		this.mapFile.setComment(getNextString());

		// Zoom interval configuration (variable)
		this.mapFile.prepareZoomIntervalConfiguration();
		for (int i = 0; i < this.mapFile.getAmountOfZoomIntervals(); i++) {
			this.mapFile.setZoomIntervalConfiguration(i, getNextByte(), getNextByte(), getNextByte(),
					getNextLong5(), getNextLong5());
		}

	}

	private void parseSubFileHeaders() throws IOException {
		for (byte z = 0; z < this.mapFile.getAmountOfZoomIntervals(); z++) {
			this.offset = this.mapFile.getAbsoluteStartPosition()[z];
			this.f.seek(this.offset);
			System.out.print("Reading header for subfile " + z + "...");

			// Read bounding box
			long firstX = MercatorProjection.longitudeToTileX(this.mapFile.getMinLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long lastX = MercatorProjection.longitudeToTileX(this.mapFile.getMaxLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long firstY = MercatorProjection.latitudeToTileY(this.mapFile.getMinLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long lastY = MercatorProjection.latitudeToTileY(this.mapFile.getMaxLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);

			this.tileBoundingBox[z] = new Rect((int) firstX, (int) lastX, (int) firstY, (int) lastY);

			// Skip index signature (16B, optional)
			if (this.mapFile.isDebugFlagSet()) {
				this.offset += 16;
			}

			int numBlocks = (this.tileBoundingBox[z].getMaxX() - this.tileBoundingBox[z].getMinX() + 1)
					* (this.tileBoundingBox[z].getMaxY() - this.tileBoundingBox[z].getMinY() + 1);

			this.tileOffset[z] = new long[numBlocks];
			System.out.println("Tile offsets: ");
			for (int i = 0; i < numBlocks; i++) {
				this.tileOffset[z][i] = getNextLong5();
				System.out.println(this.tileOffset[z][i]);
			}

			System.out.println("done");
		}
	}

	public byte[] getTile(int x, int y, byte zoomInterval) throws TileIndexOutOfBoundsException,
			IOException {
		assert this.mapFile != null;

		System.out.println("(" + x + "," + y + ") @ " + zoomInterval);

		// Goto the tile's position
		// row = y - minY; col = x - min X
		// id = row * (maxX - minX) + col

		// 7 9012
		// 6 5678
		// 5 1234
		//
		// 3456

		int row = (int) (y - this.tileBoundingBox[zoomInterval].getMinY());
		System.out.println("row: " + row);
		int col = (int) (x - this.tileBoundingBox[zoomInterval].getMinX());
		System.out.println("col: " + col);
		int id = (int) (row
				* (this.tileBoundingBox[zoomInterval].getMaxX() - this.tileBoundingBox[zoomInterval]
						.getMinX()) + col);
		System.out.println("id: " + id);
		System.out.println("Relative tile offset: " + this.tileOffset[zoomInterval][id]);

		this.offset = this.tileOffset[zoomInterval][id]
				+ this.mapFile.getAbsoluteStartPosition()[zoomInterval];
		// this.f.seek(this.offset);

		System.out.println("Getting tile (" + x + ", " + y + ") @ position " + this.offset);

		// TODO read and return tile
		return null;
	}

	// DEBUG ONLY
	public MapFile getMapFile() {
		return this.mapFile;
	}

	/**
	 * Tears down the parser and closes all of its handles.
	 */
	public void shutdown() throws IOException {
		System.out.println("Shutting down...");
		this.f.close();
	}

	private static int byteArrayToInt(byte[] bytes) {
		return (bytes[0] << 24) | ((bytes[1] & 0xff) << 16) | ((bytes[2] & 0xff) << 8)
				| (bytes[3] & 0xff);
	}

	private static long byteArrayToLong(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return bb.getLong();
	}

	private int getNextInt() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 4);
		this.offset += 4;
		return SimpleTileExtractor.byteArrayToInt(this.buffer);
	}

	private byte getNextByte() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 1);
		this.offset += 1;

		return this.buffer[0];
	}

	private String getNextString() throws IOException {
		int strlen = getNextVBEUInt();
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, strlen);
		this.buffer[strlen] = '\0';
		this.offset += strlen;

		StringBuffer sb = new StringBuffer(strlen);
		for (int i = 0; i < strlen; i++) {
			sb.append((char) this.buffer[i]);
		}
		// return new String(this.buffer).substring(0, strlen);
		return sb.toString();
	}

	private int getNextDword() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 2);
		this.offset += 2;

		return (this.buffer[0] << 8) + (this.buffer[1] & 0xff);
	}

	private long getNextLong() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 8);
		this.offset += 8;

		return SimpleTileExtractor.byteArrayToLong(buffer);
	}

	private long getNextLong5() throws IOException {

		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 5);

		long ret = (this.buffer[0] & 0xffL) << 32 | (this.buffer[1] & 0xffL) << 24
				| (this.buffer[2] & 0xffL) << 16 | (this.buffer[3] & 0xffL) << 8
				| (this.buffer[4] & 0xffL);

		this.offset += 5;

		return ret;

	}

	private int getNextVBEUInt() throws IOException {
		byte shift = 0;
		int ret = 0;
		byte b;

		// Bytes with continuation bit (low order bytes)
		while (((b = getNextByte()) & 0x80) != 0) {
			ret |= (b & 0x7f) << shift;
			shift += 7;
		}

		// High order byte (last byte)
		ret |= (b & 0x7f) << shift;

		return ret;
	}

	private int getNextVBESInt() throws IOException {
		// TODO test it
		byte shift = 0;
		int ret = 0;
		byte b;

		// Bytes with continuation bit (low order bytes)
		while (((b = getNextByte()) & 0x80) != 0) {
			ret |= (b & 0x7f) << shift;
			shift += 7;
		}

		// High order byte (last byte)
		ret |= (b & 0x7f) << shift;

		return (ret & 0x80000000) == 0 ? ret : -ret;
	}

	public int getMaxX(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMaxX();
	}

	public int getMinX(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMinX();
	}

	public int getMaxY(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMaxY();
	}

	public int getMinY(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMinY();
	}

	private class Rect {
		private int minX;
		private int maxX;
		private int minY;
		private int maxY;

		/**
		 * Sets the values for <code>minX, maxX, minY and maxY</code> by asserting
		 * <code>minX = min(x1,x2)</code> ... <code>maxY = max(y1,y2)</code>
		 * 
		 * @param x1
		 *            A value for a x-coordinate.
		 * @param x2
		 *            Another value for a x-coordinate.
		 * @param y1
		 *            A value for a y-coordinate.
		 * @param y2
		 *            Another value for a y-coordinate.
		 */
		Rect(int x1, int x2, int y1, int y2) {
			this.minX = x1 < x2 ? x1 : x2;
			this.maxX = x1 < x2 ? x2 : x1;
			this.minY = y1 < y2 ? y1 : y2;
			this.maxY = y1 < y2 ? y2 : y1;
		}

		@Override
		public String toString() {
			return "(" + minX + ", " + maxX + ") ... (" + minY + ", " + maxY + ")";
		}

		public int getMinX() {
			return minX;
		}

		public int getMaxX() {
			return maxX;
		}

		public int getMinY() {
			return minY;
		}

		public int getMaxY() {
			return maxY;
		}

	}
}
