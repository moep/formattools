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

/**
 * 
 * @author Karsten Groll
 * 
 *         This class provides methods for efficient reading of files. Data will be read chunk-wise for
 *         less I/O.
 * 
 */
public class FileReader {
	/** file pointer position */
	private int position;
	/** Size of the chunk to be read in kB */
	private int chunkSize;

	/**
	 * 
	 * @param chunkSize
	 *            Size of chunk that will be read. You should match this to your filesystem's block
	 *            size.
	 */
	public FileReader(int chunkSize) {
		this.chunkSize = chunkSize;
	}
}
