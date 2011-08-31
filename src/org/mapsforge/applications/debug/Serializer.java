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

import java.nio.ByteBuffer;

public class Serializer {
	private ByteBuffer buffer;
	private int offset;
	private byte[] tmp;

	public Serializer(byte[] data) {
		this.buffer = ByteBuffer.wrap(data);
		this.tmp = new byte[100];
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

	public String getNextString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(this.buffer.getChar());
		}
		return sb.toString();
	}

	public String getNextString() {
		int strlen = getNextVBEUInt();

		StringBuffer sb = new StringBuffer(strlen);
		for (int i = 0; i < strlen; i++) {
			sb.append(this.buffer.getChar());
		}

		return sb.toString();
	}

	public int getNextShort() {
		return this.buffer.getShort();
	}

	public long getNextLong() {
		return this.buffer.getLong();
	}

	public long getNextLong5() {

		for (int i = 0; i < 5; i++) {
			this.tmp[i] = this.buffer.get();
		}

		long ret = (this.tmp[0] & 0xffL) << 32 | (this.tmp[1] & 0xffL) << 24
				| (this.tmp[2] & 0xffL) << 16 | (this.tmp[3] & 0xffL) << 8
				| (this.tmp[4] & 0xffL);

		return ret;

	}

	public int getNextVBEUInt() {
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

	public int getNextVBESInt() {
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

}
