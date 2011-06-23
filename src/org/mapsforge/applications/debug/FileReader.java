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
import java.util.Arrays;

/**
 * 
 * @author Karsten Groll
 * 
 *         This class provides methods for efficient reading of files. Data will be read chunk-wise for
 *         less I/O.
 * 
 */
public class FileReader {
	/** relative position within the chunk */
	private int chunkPointer;
	private long filePointer;
	/** Size of the chunk to be read in bytes */
	private int chunkSize;

	private RandomAccessFile raFile;

	private byte[] buffer;
	private byte[] currentChunk;

	/**
	 * 
	 * @param chunkSize
	 *            A chunks size in bytes. A chunk is a group of bytes that will be read together, so you
	 *            should match this to your filesystem's block size.
	 * @param file
	 *            The file that should be read.
	 * @throws IOException
	 *             when file cannot be created.
	 * 
	 */
	public FileReader(int chunkSize, String file) throws IOException {
		this.chunkSize = chunkSize;

		raFile = new RandomAccessFile(file, "r");
		buffer = new byte[chunkSize];
		currentChunk = new byte[chunkSize];
	}

	/**
	 * @throws IOException
	 *             when file cannot be closed.
	 */
	public void foo() throws IOException {
		raFile.read(buffer, 0, chunkSize);
		System.out.println(Arrays.toString(buffer));
		raFile.close();
	}

	/**
	 * Returns one byte starting from the chunk's position pointer and increases the pointer by one.
	 * 
	 * @return the next byte from the pointer's position.
	 */
	public byte nextByte() {
		if (this.chunkPointer != this.chunkSize) {
			return buffer[chunkPointer];
		}
		// TODO
		return 0xf;
	}

	/**
	 * Returns the next chunk of bytes and advances the global position pointer.
	 * 
	 * @return the next chunk of bytes.
	 * 
	 * @throws IOException
	 *             when reading fails.
	 */
	public byte[] nextChunk() throws IOException {
		int bytesRead;
		this.raFile.seek(this.filePointer);
		bytesRead = this.raFile.read(this.currentChunk);

		if (bytesRead == -1) {
			return null;
		}

		this.filePointer += this.chunkSize;
		// System.out.println("Read " + bytesRead + " bytes.");

		return this.currentChunk;
	}

	public void close() throws IOException {
		this.raFile.close();
	}

	public static void readFilesFromDir(String dir, int numFiles) throws IOException {
		RandomAccessFile f;
		byte[] buffer = new byte[1024 * 300];

		for (int i = 0; i < 1000; i++) {
			System.out.println("reading " + i + ".bin");
			f = new RandomAccessFile(dir + i + ".bin", "r");
			f.read(buffer);
			f.close();
		}
	}
}
