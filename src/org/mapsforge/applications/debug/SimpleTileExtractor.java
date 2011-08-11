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
	private static final int COORDINATES_FACTOR = 1000000;
	private byte zoomInterval;
	private RandomAccessFile f;
	private MapFile mapFile;
	private long offset;
	private byte[] buffer;
	private int[][] tileOffset;
	private Rect[] tileBoundingBox;

	public SimpleTileExtractor(String path) throws IOException {
		this.zoomInterval = zoomInterval;
		this.f = new BufferedRandomAccessFile(path, "r", 1024 * 10);
		this.buffer = new byte[1024 * 100];
		parseMapFileHeader();
		this.tileBoundingBox = new Rect[this.mapFile.getAmountOfZoomIntervals()];
		parseSubFileHeaders();

		this.tileOffset = new int[this.mapFile.getAmountOfZoomIntervals()][];

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
			System.out.print("Reading header for subfile " + z);

			// Read bounding box
			long firstX = MercatorProjection.longitudeToTileX(this.mapFile.getMinLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[zoomInterval]);
			long lastX = MercatorProjection.longitudeToTileX(this.mapFile.getMaxLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[zoomInterval]);
			long firstY = MercatorProjection.latitudeToTileY(this.mapFile.getMinLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[zoomInterval]);
			long lastY = MercatorProjection.latitudeToTileY(this.mapFile.getMaxLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[zoomInterval]);

			tileBoundingBox[z] = new Rect(firstX, lastX, firstY, lastY);
			System.out.println("Bounding box: " + tileBoundingBox[z]);

			// Skip index signature (16B, optional)
			if (this.mapFile.isDebugFlagSet()) {
				this.offset += 16;
			}

			int numBlocks = (int) ((tileBoundingBox[z].maxX - tileBoundingBox[z].minX + 1) * (tileBoundingBox[z].maxY
					- tileBoundingBox[z].minY + 1));
			System.out.println("Number of blocks: " + numBlocks);

			for (long i = 0; i < numBlocks; i++) {
				System.out.println(getNextLong5());
			}

			System.out.println("done");
		}
	}

	public byte[] getTile(int x, int y, byte zoomInterval) throws TileIndexOutOfBoundsException {
		assert this.mapFile != null;

		System.out.println("Tile offset");

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

	private class Rect {
		private long minX;
		private long maxX;
		private long minY;
		private long maxY;

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
		Rect(long x1, long x2, long y1, long y2) {
			this.minX = x1 < x2 ? x1 : x2;
			this.maxX = x1 < x2 ? x2 : x1;
			this.minY = y1 < y2 ? y1 : y2;
			this.minY = y1 < y2 ? y2 : y1;
		}

		@Override
		public String toString() {
			return "(" + minX + ", " + maxX + ") ... (" + minY + ", " + maxY + ")";
		}
	}
}
