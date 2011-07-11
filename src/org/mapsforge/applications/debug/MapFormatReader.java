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

	public MapFormatReader(String path) throws FileNotFoundException {
		this.mapFile = mapFile;
		this.f = new RandomAccessFile(path, "r");

		this.buffer = new byte[4];
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

	private static String getHex(byte raw) {
		return MapFormatReader.getHex(new byte[] { raw });
	}

	private static int byteArrayToInt(byte[] bytes) {
		return (bytes[0] << 24) + ((bytes[1] & 0xff) << 16) + ((bytes[2] & 0xff) << 8)
				+ (bytes[3] & 0xff);
	}

	public void parseFile() throws IOException {
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

		// Projection Name (variable)
		System.out.println("Projection:");
		byte c;
		while ((c = this.f.readByte()) != '\0') {
			System.out.println(MapFormatReader.getHex(c) + " (" + (char) c + ")");
			this.offset++;
		}
		System.out.print('\n');

		// Tile size (2B)
		this.f.seek(offset);
		this.f.read(this.buffer, 0, 2);
		this.tileSize = this.buffer[0] << 8 + (this.buffer[1] & 0xff);
		this.offset += 2;
		System.out.println("Tile size: " + MapFormatReader.getHex(buffer[1]) + " ("
				+ this.tileSize + ")");

	}

	public byte[] getMagicData() {
		return magicData;
	}

	public int getHeaderLength() {
		return headerLength;
	}
}
