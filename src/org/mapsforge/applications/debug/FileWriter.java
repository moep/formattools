package org.mapsforge.applications.debug;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

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
	 * Creates an index structure containing of directories representing the x-dimension and files for
	 * the y-dimension.
	 * 
	 * @param path
	 *            Path where the directories and files should be created in.
	 * @param numXDimensions
	 *            Number of x-dimensions (sub folders).
	 * @param numYDimensions
	 *            Number of y-dimensions (files per sub folder).
	 * @param dataSize
	 *            Size of a fake tile in KiB.
	 * @throws IOException
	 *             when files cannot be created.
	 */
	public static void createDirStructureWithFiles(String path, int numXDimensions, int numYDimensions,
			int dataSize)
			throws IOException {
		// Create directories
		new File(path + "/bzs7/").mkdirs();

		// 30k fake data
		byte[] data = new byte[1024 * dataSize];
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) 0x88;
		}

		RandomAccessFile f;
		for (int x = 0; x < numXDimensions; x++) {
			new File(path + "/bzs7/" + x + "/").mkdirs();
			for (int y = 0; y < numYDimensions; y++) {
				f = new RandomAccessFile(path + "/bzs7/" + x + "/" + y + ".y", "rw");
				f.write(data);
				f.close();
			}
		}
	}

	/**
	 * Creates a SQLite DB with mock data for accessing tiles.
	 * 
	 * @param path
	 *            Path where the SQLite database (test.db) will be created.
	 * @param numXDimensions
	 *            Number of x dimensions.
	 * @param numYDimensions
	 *            Number of y dimensions.
	 * @param dataSize
	 *            Size of a fake tile in KiB.
	 * @throws ClassNotFoundException
	 *             when the JDBC driver could not be loaded.
	 * @throws SQLException
	 *             when the database cannot be filled.
	 */
	public static void createSQLiteDB(String path, int numXDimensions, int numYDimensions, int dataSize)
			throws ClassNotFoundException, SQLException {
		// File f = new File(path);
		// if (f.exists())
		// f.delete();

		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("DROP TABLE IF EXISTS idx;");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS idx (x INTEGER, y INTEGER, data BLOB, PRIMARY KEY (x,y))");

		// fake data
		byte[] data = new byte[1024 * dataSize];
		for (int i = 0; i < data.length; i += 8) {
			data[i] = 0xd;
			data[i + 1] = 0xe;
			data[i + 2] = 0xa;
			data[i + 3] = 0xd;
			data[i + 4] = 0xb;
			data[i + 5] = 0xa;
			data[i + 6] = 0xb;
			data[i + 7] = 0xe;
		}

		PreparedStatement pStmt = conn.prepareStatement("INSERT INTO idx VALUES (?, ?, ?)");
		// Blob b = conn.createBlob();
		// System.out.println("Creating blob: " + b.setBytes(0, data));
		for (int x = 0; x < numXDimensions; x++) {
			for (int y = 0; y < numYDimensions; y++) {
				pStmt.setInt(1, x);
				pStmt.setInt(2, y);
				// setBlob does not seem to be implemented :(
				// pStmt.setBlob(3, b);
				pStmt.setBytes(3, data);
				pStmt.addBatch();
			}
		}

		conn.setAutoCommit(false);
		pStmt.executeBatch();
		conn.setAutoCommit(true);
		conn.close();
	}

}
