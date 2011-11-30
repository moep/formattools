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
package org.mapsforge.storage.tile;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * An implementation that provides methods for accessing a map database on a PC using SQLite3. This
 * class is not thread safe and should therefore not be used more than once at a time.
 * 
 * @author Karsten Groll
 * 
 */
public class PCTilePersistanceManager implements TilePersistanceManager {
	// Database
	private Connection conn = null;
	private Statement stmt = null;
	private PreparedStatement updateOrInsertTileByIDStmt = null;
	private PreparedStatement deleteTileByIDStmt = null;
	private PreparedStatement getTileByIDStmt = null;
	private ResultSet resultSet = null;

	/**
	 * Open the specified map database. If the database does not exist it will be created.
	 * 
	 * @param path
	 *            Path to a map database file.
	 */
	public PCTilePersistanceManager(String path) {
		try {
			openOrCreateDB(path);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private void openOrCreateDB(String path) throws ClassNotFoundException, SQLException {
		Class.forName("SQLite.JDBC");

		this.conn = DriverManager.getConnection("jdbc:sqlite:/" + path);
		this.conn.setAutoCommit(false);

		this.stmt = conn.createStatement();
		this.updateOrInsertTileByIDStmt = conn.prepareStatement("INSERT OR REPLACE INTO ? VALUES (?,?);");
		this.deleteTileByIDStmt = conn.prepareStatement("DELETE FROM ? WHERE id == ?;");
		this.getTileByIDStmt = conn.prepareStatement("SELECT data FROM ? WHERE id == ?;");

		// Create database if it does not yet exist.
		File dbFile = new File(path);
		if (dbFile.length() == 0) {
			createDatabase();
		}
	}

	private void createDatabase() throws SQLException {
		System.out.println("Creating database");

		// CREATE TABLES
		this.stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tiles_0 (id INTEGER, data BLOB, PRIMARY KEY (id));");
		this.stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tiles_1 (id INTEGER, data BLOB, PRIMARY KEY (id));");
		this.stmt.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (key STRING, value STRING, PRIMARY KEY (key));");

		// Add meta data
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('version', '0.4-experimental');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('timestamp', strftime('%s'));");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('minimum_latitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('maximum_latitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('minimum_longitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('maximum_longitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('number_of_base_zoom_levels', '2');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_0_min_max', 'xx,yy');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_1_min_max', 'xx,yy');");
		// vector or png
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_0_type', 'vector');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_1_type', 'vector');");
		this.conn.commit();
	}

	// @Override
	// public void insertTile(byte[] rawData, int xPos, int yPos, byte baseZoomLevel) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void insertTile(byte[] rawData, int id, byte baseZoomLevel) {
	// // TODO Auto-generated method stub
	//
	// }

	@Override
	public void updateOrInsertTile(byte[] rawData, int xPos, int yPos, byte baseZoomLevel) {
		int id = (int) (Math.pow(yPos, 4) + xPos);
		updateOrInsertTile(rawData, id, baseZoomLevel);

	}

	@Override
	public void updateOrInsertTile(byte[] rawData, int id, byte baseZoomLevel) {
		System.out.println("update or insert: " + new String(rawData));
		try {
			this.updateOrInsertTileByIDStmt.setString(1, "tiles_" + baseZoomLevel);
			this.updateOrInsertTileByIDStmt.setInt(2, id);
			this.updateOrInsertTileByIDStmt.setBytes(3, rawData);

			this.updateOrInsertTileByIDStmt.execute();
			this.conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void deleteTile(int xPos, int yPos, byte baseZoomLevel) {
		int id = (int) (Math.pow(yPos, 4) + xPos);
		deleteTile(id, baseZoomLevel);
	}

	@Override
	public void deleteTile(int id, byte baseZoomLevel) {
		System.out.println("delete: " + id);
		try {
			this.deleteTileByIDStmt.clearBatch();
			this.deleteTileByIDStmt.setString(1, "tiles_" + baseZoomLevel);
			this.deleteTileByIDStmt.setInt(2, id);

			this.deleteTileByIDStmt.addBatch();
			this.deleteTileByIDStmt.executeBatch();
			this.conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public byte[] getTileData(int xPos, int yPos, byte baseZoomLevel) {
		int id = (int) (Math.pow(yPos, 4) + xPos);
		return getTileData(id, baseZoomLevel);
	}

	@Override
	public byte[] getTileData(int id, byte baseZoomLevel) {
		System.out.println("get: " + id);
		byte[] result = null;

		try {
			this.getTileByIDStmt.setString(1, "tiles_" + baseZoomLevel);
			this.getTileByIDStmt.setInt(2, id);
			this.resultSet = getTileByIDStmt.executeQuery();

			if (this.resultSet.next()) {
				result = this.resultSet.getBytes(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// TODO
		return result;
	}

	@Override
	public byte[] getBaseZoomLevels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		try {
			if (!this.conn.isClosed()) {
				this.conn.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Main method for testing purposes.
	 * 
	 * @param args
	 *            Not used.
	 */
	public static void main(String[] args) {
		TilePersistanceManager tpm = new PCTilePersistanceManager("/home/moep/maps/mapsforge/test.map");
		byte[] tile;

		tpm.updateOrInsertTile("moep".getBytes(), 1, (byte) 0);
		tile = tpm.getTileData(1, (byte) 0);
		tpm.close();

		System.out.println("Tile size: " + tile.length);
		System.out.println(new String(tile));
	}
}
