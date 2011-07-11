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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Temporary class for creating and filling a SQLite database with tiles. This class is intended to be
 * used from within the mapsforge osmosis plugin.
 * 
 * @author Karsten Groll
 * 
 */
public class SQLiteDBTool {
	private File dbFile;
	private Connection conn;
	private Statement stmt;
	PreparedStatement pStmt;

	/**
	 * 
	 * @param path
	 *            Path to the SQLite database file that will be created.
	 */
	public SQLiteDBTool(String path) {
		this.dbFile = new File(path);

		if (this.dbFile.exists()) {
			System.out.println(this.dbFile.getAbsolutePath() + "will be overwritten.");
			this.dbFile.delete();
		}
	}

	/**
	 * Creates a SQLite database with a simple schema and opens the connection.
	 */
	public void createDB() {
		try {
			Class.forName("org.sqlite.JDBC");
			this.conn = DriverManager.getConnection("jdbc:sqlite:" + this.dbFile.getAbsolutePath());

			this.stmt = this.conn.createStatement();
			this.stmt.executeUpdate("DROP TABLE IF EXISTS idx;");
			this.stmt
					.executeUpdate("CREATE TABLE IF NOT EXISTS idx (x INTEGER, y INTEGER, data BLOB, PRIMARY KEY (x,y))");

			this.pStmt = this.conn.prepareStatement("INSERT INTO idx VALUES (?, ?, ?)");

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prepares rows consisting of the coordinates and a byte-array representation for insertion.
	 * 
	 * @param data
	 *            binary tile data.
	 * @param xTilePos
	 *            The tile's x-position.
	 * @param yTilePos
	 *            The tile's y-position.
	 * 
	 */
	public void insertData(byte[] data, int xTilePos, int yTilePos) {
		System.out.println("insertData (" + xTilePos + ", " + yTilePos + "); size:" + data.length);
		// try {
		// this.pStmt.setInt(1, xTilePos);
		// this.pStmt.setInt(2, yTilePos);
		// this.pStmt.setBytes(3, data);
		// this.pStmt.addBatch();
		// } catch (SQLException e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Commits all rows and closes the connection.
	 */
	public void commit() {
		System.out.println("***************** COMMIT");
		try {
			// this.conn.setAutoCommit(false);
			// this.pStmt.executeBatch();
			// this.conn.setAutoCommit(true);
			// this.conn.close();
			this.conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
