package org.mapsforge.applications.debug;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

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

/**
 * Helper class for creating dummy files and directories.
 */
public class FileWriter {
	/**
	 * Creates a dummy file at a given path and file size.
	 * 
	 * @param path
	 *            path to the file that should
	 * @param fileSize
	 *            size of the file in kB.
	 * 
	 * @throws IOException
	 *             when the file cannot be created, written, or closed.
	 */
	public static void createFile(String path, int fileSize) throws IOException {
		RandomAccessFile f = new RandomAccessFile(path, "rw");

		// Create random data
		byte[] buffer = new byte[1024];
		for (int i = 0; i < 1024; i++) {
			buffer[i] = (byte) 0xda;
		}

		// write data
		for (int i = 1; i <= fileSize; i++) {
			f.write(buffer, 0, 1024);
		}

		f.close();
	}

	/**
	 * 
	 * @param path
	 * @param numFiles
	 * @param fileSize
	 */
	public static void createDirWithFiles(String path, int numFiles, int fileSize) throws IOException {
		File df = new File(path);
		System.out.println("mkdir " + path + ": " + df.mkdir());

		byte[] buffer = new byte[1024];
		for (int i = 0; i < 1024; i++) {
			buffer[i] = (byte) 0xfe;
		}

		RandomAccessFile f;
		for (int i = 0; i < numFiles; i++) {
			f = new RandomAccessFile(path + i + ".bin", "rw");
			for (int j = 0; j < fileSize; j++) {
				f.write(buffer);
			}
		}

	}

	public static void createDirStructureWithFiles(String path)
			throws IOException {
		// Create directories
		new File(path + "/bzs7/").mkdirs();

		// 30k fake data
		byte[] data = new byte[1024 * 5];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) 0x88;
		}

		RandomAccessFile f;
		for (int x = 0; x < 300; x++) {
			new File(path + "/bzs7/" + x + "/").mkdirs();
			for (int y = 0; y < 300; y++) {
				f = new RandomAccessFile(path + "/bzs7/" + x + "/" + y + ".y", "rw");
				f.write(data);
				f.close();
			}
		}
	}
}
