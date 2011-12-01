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
import java.util.Collection;
import java.util.Vector;

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
	private PreparedStatement insertOrUpdateTileByIDStmt = null;
	private PreparedStatement deleteTileByIDStmt = null;
	private PreparedStatement getTileByIDStmt = null;
	private ResultSet resultSet = null;

	// e.g.: zoomLevelMapping[baseZoomLevel = 1] -> zoom level 14
	private byte[] zoomLevelConfiguration;

	/**
	 * Open the specified map database. If the database does not exist it will be created.
	 * 
	 * @param path
	 *            Path to a map database file.
	 * @param zoomLevelConfiguration
	 *            Mapping base zoom level -> real zoom level
	 */
	public PCTilePersistanceManager(String path, byte[] zoomLevelConfiguration) {

		this.zoomLevelConfiguration = zoomLevelConfiguration;

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
		this.insertOrUpdateTileByIDStmt = conn.prepareStatement("INSERT OR REPLACE INTO ? VALUES (?,?);");
		this.deleteTileByIDStmt = conn.prepareStatement("DELETE FROM ? WHERE id == ?;");
		this.getTileByIDStmt = conn.prepareStatement("SELECT data FROM ? WHERE id == ?;");

		// Create database if it does not yet exist.
		File dbFile = new File(path);
		if (dbFile.length() == 0) {
			createDatabase();
		}

		// TODO read zoom level configuration from DB (includes data type for each base zoom level)

	}

	private void createDatabase() throws SQLException {
		System.out.println("Creating database");

		// CREATE TABLES
		for (int i = 0; i < this.zoomLevelConfiguration.length; i++) {
			this.stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tiles_" + i + " (id INTEGER, data BLOB, PRIMARY KEY (id));");
		}
		this.stmt.executeUpdate("CREATE TABLE IF NOT EXISTS metadata (key STRING, value STRING, PRIMARY KEY (key));");

		// INSERT (metadata)
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('version', '0.4-experimental');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('timestamp', strftime('%s'));");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('minimum_latitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('maximum_latitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('minimum_longitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('maximum_longitude', 'TODO');");
		this.stmt.executeUpdate("INSERT INTO metadata VALUES ('number_of_base_zoom_levels', '"
				+ this.zoomLevelConfiguration.length + "');");

		// TODO: Do we need this?
		for (int i = 0; i < this.zoomLevelConfiguration.length; i++) {
			this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_" + i + "_min_max', 'xx,yy');");
		}

		// vector or png
		for (int i = 0; i < this.zoomLevelConfiguration.length; i++) {
			this.stmt.executeUpdate("INSERT INTO metadata VALUES ('bzl_" + i + "_type', 'vector');");
		}
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
	public void insertOrUpdateTile(byte[] rawData, int xPos, int yPos, byte baseZoomLevel) {
		insertOrUpdateTile(rawData, coordinatesToID(xPos, yPos, baseZoomLevel), baseZoomLevel);
	}

	@Override
	public void insertOrUpdateTile(byte[] rawData, int id, byte baseZoomLevel) {
		System.out.println("Insert or update");
		try {
			this.insertOrUpdateTileByIDStmt.setString(1, "tiles_" + baseZoomLevel);
			this.insertOrUpdateTileByIDStmt.setInt(2, id);
			this.insertOrUpdateTileByIDStmt.setBytes(3, rawData);

			this.insertOrUpdateTileByIDStmt.execute();
			this.conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void insertOrUpdateTiles(Collection<TileDataContainer> rawData) {
		try {
			this.insertOrUpdateTileByIDStmt.clearBatch();
			for (TileDataContainer tile : rawData) {
				this.insertOrUpdateTileByIDStmt.setString(1, "tiles_" + tile.getBaseZoomLevel());
				this.insertOrUpdateTileByIDStmt.setInt(2,
						coordinatesToID(tile.getxPos(), tile.getyPos(), tile.getBaseZoomLevel()));
				this.insertOrUpdateTileByIDStmt.setBytes(3, tile.getData());
				this.insertOrUpdateTileByIDStmt.addBatch();
			}

			insertOrUpdateTileByIDStmt.executeBatch();
			this.conn.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deleteTile(int xPos, int yPos, byte baseZoomLevel) {
		deleteTile(coordinatesToID(xPos, yPos, baseZoomLevel), baseZoomLevel);
	}

	@Override
	public void deleteTile(int id, byte baseZoomLevel) {
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
	public void deleteTiles(int[] id, byte baseZoomLevel) {
		try {
			this.deleteTileByIDStmt.clearBatch();
			for (int i = 0; i < id.length; i++) {
				this.deleteTileByIDStmt.setString(1, "tiles_" + baseZoomLevel);
				this.deleteTileByIDStmt.setInt(2, id[i]);

				this.deleteTileByIDStmt.addBatch();
			}
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

		return result;
	}

	@Override
	public Collection<TileDataContainer> getTileData(int[] ids, byte baseZoomLevel) {
		Vector<TileDataContainer> ret = new Vector<TileDataContainer>();

		// System.out.println("SELECT data FROM tiles_" + baseZoomLevel + " WHERE id IN " +
		// getIDListString(ids) + ";");
		// TODO Can we use a prepared statement here?
		try {
			this.stmt.execute("SELECT * FROM tiles_" + baseZoomLevel + " WHERE id IN (" + getIDListString(ids) + ");");
			this.resultSet = this.stmt.getResultSet();

			int i = 0;
			while (this.resultSet.next()) {
				// TODO calculate values (create constructor with id?)
				ret.add(new TileDataContainer(resultSet.getBytes(1), TileDataContainer.TILE_TYPE_VECTOR, -1, -1, baseZoomLevel));
				++i;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ret;

	}

	private String getIDListString(int ids[]) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < ids.length; i++) {
			sb.append(ids[i]);

			if (i != ids.length - 1) {
				sb.append(',');
			}
		}

		return sb.toString();
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

	private int coordinatesToID(int xPos, int yPos, int baseZoomLevel) {
		return (int) (yPos * Math.pow(this.zoomLevelConfiguration[baseZoomLevel], 4) + xPos);
	}

	/**
	 * Main method for testing purposes.
	 * 
	 * @param args
	 *            Not used.
	 */
	public static void main(String[] args) {
		PCTilePersistanceManager tpm = new PCTilePersistanceManager("/home/moep/maps/mapsforge/berlin.map", new byte[] {
				(byte) 8, (byte) 14 });

		Vector<TileDataContainer> tiles = new Vector<TileDataContainer>();
		tiles.add(new TileDataContainer("moep".getBytes(), TileDataContainer.TILE_TYPE_VECTOR, 1, 0, (byte) 1));
		tiles.add(new TileDataContainer("bla".getBytes(), TileDataContainer.TILE_TYPE_VECTOR, 2, 0, (byte) 1));
		tiles.add(new TileDataContainer("blubb".getBytes(), TileDataContainer.TILE_TYPE_VECTOR, 3, 0, (byte) 1));
		tiles.add(new TileDataContainer("bleh!".getBytes(), TileDataContainer.TILE_TYPE_VECTOR, 4, 0, (byte) 1));
		tiles.add(new TileDataContainer("narf!".getBytes(), TileDataContainer.TILE_TYPE_VECTOR, 5, 0, (byte) 1));

		tpm.insertOrUpdateTiles(tiles);

		Collection<TileDataContainer> ret = tpm.getTileData(new int[] { 2, 3, 4 }, (byte) 1);
		for (TileDataContainer c : ret) {
			System.out.println(c.getData());
		}

		tpm.close();

	}
}
