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
import java.util.Collection;
import java.util.Vector;

import org.mapsforge.core.Rect;
import org.mapsforge.storage.dataExtraction.MapFileMetaData;
import org.sqlite.android.Database;
import org.sqlite.android.SQLiteException;
import org.sqlite.android.Stmt;

public class AndroidTilePersistenceManager implements TilePersistenceManager {
	private Database db = null;
	private Stmt stmt = null;
	private Stmt[] insertOrUpdateTileByIDStmt = null;
	private Stmt[] deleteTileByIDStmt = null;
	private Stmt[] getTileByIDStmt = null;
	private Stmt getMetaDataStatement = null;
	private MapFileMetaData mapFileMetaData;

	/**
	 * Open the specified map database. If the database does not exist it will be created.
	 * 
	 * @param path
	 *            Path to a map database file.
	 * @param mapFileMetaData
	 *            The map file's meta data. This will only be used when a new map file should be
	 *            created. Otherwise the meta data will be parsed from the map file. If set to null, an
	 *            empty meta data container will be used for creating the database.
	 */
	public AndroidTilePersistenceManager(String path, MapFileMetaData mapFileMetaData) {
		this.db = new Database();

		if (mapFileMetaData == null) {
			// Create default meta data values
			this.mapFileMetaData = MapFileMetaData.createInstanceWithDefaultValues();
		} else {
			this.mapFileMetaData = mapFileMetaData;
		}

		openOrCreateDB(path);
	}

	/**
	 * Opens the specified map database. If the database does not exist it will be created.
	 * 
	 * @param path
	 *            Path to a map database file.
	 */
	public AndroidTilePersistenceManager(String path) {
		this(path, null);
	}

	private void openOrCreateDB(String path) {
		File dbFile = new File(path);
		boolean fileExists = false;
		if (dbFile.length() != 0) {
			fileExists = true;
		}

		try {
			this.db.open(path, 0666);
			this.getMetaDataStatement = this.db.prepare("SELECT value FROM metadata WHERE key == ?;");

			if (fileExists) {
				loadDatabase();
			} else {
				createDatabase();
			}

			int amountOfZoomIntervals = this.mapFileMetaData.getAmountOfZoomIntervals();
			this.insertOrUpdateTileByIDStmt = new Stmt[amountOfZoomIntervals];
			this.deleteTileByIDStmt = new Stmt[amountOfZoomIntervals];
			this.getTileByIDStmt = new Stmt[amountOfZoomIntervals];

			for (int i = 0; i < amountOfZoomIntervals; i++) {
				this.insertOrUpdateTileByIDStmt[i] = this.db.prepare("INSERT OR REPLACE INTO tiles_" + i + " VALUES (?,?);");
				this.deleteTileByIDStmt[i] = this.db.prepare("DELETE FROM tiles_" + i + " WHERE id == ?;");
				this.getTileByIDStmt[i] = this.db.prepare("SELECT data FROM tiles_" + i + " WHERE id == ?;");
			}

		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
			e.printStackTrace();

		}

	}

