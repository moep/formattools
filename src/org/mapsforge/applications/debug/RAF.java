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

public class RAF extends RandomAccessFile {

	public RAF(File file, String mode) throws FileNotFoundException {
		super(file, mode);
	}

	public RAF(String path, String mode) throws FileNotFoundException {
		super(new File(path), mode);
	}

	@Override
	public void seek(long pos) throws IOException {
		super.seek(pos);

		System.out.println("[Offset: " + MapFormatReader.getHex((int) pos) + " (" + pos + ")]");
	}
}
