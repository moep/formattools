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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.mapsforge.core.MercatorProjection;

/**
 * Class for reading the current map format.
 * 
 * @author Karsten
 * 
 */
public class MapFormatReader {
	private static final String HEXES = "0123456789ABCDEF";

	private RandomAccessFile f;
	private byte[] buffer;
	private long offset = 0;
	/** for switching between a long and double representation of longitude and latitude values */
	private static final double COORDINATES_FACTOR = 1000000.0;

	private MapFile mapFile;

	private byte[] magicByte;
	private int headerSize;
	private int fileVersion;
	private byte flags;
	private byte amountOfZoomIntervals;
	private String projection;
	private int tileSize;

	// Bounding box
	private int maxLat;
	private int minLon;
	private int minLat;
	private int maxLon;

	// Map start position
	private int mapStartLon;
	private int mapStartLat;
	private long dateOfCreation;

	// POI tag mapping
	private int numPOIMappings;

	// Ways tag mapping
	private int numWayTagMappings;

	private String comment;

	// Zoom interval configuration
	private byte[] baseZoomLevel;
	private byte[] minimalZoomLevel;
	private byte[] maximalZoomLevel;
	private long[] absoluteStartPosition;
	private long[] subFileSize;
	private ArrayList<Long>[] tileOffsets;

	/**
	 * The constructor.
	 * 
	 * @param path
	 *            Path to the map file that should be read.
	 * @throws FileNotFoundException
	 *             when file cannot be found.
	 */
	public MapFormatReader(String path) throws FileNotFoundException {
		this.f = new RandomAccessFile(path, "r");
		// this.f = new RAF(path, "r");

		this.buffer = new byte[1024];
		this.mapFile = new MapFile();
	}

	/**
	 * Parses the map file and stores its structure.
	 * 
	 * @throws IOException
	 *             when the file cannot be read.
	 */
	public MapFile parseFile() throws IOException {
		parseHeader();

		// this.tileOffsets = new ArrayList[this.amountOfZoomIntervals];
		// parseSubFile((byte) 0);

		// for (byte i = 0; i < this.numZoomIntervals; i++) {
		// parseSubFile((byte) i);
		// }

		return this.mapFile;
	}

	/**
	 * Closes the file handler.
	 * 
	 * @throws IOException
	 *             when your disk is going to die.
	 */
	public void close() throws IOException {
		if (this.f != null)
			this.f.close();
	}