	private void createDatabase() throws SQLiteException {

		// CREATE TABLES
		for (int i = 0; i < this.mapFileMetaData.getAmountOfZoomIntervals(); i++) {
			this.db.exec("CREATE TABLE IF NOT EXISTS tiles_" + i + " (id INTEGER, data BLOB, PRIMARY KEY (id));", null);
		}

		// Mostly information from former file header
		this.db.exec("CREATE TABLE IF NOT EXISTS metadata (key STRING, value STRING, PRIMARY KEY (key));", null);
		this.db.exec("CREATE TABLE IF NOT EXISTS poi_tags (tag STRING, value INTEGER);", null);
		this.db.exec("CREATE TABLE IF NOT EXISTS way_tags (tag STRING, value INTEGER);", null);
		this.db.exec(
				"CREATE TABLE IF NOT EXISTS zoom_interval_configuration "
						+
						"(interval TINYINT, baseZoomLevel TINYINT, minimalZoomLevel TINYINT, maximalZoomLevel TINYINT, dataType TINYINT);",
				null);

		// INSERT (metadata)
		this.db.exec("INSERT INTO metadata VALUES ('version', '" + this.mapFileMetaData.getFileVersion() + "');", null);
		this.db.exec("INSERT INTO metadata VALUES ('dateOfCreation', '" + this.mapFileMetaData.getDateOfCreation()
				+ "');", null);
		this.db.exec("INSERT INTO metadata VALUES ('boundingBoxMinLat', " + this.mapFileMetaData.getMinLat() + " );", null);
		this.db.exec("INSERT INTO metadata VALUES ('boundingBoxMaxLat', " + this.mapFileMetaData.getMaxLat() + " );", null);
		this.db.exec("INSERT INTO metadata VALUES ('boundingBoxMinLon', " + this.mapFileMetaData.getMinLon() + " );", null);
		this.db.exec("INSERT INTO metadata VALUES ('boundingBoxMaxLon', " + this.mapFileMetaData.getMaxLon() + " );", null);
		this.db.exec("INSERT INTO metadata VALUES ('tileSize', '" + this.mapFileMetaData.getTileSize() + "');", null);
		this.db.exec("INSERT INTO metadata VALUES ('projection', '" + this.mapFileMetaData.getProjection() + "');", null);
		this.db.exec("INSERT INTO metadata VALUES ('languagePreference', '"
				+ this.mapFileMetaData.getLanguagePreference() + "');", null);

		// Flags
		this.db.exec("INSERT INTO metadata VALUES ('debugInformationFlag', '"
				+ (this.mapFileMetaData.isDebugFlagSet() ? "1" : "0") + "');", null);
		this.db.exec("INSERT INTO metadata VALUES ('mapPositionExistsFlag', '"
				+ (this.mapFileMetaData.isMapStartPositionFlagSet() ? "1" : "0") + "');", null);

		if (this.mapFileMetaData.isMapStartPositionFlagSet()) {
			this.db.exec("INSERT INTO metadata VALUES ('mapStartLat', '" + this.mapFileMetaData.getMapStartLat()
					+ "');", null);
			this.db.exec("INSERT INTO metadata VALUES ('mapStartLon', '" + this.mapFileMetaData.getMapStartLon()
					+ "');", null);
		}

		this.db.exec("INSERT INTO metadata VALUES ('comment', '" + this.mapFileMetaData.getComment() + "');", null);

		// Create default zoom level configuration
		for (int i = 0; i < this.mapFileMetaData.getAmountOfZoomIntervals(); i++) {
			this.db.exec("INSERT INTO zoom_interval_configuration VALUES ('"
					+ i + "','"
					+ this.mapFileMetaData.getBaseZoomLevel()[i] + "','"
					+ this.mapFileMetaData.getMinimalZoomLevel()[i] + "','"
					+ this.mapFileMetaData.getMaximalZoomLevel()[i] + "','"
					+ TileDataContainer.TILE_TYPE_VECTOR
					+ "');", null);
		}

		// Create POI tag mapping entries
		for (int i = 0; i < this.mapFileMetaData.getAmountOfPOIMappings(); i++) {
			this.db.exec("INSERT INTO poi_tags (tag, value) VALUES ('" + this.mapFileMetaData.getPOIMappings()[i] + "', '"
					+ i + "')", null);
		}

		// Create Way tag mapping entries
		for (int i = 0; i < this.mapFileMetaData.getAmountOfPOIMappings(); i++) {
			this.db.exec("INSERT INTO way_tags (tag, value) VALUES ('" + this.mapFileMetaData.getWayTagMappings()[i]
					+ "', '"
					+ i + "')", null);
		}
	}

	@Override
	public void insertOrUpdateTile(byte[] rawData, int xPos, int yPos, byte baseZoomInterval) {
		insertOrUpdateTile(rawData, coordinatesToID(xPos, yPos, baseZoomInterval), baseZoomInterval);
	}

	@Override
	public void insertOrUpdateTile(byte[] rawData, int id, byte baseZoomInterval) {
		try {
			this.insertOrUpdateTileByIDStmt[baseZoomInterval].reset();
			this.insertOrUpdateTileByIDStmt[baseZoomInterval].clear_bindings();
			this.insertOrUpdateTileByIDStmt[baseZoomInterval].bind(1, "tiles_" + baseZoomInterval);
			this.insertOrUpdateTileByIDStmt[baseZoomInterval].bind(2, id);
			this.insertOrUpdateTileByIDStmt[baseZoomInterval].bind(3, rawData);

			this.insertOrUpdateTileByIDStmt[baseZoomInterval].step();
		} catch (SQLiteException e) {
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
		}

	}

