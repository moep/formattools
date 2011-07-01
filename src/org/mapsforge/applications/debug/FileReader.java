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

/**
 * 
 * @author Karsten Groll
 * 
 *         This class provides methods for efficient reading of files. Data will be read chunk-wise for
 *         less I/O.
 * 
 */
public class FileReader {
	/**
	 * Reads a given file 1000 times.
	 * 
	 * @param pathToFile
	 *            file that should be read.
	 * @throws IOException
	 *             if file cannot be read.
	 */
	public static void readFile(String pathToFile) throws IOException {
		RandomAccessFile f;
		byte[] buffer = new byte[1024 * 30];

		for (int i = 0; i < 1000; i++) {
			// System.out.println("reading " + path);
			f = new RandomAccessFile(pathToFile, "r");
			f.read(buffer);
			f.close();
		}
	}

}