	private void parseHeader() throws IOException {
		// Magic bytes (20B)
		this.magicByte = new byte[20];
		this.f.read(this.magicByte, 0, 20);
		this.offset += 20;
		this.mapFile.setMagicByte(this.magicByte);

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
		if ((this.flags & 0x40) != 0) {
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
		System.out.println("Reading way tag mappings: ");
		for (int i = 0; i < this.numWayTagMappings; i++) {

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
		for (int i = 0; i < this.amountOfZoomIntervals; i++) {
			this.mapFile.setZoomIntervalConfiguration(i, getNextByte(), getNextByte(), getNextByte(),
					getNextLong5(), getNextLong5());
		}

	}

	private void parseSubFile(byte zoomInterval) throws IOException {
		System.out.println("------ S U B F I L E " + zoomInterval + " ------");

		this.offset = this.absoluteStartPosition[zoomInterval];
		this.f.seek(this.offset);

		// Calculate number of blocks in the file index
		// x,y tile coordinates of the first and last tile in the index
		long firstX = MercatorProjection.longitudeToTileX(this.minLon
				/ MapFormatReader.COORDINATES_FACTOR, this.baseZoomLevel[zoomInterval]);
		long lastX = MercatorProjection.longitudeToTileX(this.maxLon
				/ MapFormatReader.COORDINATES_FACTOR, this.baseZoomLevel[zoomInterval]);
		long firstY = MercatorProjection.latitudeToTileY(this.minLat
				/ MapFormatReader.COORDINATES_FACTOR, this.baseZoomLevel[zoomInterval]);
		long lastY = MercatorProjection.latitudeToTileY(this.maxLat
				/ MapFormatReader.COORDINATES_FACTOR, this.baseZoomLevel[zoomInterval]);

		long numBlocks = (Math.abs(lastX - firstX) + 1) * (Math.abs(lastY - firstY) + 1);
		System.out.println("Number of blocks in this subfile: " + numBlocks);

		// Tile index segment
		// index signature (16B, optional)
		if ((this.flags & 0x80) != 0) {
			this.f.read(this.buffer, 0, 16);
			this.offset += 16;
			System.out.println("Index signature: " + new String(this.buffer, 0, 16));
		}

		// Get all tile offsets
		// TODO correct handling of water tiles
		System.out.println("Tile# \tOffset \t\t\t\tWater bit");
		long tileOffset;
		this.tileOffsets[zoomInterval] = new ArrayList<Long>();
		for (long i = 0; i < numBlocks; i++) {
			tileOffset = getNextLong5();
			this.tileOffsets[zoomInterval].add(tileOffset);
			System.out.println(i + ": \t" + getHex(tileOffset) + " (" + tileOffset + ")\t"
					+ ((tileOffset & 0x000000010000000000L) != 0));
		}

		// Headers, POIs and ways
		byte specialByte;
		byte numTags;
		byte poiFlags;
		for (long i = 0; i < numBlocks; i++) {
			System.out.println("-- B L O C K " + i + " --");

			// H E A D E R

			// Tile signature (32B, optional)
			// ###TileStart
			if ((this.flags & 0x80) != 0) {
				this.f.seek(this.offset);
				this.f.read(this.buffer, 0, 32);
				this.offset += 32;
				System.out.println(new String(this.buffer, 0, 32));
			}

			// Zoom table (variable)
			System.out.println("Zoom level \t#POIs\t#Ways");
			for (int row = this.minimalZoomLevel[zoomInterval]; row <= this.maximalZoomLevel[zoomInterval]; row++) {
				// TODO save values
				System.out.println(row + "\t\t" + getNextDword() + "\t" + getNextDword());
			}

			// OLD
			// this.offset += (this.maximalZoomLevel[zoomInterval] - this.minimalZoomLevel[zoomInterval]
			// + 1) * 4;
			// this.f.seek(this.offset);
			// !OLD

			// First way offset (VBE-U)
			// TODO save data
			System.out.println("First way offset: " + getNextVBEUInt());

			// P O I s
			// TODO for loop (how?)

			for (int poi = 0; poi < 3; poi++) {
				// POI signature (32B, optional)
				if ((this.flags & 0x80) != 0) {
					this.f.seek(this.offset);
					this.f.read(this.buffer, 0, 32);
					this.offset += 32;
					System.out.println(new String(this.buffer, 0, 32));
				}

				// Position (2 * VBE-S)
				// TODO save data
				System.out.println("lat diff: " + getNextVBESInt());
				System.out.println("lon diff: " + getNextVBESInt());

				// Special byte (1B)
				specialByte = getNextByte();
				System.out.println("Special byte: " + getHex(specialByte));
				numTags = (byte) (specialByte & 0x0f);
				System.out.println("#Tags: " + getHex(numTags) + " (" + numTags + ")");

				// POI tags (variable)
				for (byte j = 0; j < numTags; j++) {
					System.out.println("Tag: " + getNextVBEUInt());
				}

				// Flags (1B)
				poiFlags = getNextByte();

				// POI name (variable, optional)
				if ((poiFlags & 0x80) != 0) {
					// TODO store variable
					System.out.println("POI Name: " + getNextString());
				}

				// POI elevation (VBE-S, optional)
				if ((poiFlags & 0x40) != 0) {
					// TODO store value
					System.out.println("Elevation: " + getNextVBESInt());
				}

				// House number (String, optional)
				if ((poiFlags & 0x20) != 0) {
					// TODO store value
					System.out.println("House number: " + getNextString());
				}
			}

			// W A Y S
			for (int way = 0; way < 14099; way++) {

				// Way signature (32B, optional)
				// TODO save data
				if ((this.flags & 0x80) != 0) {
					this.f.seek(this.offset);
					this.f.read(this.buffer, 0, 32);
					this.offset += 32;
					System.out.println(new String(this.buffer, 0, 32));
				}

				// Way size (VBE-U)
				// TODO save data
				System.out.println("Way size: " + getNextVBEUInt());

				// Sub tile bitmap (2B)
				// TODO save data
				System.out.println("Sub tile bitmap: " + getNextDword());

				// Special byte 1 (1B)
				// TODO save data
				byte waySpecialByte1 = getNextByte();
				System.out.println("Special byte 1: " + getHex(waySpecialByte1));

				// Special byte 1 (1B)
				// TODO save data
				System.out.println("Special byte 2: " + getHex(getNextByte()));

				// Way type bitmap (1B)
				// TODO save data
				System.out.println("Way type bitmap: " + getHex(getNextByte()));

				// Tag ID (n * VBE-U)
				// for each tag ...
				System.out.println("Tag \t Tag ID");
				for (byte tag = 0; tag < (waySpecialByte1 & 0x0f); tag++) {
					// TODO save data
					System.out.println(tag + "\t" + getHex(getNextVBEUInt()));
				}

				// Way node amount (VBE-U)
				// TODO save data
				System.out.println("Amount of way nodes: " + getNextVBEUInt());

				// First way node (2*VBE-S)
				// TODO save data
				System.out.println("First way node lat diff:" + getNextVBESInt());
				System.out.println("First way node lon diff:" + getNextVBESInt());

				// Way nodes (2*VBE-S)
				// TODO save data
				System.out.println("Way node lat diff:" + getNextVBESInt());
				System.out.println("Way node lon diff:" + getNextVBESInt());

				// Flags
				// TODO save data
				byte wayFlags = getNextByte();
				System.out.println("Flags: " + getHex(wayFlags));

				// Name (String, optional)
				// TODO save data
				if ((wayFlags & 0x80) != 0) {
					System.out.println("Name: " + getNextString());
				}

				// Reference (String, optional)
				// TODO save data
				if ((wayFlags & 0x40) != 0) {
					System.out.println("Reference: " + getNextString());
				}

				// Label position (2*VBE-S)
				// TODO save data
				if ((wayFlags & 0x20) != 0) {
					System.out.println("Way node lat diff:" + getNextVBESInt());
					System.out.println("Way node lon diff:" + getNextVBESInt());
				}

				System.out.println("OFFSET: " + this.offset);

			}

		}

	}

	static String getHex(byte[] raw) {
		if (raw == null) {
			return null;
		}
		final StringBuilder hex = new StringBuilder(2 * raw.length + 2);
		hex.append("0x");
		for (final byte b : raw) {
			hex.append(MapFormatReader.HEXES.charAt((b & 0xF0) >> 4))
					.append(MapFormatReader.HEXES.charAt((b & 0x0F)));
		}
		return hex.toString();
	}

	static String getHex(int raw) {
		return MapFormatReader.getHex(new byte[] { (byte) (raw >>> 24), (byte) (raw >>> 16),
				(byte) (raw >>> 8), (byte) (raw & 0xff) });
	}

	static String getHex(long raw) {
		return MapFormatReader.getHex(new byte[] { (byte) (raw >>> 56), (byte) (raw >>> 48),
				(byte) (raw >>> 40), (byte) (raw >>> 32), (byte) (raw >>> 24), (byte) (raw >>> 16),
				(byte) (raw >>> 8), (byte) (raw & 0xff) });
	}

	static String getHex(byte raw) {
		return MapFormatReader.getHex(new byte[] { raw });
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
		return MapFormatReader.byteArrayToInt(this.buffer);
	}

	private byte getNextByte() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 1);
		this.offset += 1;

		return this.buffer[0];
	}