	@Override
	public void insertOrUpdateTiles(Collection<TileDataContainer> rawData) {
		try {
			this.db.exec("BEGIN TRANSACTION;", null);
			for (TileDataContainer tile : rawData) {
				this.insertOrUpdateTileByIDStmt[tile.getBaseZoomLevel()].reset();
				this.insertOrUpdateTileByIDStmt[tile.getBaseZoomLevel()].bind(1,
						coordinatesToID(tile.getxPos(), tile.getyPos(), tile.getBaseZoomLevel()));
				this.insertOrUpdateTileByIDStmt[tile.getBaseZoomLevel()].bind(2, tile.getData());
			}
			this.db.exec("COMMIT TRANSACTION;", null);
		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
		}

	}

	@Override
	public void deleteTile(int xPos, int yPos, byte baseZoomInterval) {
		deleteTile(coordinatesToID(xPos, yPos, baseZoomInterval), baseZoomInterval);
	}

	@Override
	public void deleteTile(int id, byte baseZoomInterval) {
		try {
			this.deleteTileByIDStmt[baseZoomInterval].reset();
			this.deleteTileByIDStmt[baseZoomInterval].clear_bindings();
			this.deleteTileByIDStmt[baseZoomInterval].bind(1, id);

			this.deleteTileByIDStmt[baseZoomInterval].step();
		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
		}

	}

	@Override
	public void deleteTiles(int[] ids, byte baseZoomInterval) {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] getTileData(int xPos, int yPos, byte baseZoomInterval) {
		return getTileData(coordinatesToID(xPos, yPos, baseZoomInterval), baseZoomInterval);
	}

	@Override
	public byte[] getTileData(int id, byte baseZoomInterval) {
		byte[] result = null;

		try {
			this.getTileByIDStmt[baseZoomInterval].clear_bindings();
			this.getTileByIDStmt[baseZoomInterval].reset();
			this.getTileByIDStmt[baseZoomInterval].bind(1, id);

			if (this.getTileByIDStmt[baseZoomInterval].step()) {
				result = this.getTileByIDStmt[baseZoomInterval].column_bytes(0);
			}
		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
		}

		return result;
	}

	@Override
	public Collection<TileDataContainer> getTileData(int[] ids, byte baseZoomInterval) {
		Vector<TileDataContainer> ret = new Vector<TileDataContainer>();

		// TODO Set tile coordinates
		try {
			this.stmt = this.db.prepare("SELECT * FROM tiles_" + baseZoomInterval + " WHERE id IN (" + getIDListString(ids)
					+ ");");
			while (this.stmt.step()) {
				// TODO calculate values (create constructor with id?)
				ret.add(new TileDataContainer(this.stmt.column_bytes(0), TileDataContainer.TILE_TYPE_VECTOR, -1, -1,
						baseZoomInterval));
			}
		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
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
	public MapFileMetaData getMetaData() {
		return this.mapFileMetaData;
	}

	@Override
	public void setMetaData(MapFileMetaData mapFileMetaData) {
		// TODO Auto-generated method stub

	}

	/**
	 * Loads all meta data from an existing map database.
	 */
	private void loadDatabase() {
		this.mapFileMetaData = new MapFileMetaData();

		try {
			// Version
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "version");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setFileVersion(this.getMetaDataStatement.column_string(0));
			}

			// Date of creation
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "dateOfCreation");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setDateOfCreation(Long.parseLong(this.getMetaDataStatement.column_string(0)));
			}

