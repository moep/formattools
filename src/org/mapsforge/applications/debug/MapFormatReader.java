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
import java.util.Arrays;

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

	private byte[] magicData;
	private int headerLength;
	private int fileVersion;
	private byte numZoomIntervals;
	private byte flags;
	private int tileSize;
	private String projectionName;
	private int maxLat;
	private int minLon;
	private int minLat;
	private int maxLon;
	private int mapStartLon;
	private int mapStartLat;
	private long dateOfCreation;
	private int numPOIMappings;
	private int numWayTagMappings;
	private String comment;
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
	}

	/**
	 * Parses the map file and stores its structure.
	 * 
	 * @throws IOException
	 *             when the file cannot be read.
	 */
	public void parseFile() throws IOException {
		parseHeader();

		this.tileOffsets = new ArrayList[this.numZoomIntervals];

		parseSubFile((byte) 0);
		// for (byte i = 0; i < this.numZoomIntervals; i++) {
		// parseSubFile((byte) i);
		// }
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
		System.out.println("------ H E A D E R ------");

		// Magic bytes (20B)
		this.magicData = new byte[20];
		this.f.read(this.magicData, 0, 20);
		this.offset += 20;
		System.out.println("Magic bytes: " + MapFormatReader.getHex(this.magicData));

		// Header Length (4B)
		this.headerLength = getNextInt();
		System.out.println("Header lenght: " + MapFormatReader.getHex(this.headerLength) + " ("
				+ this.headerLength + ")");

		// File version (4B)
		this.fileVersion = getNextInt();
		System.out.println("File version: " + MapFormatReader.getHex(this.fileVersion) + " ("
				+ this.fileVersion + ")");

		// Flags (1B)
		this.flags = getNextByte();

		System.out.println("Flags: " + MapFormatReader.getHex(this.flags));

		// Number of zoom intervals (1B)
		this.numZoomIntervals = getNextByte();
		System.out.println("#zoom intervals: " + MapFormatReader.getHex(this.numZoomIntervals) + " ("
				+ this.numZoomIntervals + ")");

		// Projection name (variable)
		this.projectionName = getNextString();
		System.out.println("Projection name: " + this.projectionName);

		// Tile size (2B)
		this.tileSize = getNextDword();
		System.out.println("Tile size: "
				+ MapFormatReader.getHex(Arrays.copyOfRange(this.buffer, 0, 2)) + " ("
				+ this.tileSize + ")");

		// Maximum latitude (4B)
		this.maxLat = getNextInt();
		System.out.println("Maximum latitude: " + MapFormatReader.getHex(this.maxLat) + " ("
				+ this.maxLat + ")");

		// Minimum longitude (4B)
		this.minLon = getNextInt();
		System.out.println("Minimum longitude: " +
				MapFormatReader.getHex(this.minLon) + " ("
				+ this.minLon + ")");

		// Minimum latitude (4B)
		this.minLat = getNextInt();
		System.out.println("Minimum latitude: " + MapFormatReader.getHex(this.minLat) + " ("
				+ this.minLat + ")");

		// Maximum longitude (4B)
		this.maxLon = getNextInt();
		System.out.println("Maximum longitude: " + MapFormatReader.getHex(this.maxLon) + " ("
				+ this.maxLon + ")");

		// Map start position (8B)
		if ((this.flags & 0x40) != 0) {
			// Map start latidute (4B)
			this.mapStartLat = getNextInt();
			System.out.println("Map start latitude: " + MapFormatReader.getHex(this.mapStartLat)
					+ " ("
					+ this.mapStartLat + ")");

			// Map start longitude (4B)
			this.mapStartLon = getNextInt();
			System.out.println("Map start longitude: " + MapFormatReader.getHex(this.mapStartLon)
					+ " ("
					+ this.mapStartLon + ")");
		}

		// Date of creation (8B)
		this.dateOfCreation = getNextLong();
		System.out.println("Date of creation: " + MapFormatReader.getHex(this.dateOfCreation)
				+ " ("
				+ this.dateOfCreation + ")");

		// POI tag mapping (variable)
		// amount of mappings (2B)
		this.numPOIMappings = getNextDword();
		System.out.println("Number of POI mappings: "
				+ MapFormatReader.getHex(this.buffer[1]) + " ("
				+ this.numPOIMappings + ")");

		// POI tag mapping (variable)
		int tagID;
		String tagName;
		System.out.println("Reading POI mappings: ");
		for (int i = 0; i < this.numPOIMappings; i++) {

			// Tag name (variable)
			tagName = getNextString();

			// tag ID (2B)
			tagID = getNextDword();
			System.out.println("  " + tagName + " => " + tagID);
		}

		// Way tag mapping (variable)
		// amount of mappings (2B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 2);
		this.numWayTagMappings = (this.buffer[0] << 8) + (this.buffer[1] & 0xff);
		this.offset += 2;
		System.out.println("Number of Way Tag mappings: "
				+ MapFormatReader.getHex(this.buffer[1]) + " ("
				+ this.numPOIMappings + ")");

		// Way tag mapping (variable)
		System.out.println("Reading way tag mappings: ");
		for (int i = 0; i < this.numWayTagMappings; i++) {

			// tag name (variable)
			tagName = getNextString();

			// tag ID (2B)
			tagID = getNextDword();
			System.out.println("  " + tagName + " => " + tagID);
		}

		// Comment (variable)
		this.comment = getNextString();
		System.out.println("Comment: " + this.comment);

		// Zoom interval configuration (variable)
		this.baseZoomLevel = new byte[this.numZoomIntervals];
		this.minimalZoomLevel = new byte[this.numZoomIntervals];
		this.maximalZoomLevel = new byte[this.numZoomIntervals];
		this.absoluteStartPosition = new long[this.numZoomIntervals];
		this.subFileSize = new long[this.numZoomIntervals];

		for (int i = 0; i < this.numZoomIntervals; i++) {
			System.out.println("Zoomlevel: " + i);

			// Base zoom level
			this.baseZoomLevel[i] = getNextByte();
			System.out.println("  Base zoom level: " + this.baseZoomLevel[i]);

			// Minimal zoom Level
			this.minimalZoomLevel[i] = getNextByte();
			System.out.println("  Minimal zoom level: " + this.minimalZoomLevel[i]);

			// Maximal zoom Level
			this.maximalZoomLevel[i] = getNextByte();
			System.out.println("  Maximal zoom level: " + this.maximalZoomLevel[i]);

			// TODO Absolute start position
			this.absoluteStartPosition[i] = getNextLong5();
			System.out.println("  Absolute start position: " + getHex(this.absoluteStartPosition[i])
					+ " (" + this.absoluteStartPosition[i] + ")");

			// TODO Subfile size
			this.subFileSize[i] = getNextLong5();
			System.out.println("  Subfile size: " + getHex(this.subFileSize[i]) + " ("
					+ this.subFileSize[i] + ")");
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

			// W A Y S
			// TODO loop

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

	private static String getHex(byte[] raw) {
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

	private static String getHex(long raw) {
		return MapFormatReader.getHex(new byte[] { (byte) (raw >>> 56), (byte) (raw >>> 48),
				(byte) (raw >>> 40), (byte) (raw >>> 32), (byte) (raw >>> 24), (byte) (raw >>> 16),
				(byte) (raw >>> 8), (byte) (raw & 0xff) });
	}

	private static String getHex(byte raw) {
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
		// TODO implement

		f.seek(this.offset);
		this.f.read(this.buffer, 0, 4);
		for (int i = 0; i < 4; i++) {
			this.offset += 1;
			if ((this.buffer[i] & 0x80) == 0)
				break;
		}

		return 42;
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
