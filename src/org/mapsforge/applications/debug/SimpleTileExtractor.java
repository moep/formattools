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
 * not read all at once. Instead each tile has to be pulled by the {@link #getTile()} method.
 * 
 * @author Karsten Groll
 * 
 */
public class SimpleTileExtractor {
	private static final long BITMASK_INDEX_OFFSET = 0x7FFFFFFFFFL;
	private static final double COORDINATES_FACTOR = 1000000.0;
	private RandomAccessFile f;
	private MapFile mapFile;
	private long offset;
	private byte[] buffer;
	private long[][] tileOffset;
	private Rect[] tileBoundingBox;

	/**
	 * Constructor that parses a map file's header.
	 * 
	 * @param path
	 *            Path to the map file that should be parsed.
	 * @throws IOException
	 *             if the file cannot be read.
	 */
	public SimpleTileExtractor(String path) throws IOException {
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

		// File size (8B)
		this.mapFile.setFileSize(getNextLong());

		// Date of creation (8B)
		this.mapFile.setDateOfCreation(getNextLong());

		// Bounding box (4*4B)
		this.mapFile.setBoundingBox(getNextInt(), getNextInt(), getNextInt(), getNextInt());

		// Tile size (2B)
		this.mapFile.setTileSize(getNextDword());

		// Projection name (variable)
		this.mapFile.setProjection(getNextString());
		System.out.println("Projection: " + this.mapFile.getProjection());

		// Flags (1B)
		this.mapFile.setFlags(getNextByte());

		// Map start position (8B)
		if (this.mapFile.isMapStartPositionFlagSet()) {
			this.mapFile.setMapStartPosition(getNextInt(), getNextInt());
		}

		// Start zoom level (1B)
		if (this.mapFile.isZoomLevelFlagSet()) {
			getNextByte();
		}

		// Language preference (variable)
		if (this.mapFile.isLanguagePreferenceFlagSet()) {
			this.mapFile.setLanguagePreference(getNextString());
			System.out.println("Language preference: " + this.mapFile.getLanguagePreference());
		}

		// Comment (variable)
		if (this.mapFile.isDebugFlagSet()) {
			this.mapFile.setComment(getNextString());
			System.out.println("Comment: " + this.mapFile.getComment());
		}

		// POI tag mapping (variable)
		// amount of mappings (2B)
		this.mapFile.setAmountOfPOIMappings(getNextDword());
		this.mapFile.preparePOIMappings();

		// POI tag mapping (variable)
		String tagName;
		for (int i = 0; i < this.mapFile.getAmountOfPOIMappings(); i++) {

			// Tag name (variable)
			tagName = getNextString();

			// tag ID (2B)
			this.mapFile.getPOIMappings()[i] = tagName;
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
			this.mapFile.getWayTagMappings()[i] = tagName;
		}

		// Number of zoom intervals (1B)
		this.mapFile.setAmountOfZoomIntervals(getNextByte());

		// Zoom interval configuration (variable)
		this.mapFile.prepareZoomIntervalConfiguration();
		for (int i = 0; i < this.mapFile.getAmountOfZoomIntervals(); i++) {
			this.mapFile.setZoomIntervalConfiguration(i, getNextByte(), getNextByte(), getNextByte(),
					getNextLong(), getNextLong());
		}

	}

	private void parseSubFileHeaders() throws IOException {
		for (byte z = 0; z < this.mapFile.getAmountOfZoomIntervals(); z++) {
			this.offset = this.mapFile.getAbsoluteStartPosition()[z];
			this.f.seek(this.offset);

			// Read bounding box
			long firstX = MercatorProjection.longitudeToTileX(this.mapFile.getMinLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long lastX = MercatorProjection.longitudeToTileX(this.mapFile.getMaxLon()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long firstY = MercatorProjection.latitudeToTileY(this.mapFile.getMaxLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);
			long lastY = MercatorProjection.latitudeToTileY(this.mapFile.getMinLat()
					/ SimpleTileExtractor.COORDINATES_FACTOR,
					this.mapFile.getBaseZoomLevel()[z]);

			this.tileBoundingBox[z] = new Rect((int) firstX, (int) lastX, (int) firstY, (int) lastY);

			System.out.println("\nFirstX : " + firstX);
			System.out.println("LastX : " + lastX);
			System.out.println("FirstY : " + firstY);
			System.out.println("LastY : " + lastY);

			// System.out.println("Min lat: " + this.mapFile.getMinLat());
			// System.out.println("Max lat: " + this.mapFile.getMaxLat());
			// System.out.println("Min lon: " + this.mapFile.getMinLon());
			// System.out.println("Max lon: " + this.mapFile.getMaxLon());

			// Skip index signature (16B, optional)
			if (this.mapFile.isDebugFlagSet()) {
				this.offset += 16;
			}

			int numBlocks = (this.tileBoundingBox[z].getMaxX() - this.tileBoundingBox[z].getMinX() + 1)
					* (this.tileBoundingBox[z].getMaxY() - this.tileBoundingBox[z].getMinY() + 1);
			System.out.println("#Blocks: " + numBlocks);
			// numBlocks = Math.abs(numBlocks);

			// System.out.println("TBB: (" + this.tileBoundingBox[z].getMinX() + "," +
			// this.tileBoundingBox[z].getMaxX() +
			// ")...(" + this.tileBoundingBox[z].getMinY() + "," + this.tileBoundingBox[z].getMaxY() +
			// ")");
			System.out.println("Next tile start @ " + (numBlocks * 5 + this.offset));
			this.tileOffset[z] = new long[numBlocks];
			for (int i = 0; i < numBlocks; i++) {
				this.tileOffset[z][i] = getNextLong5() & BITMASK_INDEX_OFFSET;
				System.out.printf("offset: %5x\r\n", this.tileOffset[z][i]);
			}
			System.out.println("Next tile start @ " + (this.offset));
		}
	}

	/**
	 * Gets a raw data tile at a given tile coordinate (x,y) and a base zoom interval. The coordinates
	 * are the squares' coordinates within the default zoom level of the base zoom interval.
	 * 
	 * @param x
	 *            The tile's x-coordinate.
	 * @param y
	 *            The tile's y-coordinate.
	 * @param zoomInterval
	 *            The base zoom interval.
	 * @return Raw data as byte array containing the tile's data.
	 * @throws TileIndexOutOfBoundsException
	 *             if the given coordinates are not within the map file's defined bounding box or if the
	 *             base zoom level does not exist.
	 * @throws IOException
	 *             if the file could not be read.
	 */
	public byte[] getTile(int x, int y, byte zoomInterval) throws TileIndexOutOfBoundsException,
			IOException {
		assert this.mapFile != null;
		assert zoomInterval > 0;

		// TODO test exception
		if (x < getMinX(zoomInterval) || x > getMaxX(zoomInterval) || y < getMinY(zoomInterval)
				|| y > getMaxY(zoomInterval) || zoomInterval > this.mapFile.getAmountOfZoomIntervals()) {
			throw new TileIndexOutOfBoundsException(x, y, zoomInterval);
		}

		int row = y - this.tileBoundingBox[zoomInterval].getMinY();
		int col = x - this.tileBoundingBox[zoomInterval].getMinX();
		int id = row
				* (this.tileBoundingBox[zoomInterval].getMaxX()
						- this.tileBoundingBox[zoomInterval].getMinX() + 1) + col;

		this.offset = this.tileOffset[zoomInterval][id]
				+ this.mapFile.getAbsoluteStartPosition()[zoomInterval];
		// this.f.seek(this.offset);

		// System.out.print("Getting tile " + id + " (" + x + ", " + y + ") @ position " + this.offset);

		if (isEmptyTile(id, zoomInterval)) {
			// System.out.println(" -> empty");
			return null;
		}

		// TODO isEmptyTile <=> getTileSize == 0; merge functions

		// Read the tile
		int tileSize = getTileSize(id, zoomInterval);
		System.out.println("Reading: " + tileSize);
		byte[] tile = new byte[tileSize];
		this.f.seek(this.offset);
		this.f.read(tile, 0, tileSize);

		// System.out.println(" -> " + tileSize);

		return tile;
	}

	/**
	 * @param tileID
	 *            The tile's index position.
	 * @param zoomInterval
	 *            the zoom interval the tile is located in.
	 * @return true if the given tile is empty.
	 */
	private boolean isEmptyTile(int tileID, byte zoomInterval) {
		return getTileSize(tileID, zoomInterval) == 0;
	}

	private int getTileSize(int tileID, byte zoomInterval) {
		// Last tile?
		if (tileID == this.tileOffset[zoomInterval].length - 1) {
			return (int) (this.mapFile.getSubFileSize()[zoomInterval] - tileOffset[zoomInterval][tileID]);
		}

		return (int) (tileOffset[zoomInterval][tileID + 1] - tileOffset[zoomInterval][tileID]);
	}

	/**
	 * Returns the parser's <code>MapFile</code> object for retrieving the map file's meta information.
	 * 
	 * @return The parser's {@link MapFile}.
	 */
	public MapFile getMapFile() {
		return this.mapFile;
	}

	/**
	 * Tears down the parser and closes all of its handles.
	 * 
	 * @throws IOException
	 *             if the file cannot be closed.
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

	/**
	 * @param zoomInterval
	 *            The zoom interval that should be queried.
	 * @return Maximum x-coordinate for a tile on a given zoom interval.
	 */
	public int getMaxX(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMaxX();
	}

	/**
	 * @param zoomInterval
	 *            The zoom interval that should be queried.
	 * @return Minimum x-coordinate for a tile on a given zoom interval.
	 */
	public int getMinX(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMinX();
	}

	/**
	 * @param zoomInterval
	 *            The zoom interval that should be queried.
	 * @return Maximum y-coordinate for a tile on a given zoom interval.
	 */
	public int getMaxY(byte zoomInterval) {
		return this.tileBoundingBox[zoomInterval].getMaxY();
	}

	/**
	 * @param zoomInterval
	 *            The zoom interval that should be queried.
	 * @return Minimum y-coordinate for a tile on a given zoom interval.
	 */
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
			this.minX = x1;
			this.maxX = x2;
			this.minY = y1;
			this.maxY = y2;
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