			// Bounding box
			Rect boundingBox = new Rect(0, 0, 0, 0);
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "boundingBoxMinLat");
			if (this.getMetaDataStatement.step()) {
				boundingBox.minLatitudeE6 = Integer.parseInt(this.getMetaDataStatement.column_string(0));
			}
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "boundingBoxMaxLat");
			if (this.getMetaDataStatement.step()) {
				boundingBox.maxLatitudeE6 = Integer.parseInt(this.getMetaDataStatement.column_string(0));
			}
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "boundingBoxMinLon");
			if (this.getMetaDataStatement.step()) {
				boundingBox.minLongitudeE6 = Integer.parseInt(this.getMetaDataStatement.column_string(0));
			}
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "boundingBoxMaxLon");
			if (this.getMetaDataStatement.step()) {
				boundingBox.maxLongitudeE6 = Integer.parseInt(this.getMetaDataStatement.column_string(0));
			}
			this.mapFileMetaData.setBoundingBox(boundingBox.minLatitudeE6, boundingBox.minLongitudeE6, boundingBox.maxLatitudeE6,
					boundingBox.maxLongitudeE6);

			// Tile size
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "tileSize");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setTileSize(Integer.parseInt(this.getMetaDataStatement.column_string(0)));
			}

			// Projection
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "projection");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setProjection(this.getMetaDataStatement.column_string(0));
			}

			// Language preference
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "languagePreference");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setLanguagePreference(this.getMetaDataStatement.column_string(0));
			}

			byte flags = 0;
			// Debug flag
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "debugInformationFlag");
			if (this.getMetaDataStatement.step()) {
				if (this.getMetaDataStatement.column_string(0).equals("1")) {
					flags = (byte) (flags | (byte) 0x80);
				}
			}

			// Map position flag
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "mapPositionExistsFlag");
			if (this.getMetaDataStatement.step()) {
				if (this.getMetaDataStatement.column_string(0).equals("1")) {
					flags = (byte) (flags | (byte) 0x40);
				}
			}

			this.mapFileMetaData.setFlags(flags);

			// Comment
			this.getMetaDataStatement.reset();
			this.getMetaDataStatement.clear_bindings();
			this.getMetaDataStatement.bind(1, "comment");
			if (this.getMetaDataStatement.step()) {
				this.mapFileMetaData.setComment(this.getMetaDataStatement.column_string(0));
			}

			// POI tag mappings
			int numPoiTags = 0;
			this.stmt = this.db.prepare("SELECT count(*) FROM poi_tags;");
			while (this.stmt.step()) {
				numPoiTags = Integer.parseInt(this.stmt.column_string(0));
			}
			this.mapFileMetaData.setAmountOfPOIMappings(numPoiTags);
			this.mapFileMetaData.preparePOIMappings();

			this.stmt = this.db.prepare("SELECT tag, value FROM poi_tags;");
			while (this.stmt.step()) {
				this.mapFileMetaData.getPOIMappings()[this.stmt.column_int(1)] = this.stmt.column_string(0);
			}

			// Way Tag mappings
			int numWayTags = 0;
			this.stmt = this.db.prepare("SELECT count(*) FROM way_tags;");
			while (this.stmt.step()) {
				numWayTags = Integer.parseInt(this.stmt.column_string(0));
			}
			this.mapFileMetaData.setAmountOfWayTagMappings(numWayTags);
			this.mapFileMetaData.prepareWayTagMappings();

			this.stmt = this.db.prepare("SELECT tag, value FROM way_tags;");
			while (this.stmt.step()) {
				this.mapFileMetaData.getWayTagMappings()[this.stmt.column_int(1)] = this.stmt.column_string(0);
			}

			// Zoom interval configuration
			byte numIntervals = 0;
			this.stmt = this.db.prepare("SELECT count(*) FROM zoom_interval_configuration;");
			while (this.stmt.step()) {
				numIntervals = Byte.parseByte(this.stmt.column_string(0));
			}
			this.mapFileMetaData.setAmountOfZoomIntervals(numIntervals);
			this.mapFileMetaData.prepareZoomIntervalConfiguration();

			this.stmt = this.db
					.prepare("SELECT interval, baseZoomLevel, minimalZoomLevel, maximalZoomLevel, dataType FROM zoom_interval_configuration;");
			while (this.stmt.step()) {
				this.mapFileMetaData.setZoomIntervalConfiguration(Integer.parseInt(this.stmt.column_string(0)),
						Byte.parseByte(this.stmt.column_string(1)),
						Byte.parseByte(this.stmt.column_string(2)),
						Byte.parseByte(this.stmt.column_string(3)),
						Byte.parseByte(this.stmt.column_string(4)));
			}

		} catch (SQLiteException e) {
			// TODO Android error handling
			System.out.println("!!!EXCEPTION!!! " + e.getMessage());
		}

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	private int coordinatesToID(int xPos, int yPos, int baseZoomInterval) {
		return (int) (yPos * Math.pow(this.mapFileMetaData.getBaseZoomLevel()[baseZoomInterval], 2) + xPos);
	}

}