	private String getNextString() throws IOException {
		int strlen = getNextByte();
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, strlen);
		this.buffer[strlen] = '\0';
		this.offset += strlen;

		return new String(this.buffer).substring(0, strlen);
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

		return MapFormatReader.byteArrayToLong(buffer);
	}

	private long getNextLong5() throws IOException {
		this.f.seek(this.offset);
		this.f.read(this.buffer, 0, 5);
		this.offset += 5;

		return (this.buffer[0] & 0xffL) << 32 | (this.buffer[1] & 0xffL) << 24
				| (this.buffer[2] & 0xffL) << 16 | (this.buffer[3] & 0xffL) << 8
				| (this.buffer[4] & 0xffL);

	}

	private int getNextVBEUInt() throws IOException {
		int ret = 0x0;
		byte b;
		for (int i = 0; i < 4; i++) {
			b = getNextByte();
			ret |= b & 0x7f;

			if ((b & 0x80) == 0) {
				break;
			}

			ret = ret << 7;

		}

		return ret;
	}

	private int getNextVBESInt() throws IOException {
		// TODO implement

		f.seek(this.offset);
		this.f.read(this.buffer, 0, 4);
		for (int i = 0; i < 4; i++) {
			this.offset += 1;
			if ((this.buffer[i] & 0x80) == 0)
				break;
		}

		return 1337;
	}
}
