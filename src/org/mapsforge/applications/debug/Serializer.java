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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

// TODO Can this be joined with org.mapsforge.android.maps.mapdatabase.ReadBuffer?
public class Serializer {
	private static final String CHARSET_UTF8 = "UTF-8";

	private ByteBuffer buffer;
	private byte[] bytes;

	public Serializer(byte[] data) {
		this.buffer = ByteBuffer.wrap(data);
		this.bytes = new byte[100];
	}

	public static long byteArrayToLong(byte[] bytes) {
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		return bb.getLong();
	}

	public int getNextInt() {
		return this.buffer.getInt();
	}

	public byte getNextByte() {
		return this.buffer.get();
	}

	// TODO Make UTF8-ready
	public String getNextString(int length) {
		// StringBuilder sb = new StringBuilder(length);
		// for (int i = 0; i < length; i++) {
		// sb.append((char) this.buffer.get());
		// }
		// return sb.toString();
		String ret;
		try {
			ret = new String(this.buffer.array(), this.buffer.position(), length, CHARSET_UTF8);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}

		skip(length);
		return ret;
	}

	public String getNextString() {
		int strlen = getNextVBEUInt();
		return getNextString(strlen);
	}

	public short getNextShort() {
		return this.buffer.getShort();
	}

	public long getNextLong() {
		return this.buffer.getLong();
	}

	public long getNextLong5() {

		for (int i = 0; i < 5; i++) {
			this.bytes[i] = this.buffer.get();
		}

		long ret = (this.bytes[0] & 0xffL) << 32 | (this.bytes[1] & 0xffL) << 24
				| (this.bytes[2] & 0xffL) << 16 | (this.bytes[3] & 0xffL) << 8
				| (this.bytes[4] & 0xffL);

		return ret;

	}

	public int getNextVBEUInt() {
		// byte shift = 0;
		// int ret = 0;
		// byte b;
		//
		// // Bytes with continuation bit (low order bytes)
		// while (((b = getNextByte()) & 0x80) != 0) {
		// ret |= (b & 0x7f) << shift;
		// shift += 7;
		// }
		//
		// // High order byte (last byte)
		// ret |= (b & 0x7f) << shift;
		//
		// return ret;

		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		byte b = getNextByte();
		while ((b & 0x80) != 0) {
			variableByteDecode |= (b & 0x7f) << variableByteShift;
			variableByteShift += 7;
			b = getNextByte();
		}

		// read the seven data bits from the last byte
		return variableByteDecode
				| (b << variableByteShift);
	}

	public int getNextVBESInt() {
		// // TODO test it
		// byte shift = 0;
		// int ret = 0;
		// byte b;
		//
		// // Bytes with continuation bit (low order bytes)
		// while (((b = getNextByte()) & 0x80) != 0) {
		// ret |= (b & 0x7f) << shift;
		// shift += 7;
		// }
		//
		// // High order byte (last byte)
		// ret |= (b & 0x7f) << shift;
		//
		// return (ret & 0x80000000) == 0 ? ret : -ret;

		int variableByteDecode = 0;
		byte variableByteShift = 0;

		// check if the continuation bit is set
		byte b = getNextByte();
		while ((b & 0x80) != 0) {
			variableByteDecode |= (b & 0x7f) << variableByteShift;
			variableByteShift += 7;
			b = getNextByte();
		}

		// read the six data bits from the last byte
		if ((b & 0x40) != 0) {
			// negative
			return -(variableByteDecode | ((b & 0x3f) << variableByteShift));
		}
		// positive
		return variableByteDecode
				| ((b & 0x3f) << variableByteShift);
	}

	/**
	 * Skips n bytes.
	 * 
	 * @param bytes
	 *            Number of bytes to be skipped.
	 */
	public void skip(int bytes) {
		this.buffer.position(this.buffer.position() + bytes);
	}

	/**
	 * 
	 * @return The buffer's position
	 */
	public int getPosition() {
		return this.buffer.position();
	}

}
