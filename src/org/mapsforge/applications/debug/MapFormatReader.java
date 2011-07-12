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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Class for reading the current map format.
 * 
 * @author Karsten
 * 
 */
public class MapFormatReader {
	private static final String HEXES = "0123456789ABCDEF";

	private File mapFile;
	private RandomAccessFile f;
	private byte[] buffer;
	private long offset = 0;

	private byte[] magicData;
	private int headerLength;
	private int fileVersion;
	private byte numZoomIntervals;
	private byte flags;
	private int tileSize;
	private int projectionNameStringLength;
	private String projectionName;
	private int maxLat;
	private int minLon;
	private int minLat;
	private int maxLon;

	private int mapStartLon;

	private int mapStartLat;

	private long dateOfCreation;

	private int numPOIMappings;

	public MapFormatReader(String path) throws FileNotFoundException {
		this.mapFile = mapFile;
		this.f = new RandomAccessFile(path, "r");

		this.buffer = new byte[1024];
	}

	public void close() throws IOException {
		if (this.f != null)
			this.f.close();
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

	private static String getHex(int raw) {
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
		return (bytes[0] << 24) + ((bytes[1] & 0xff) << 16) + ((bytes[2] & 0xff) << 8)
				+ (bytes[3] & 0xff);
	}

	private static long byteArrayToLong(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return bb.getLong();
	}

	public void parseFile() throws IOException {
		parseHeader();
	}

	private void parseHeader() throws IOException {
		System.out.println("------ H E A D E R ------");

		// Magic bytes (20B)
		this.magicData = new byte[20];
		this.f.read(this.magicData, 0, 20);
		this.offset += 20;
		System.out.println("Magic bytes: " + MapFormatReader.getHex(this.magicData));

		// Header Length (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.headerLength = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("Header lenght: " + MapFormatReader.getHex(this.headerLength) + " ("
				+ this.headerLength + ")");

		// File version (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.fileVersion = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("File version: " + MapFormatReader.getHex(this.fileVersion) + " ("
				+ this.fileVersion + ")");

		// Flags (1B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 1);
		this.flags = this.buffer[0];
		this.offset += 1;
		System.out.println("Flags: " + MapFormatReader.getHex(this.flags));

		// Number of zoom intervals (1B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 1);
		this.numZoomIntervals = this.buffer[0];
		this.offset += 1;
		System.out.println("#zoom intervals: " + MapFormatReader.getHex(this.numZoomIntervals) + " ("
				+ this.numZoomIntervals + ")");

		// Projection Name length (1B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 1);
		this.projectionNameStringLength = this.buffer[0];
		this.offset += 1;
		System.out.println("Projection name string length: "
				+ MapFormatReader.getHex(this.projectionNameStringLength) + " ("
				+ this.projectionNameStringLength + ")");

		// Projection name (variable)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, this.projectionNameStringLength);
		this.buffer[this.projectionNameStringLength] = '\0';
		this.projectionName = new String(this.buffer);
		this.offset += this.projectionNameStringLength;
		System.out.println("Projection name: " + this.projectionName);

		// Tile size (2B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 2);
		this.tileSize = (this.buffer[0] << 8) + (this.buffer[1] & 0xff);
		this.offset += 2;
		System.out.println("Tile size: "
				+ MapFormatReader.getHex(Arrays.copyOfRange(this.buffer, 0, 2)) + " ("
				+ this.tileSize + ")");

		// Maximum latitude (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.maxLat = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("Maximum latitude: " + MapFormatReader.getHex(this.maxLat) + " ("
				+ this.maxLat + ")");

		// Minimum longitude (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.minLon = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("Minimum longitude: " + MapFormatReader.getHex(this.minLon) + " ("
				+ this.minLon + ")");

		// Minimum latitude (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.minLat = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("Minimum latitude: " + MapFormatReader.getHex(this.minLat) + " ("
				+ this.minLat + ")");

		// Maximum longitude (4B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 4);
		this.maxLon = MapFormatReader.byteArrayToInt(buffer);
		this.offset += 4;
		System.out.println("Maximum longitude: " + MapFormatReader.getHex(this.maxLon) + " ("
				+ this.maxLon + ")");

		// Map start position (8B)
		if ((this.flags & 0x02) == 0x02) {
			// Map start longitude (4B)
			this.f.seek(offset);
			this.f.read(this.buffer, 0, 4);
			this.mapStartLon = MapFormatReader.byteArrayToInt(buffer);
			this.offset += 4;
			System.out.println("Map start longitude: " + MapFormatReader.getHex(this.mapStartLon)
					+ " ("
					+ this.mapStartLon + ")");

			// Map start latidute (4B)
			this.f.seek(offset);
			this.f.read(this.buffer, 0, 4);
			this.mapStartLat = MapFormatReader.byteArrayToInt(buffer);
			this.offset += 4;
			System.out.println("Map start latitude: " + MapFormatReader.getHex(this.mapStartLat)
					+ " ("
					+ this.mapStartLat + ")");
		}

		// Date of creation (8B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 8);
		this.dateOfCreation = MapFormatReader.byteArrayToLong(buffer);
		this.offset += 8;
		System.out.println("Date of creation: " + MapFormatReader.getHex(this.dateOfCreation)
				+ " ("
				+ this.dateOfCreation + ")");

		// POI tag mapping (variable)
		// amount of mappings (2B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 2);
		this.numPOIMappings = (this.buffer[0] << 8) + (this.buffer[1] & 0xff);
		this.offset += 2;
		System.out.println("Number of POI mappings: "
				+ MapFormatReader.getHex(this.buffer[1]) + " ("
				+ this.numPOIMappings + ")");

		// POI tag mapping (variable)
		int strLen;
		int tagID;
		String tagName;
		System.out.println("Reading mappings: ");
		for (int i = 0; i < this.numPOIMappings; i++) {
			// string length
			this.f.seek(offset);
			this.f.read(this.buffer, 0, 1);
			strLen = this.buffer[0];
			this.offset += 1;

			// tag name
			this.f.seek(offset);
			this.f.read(this.buffer, 0, strLen);
			this.buffer[strLen] = '\0';
			tagName = new String(this.buffer).substring(0, strLen);
			this.offset += strLen;

			// tag ID
			this.f.seek(offset);
			this.f.read(this.buffer, 0, 2);
			tagID = (this.buffer[0] << 8) + (this.buffer[1] & 0xff);
			this.offset += 2;

			System.out.println("  " + tagName + " => " + tagID);
		}
	}

}
