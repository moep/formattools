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


/**
 * A buffered implementation of Java's <code>RandomAccessFile</code>. It reads data in huge blocks to
 * reduce overhead from random I/O. It's optimized for {@link MapFormatReader} only. Reading data bigger
 * than the block size might fail.
 * 
 * @author Karsten Groll
 * 
 */
public class BufferedRandomAccessFile extends RandomAccessFile {

	private static final int KIBI_BYTE_FACTOR = 1024;
	private int BUFFER_SIZE;
	/** Offset within the buffer. */
	private int bufferOffset;
	/** The buffer we store the data in. */
	private byte[] buffer;
	/** The number of the block that is buffered. */
	private int blockID;

	/**
	 * 
	 * @param name
	 *            The path to the file that should be used.
	 * @param mode
	 *            The mode (r, w, rw).
	 * @param bufferSize
	 *            Amount of bytes in KiB that should be read block-wise.
	 * @throws FileNotFoundException
	 *             when the file does not exist.
	 */
	public BufferedRandomAccessFile(String name, String mode, int bufferSize)
			throws FileNotFoundException {
		this(name, mode);

		this.BUFFER_SIZE = bufferSize * KIBI_BYTE_FACTOR;
		this.buffer = new byte[BUFFER_SIZE];
		this.blockID = -1;

	}

	private BufferedRandomAccessFile(String name, String mode) throws FileNotFoundException {
		super(name, mode);
	}

	/**
	 * Reads the next block of data into the buffer.
	 * 
	 * @throws IOException
	 *             if file cannot be read.
	 */
	private void readBlock() throws IOException {
		// Goto block offset
		super.seek(this.blockID * BUFFER_SIZE);

		// Clear the buffer
		this.buffer = null;
		System.gc();
		this.buffer = new byte[BUFFER_SIZE];

		// Read the block
		super.read(this.buffer, 0, BUFFER_SIZE);
	}

	@Override
	public void seek(long pos) throws IOException {
		// Set the block offset pointer
		this.bufferOffset = (int) (pos % BUFFER_SIZE);

		// Check if we have to load a new block
		if (this.blockID != (int) (pos / BUFFER_SIZE)) {
			this.blockID = (int) (pos / BUFFER_SIZE);
			readBlock();
		}
	}

	/**
	 * Reads up to <code>len</code> bytes from the buffer. If the buffer end is reached a new block will
	 * be read into the buffer. This method blocks if you try to read beyond the last block.
	 * 
	 * @throws IOException
	 *             If the file cannot be read.
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		assert off >= 0;
		assert len >= 0;

		// Check if the desired block can be read at once
		if (off + len + this.bufferOffset <= BUFFER_SIZE) {
			// Copy contents from buffer to b starting from bufferOffset + provided offset
			System.arraycopy(this.buffer, this.bufferOffset + off, b, 0, len);
		} else {
			// Amount of bytes from the offset position until the buffer's end
			int numFirstBytes = BUFFER_SIZE - this.bufferOffset + off;

			// Copy everything until the buffer's end
			System.arraycopy(this.buffer, this.bufferOffset + off, b, 0, numFirstBytes);

			// Read the remaining bytes
			super.seek(BUFFER_SIZE * (blockID + 1));
			super.read(b, off + numFirstBytes, len - numFirstBytes);
		}

		return -1;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Buffer offset: ").append(this.bufferOffset).append(" Block ID: ")
				.append(this.blockID);

		return sb.toString();
	}

	/**
	 * 
	 * @return The buffer's content as a hex string starting with "0x".
	 */
	public String getDebugDataString() {
		StringBuilder sb = new StringBuilder();
		sb.append(MapFormatReader.getHex(this.buffer)).append(MapFile.NL);
		sb.append("  ");
		for (int i = 0; i < bufferOffset; i++) {
			sb.append("  ");
		}
		sb.append("^^");
		return sb.toString();
	}

}
