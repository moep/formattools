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
package org.mapsforge.applications.debug.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import SQLite.Database;

@Deprecated
public class TileSQLiteWriter {
	private static final int BATCH_SIZE = 10000;
	private Connection conn = null;
	private Database db = null;
	private PreparedStatement[] insertStatements;
	// e.g.: zoomLevelMapping[baseZoomLevel = 1] -> zoom level 14
	private byte[] zoomLevelMapping;
	private boolean useCompression;
	private int numInserts[];

	// Debug only
	private int savings = 0;

	public TileSQLiteWriter(String path, byte[] zoomLevelMapping, boolean useCompression) {
		this.zoomLevelMapping = zoomLevelMapping;
		this.numInserts = new int[zoomLevelMapping.length];
		this.insertStatements = new PreparedStatement[zoomLevelMapping.length];
		this.useCompression = useCompression;
		try {
			Class.forName("SQLite.JDBC");
			this.conn = DriverManager.getConnection("jdbc:sqlite:/" + path);
			this.conn.setAutoCommit(false);
			createDB();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void createDB() {
		System.out.println("Creating database");
		try {
			Statement stmt = conn.createStatement();
			for (int i = 0; i < zoomLevelMapping.length; i++) {
				stmt.execute("DROP TABLE IF EXISTS tiles_" + i);
				stmt.execute("CREATE TABLE tiles_" + i + " (id INTEGER, data BLOB, PRIMARY KEY (id));");

				this.insertStatements[i] = conn.prepareStatement("INSERT INTO tiles_" + i + " VALUES (?, ?)");
			}

			this.conn.commit();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insert(byte[] data, int x, int y, byte zoomLevel) {
		int pos = y * (0x1 << this.zoomLevelMapping[zoomLevel]) + x;
		try {
			this.insertStatements[zoomLevel].setInt(1, pos);

			if (!this.useCompression) {
				this.insertStatements[zoomLevel].setBytes(2, data);
			} else {
				this.insertStatements[zoomLevel].setBytes(2, gzip(data));
			}

			this.insertStatements[zoomLevel].addBatch();

			++this.numInserts[zoomLevel];

			if (this.numInserts[zoomLevel] % BATCH_SIZE == 0) {
				commitBatch(zoomLevel);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void commitBatch(byte zoomLevel) {
		this.numInserts[zoomLevel] = 0;
		try {
			this.insertStatements[zoomLevel].executeBatch();
			this.conn.commit();
			this.insertStatements[zoomLevel].clearBatch();
		} catch (SQLException e) {
			System.err.println(e.getMessage());
			System.exit(0);
		}

	}

	public void finish() {

		System.out.println("\r\nFinal commit");
		System.out.println("Savings: " + savings);
		for (byte i = 0; i < this.insertStatements.length; i++) {

			try {
				this.insertStatements[i].executeBatch();
				this.conn.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}

	private byte[] deflate(byte[] data) {
		Deflater deflater = new Deflater(Deflater.BEST_SPEED);
		deflater.setStrategy(Deflater.HUFFMAN_ONLY);
		deflater.setInput(data);
		deflater.finish();

		ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
		int totalBytes = 0;
		int bytes = -1;
		byte[] buffer = new byte[1024 * 10];
		while (!deflater.finished()) {
			bytes = deflater.deflate(buffer);
			totalBytes += bytes;
			os.write(buffer, 0, bytes);
		}

		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		savings += data.length - totalBytes;

		return os.toByteArray();
	}

	private byte[] gzip(byte data[]) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		GZIPOutputStream gzos = null;
		try {
			gzos = new GZIPOutputStream(os);
			gzos.write(data);
			gzos.finish();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gzos != null) {
				try {
					gzos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		System.out.println("Compression ratio: " + (1.0f * os.toByteArray().length / data.length));

		return os.toByteArray();
	}
}
