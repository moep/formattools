package org.mapsforge.storage.debug;

import java.util.ArrayList;
import java.util.Collection;

import org.mapsforge.core.GeoCoordinate;

import SQLite3.Database;
import SQLite3.Exception;
import SQLite3.Stmt;

/**
 * POI persistence manager using SQLite 3 with R-tree support.
 * 
 * @author Karsten Groll
 * 
 */
class SQLitePoiPersistenceManager implements PoiPersistenceManager {
	// Number of tables needed for db verification
	private static final int NUMBER_OF_TABLES = 3;
	private static final String LOG_TAG = "mapformat";

	private String dbFilePath = null;
	private Database db = null;
	private PoiCategoryManager cm = null;

	private Stmt findInBoxStatement = null;
	private Stmt findByIDStatement = null;
	private Stmt insertPoiStatement1 = null;
	private Stmt insertPoiStatement2 = null;
	private Stmt isValidDBStatement = null;

	// Container for return values
	ArrayList<PointOfInterest> ret;
	PointOfInterest poi;

	/**
	 * 
	 * @param dbFilePath
	 *            Path to SQLite file containing POI data. If the file does not exist the file and its
	 *            tables will be created.
	 */
	SQLitePoiPersistenceManager(String dbFilePath) {
		// Open / create POI database
		this.dbFilePath = dbFilePath;
		createOrOpenDBFile();

		// Load categories from database
		this.cm = new SQLitePoiCategoryManager(db);

		// Queries
		try {
			// Finds POIs by a given bounding box
			this.findInBoxStatement = db
					.prepare("SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
							+ "FROM poi_index "
							+ "JOIN poi_data ON poi_index.id = poi_data.id "
							+ "WHERE "
							+ "minLat <= ? AND "
							+ "minLon <= ? AND "
							+ "minLat >= ? AND "
							+ "minLon >= ? LIMIT ?");

			// Finds a POI by its unique ID
			this.findByIDStatement = db
					.prepare("SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
							+ "FROM poi_index "
							+ "JOIN poi_data ON poi_index.id = poi_data.id "
							+ "WHERE " + "poi_index.id = ?;");

			// Inserts a POI into index
			this.insertPoiStatement1 = db
					.prepare("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
			// Inserts a POI's data
			this.insertPoiStatement2 = db
					.prepare("INSERT INTO poi_data VALUES (?, ?, ?);");

		} catch (Exception e) {
			e.printStackTrace();
		}

		this.ret = new ArrayList<PointOfInterest>();
	}

	@Override
	public Collection<PointOfInterest> findNearPosition(GeoCoordinate point,
			int distance, String categoryName, int limit) {

		double minLat = point.getLatitude()
				- GeoCoordinate.latitudeDistance(distance);
		double minLon = point.getLongitude()
				- GeoCoordinate
						.longitudeDistance(distance, point.getLatitude());
		double maxLat = point.getLatitude()
				+ GeoCoordinate.latitudeDistance(distance);
		double maxLon = point.getLongitude()
				+ GeoCoordinate
						.longitudeDistance(distance, point.getLatitude());

		return findInRect(new GeoCoordinate(minLat, minLon), new GeoCoordinate(
				maxLat, maxLon), categoryName, limit);
	}

	@Override
	public Collection<PointOfInterest> findInRect(GeoCoordinate p1,
			GeoCoordinate p2, String categoryName, int limit) {
		// Clear previous results
		this.ret.clear();

		// Query
		try {
			findInBoxStatement.reset();
			findInBoxStatement.clear_bindings();

			this.findInBoxStatement.bind(1, p2.getLatitude());
			this.findInBoxStatement.bind(2, p2.getLongitude());
			this.findInBoxStatement.bind(3, p1.getLatitude());
			this.findInBoxStatement.bind(4, p1.getLongitude());
			this.findInBoxStatement.bind(5, limit);

			// TODO externalize to getPoiByStatement

			long id = -1;
			double lat = 0;
			double lon = 0;
			String data = "";
			int categoryID = -1;

			while (this.findInBoxStatement.step()) {
				id = this.findInBoxStatement.column_long(0);
				lat = this.findInBoxStatement.column_double(1);
				lon = this.findInBoxStatement.column_double(2);
				data = this.findInBoxStatement.column_string(3);
				categoryID = this.findInBoxStatement.column_int(4);

				this.poi = new PoiImpl(id, lat, lon, data,
						CategoryResolver.getPoiCategoryByID(categoryID));
				this.ret.add(poi);
			}
		} catch (Exception e) {
			// Log.e(LOG_TAG, e.getMessage());
			e.printStackTrace();
		}

		return this.ret;
	}

	@Override
	public Collection<PointOfInterest> findInRectWithFilter(GeoCoordinate p1, GeoCoordinate p2, String categoryName, int limit,
			CategoryFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void insertPointOfInterest(PointOfInterest poi) {
		// TODO implement

	}

	@Override
	public void insertPointsOfInterest(Collection<PointOfInterest> pois) {
		// TODO implement

	}

	@Override
	public void removePointOfInterest(PointOfInterest poi) {
		// TODO implement

	}

	@Override
	public void close() {
		// Close statements

		if (this.findInBoxStatement != null) {
			try {
				this.findInBoxStatement.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

		if (this.findByIDStatement != null) {
			try {
				this.findByIDStatement.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

		if (this.insertPoiStatement1 != null) {
			try {
				this.insertPoiStatement1.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

		if (this.insertPoiStatement2 != null) {
			try {
				this.insertPoiStatement2.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

		if (this.isValidDBStatement != null) {
			try {
				this.isValidDBStatement.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

		// Close connection

		if (this.db != null) {
			try {
				this.db.close();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}

	}

	@Override
	public PointOfInterest findPointByID(long poiID) {

		// Log.d(LOG_TAG,
		// "SELECT poi_index.id, poi_index.minLat, poi_index.minLon, poi_data.data, poi_data.category "
		// + "FROM poi_index "
		// + "JOIN poi_data ON poi_index.id = poi_data.id "
		// + "WHERE " + "poi_index.id = " + poiID + ";");

		// TODO externalize to getPoiFromStatement

		long id = -1;
		double lat = 0;
		double lon = 0;
		String data = "";
		int categoryID = -1;
		this.poi = null;

		try {
			this.findByIDStatement.clear_bindings();
			this.findByIDStatement.reset();
			this.findByIDStatement.bind(1, poiID);

			if (this.findByIDStatement.step()) {
				id = this.findByIDStatement.column_long(0);
				lat = this.findByIDStatement.column_double(1);
				lon = this.findByIDStatement.column_double(2);
				data = this.findByIDStatement.column_string(3);
				categoryID = this.findByIDStatement.column_int(4);

				this.poi = new PoiImpl(id, lat, lon, data,
						CategoryResolver.getPoiCategoryByID(categoryID));
			}
		} catch (Exception e) {
			// Log.e(LOG_TAG, "getPointById: " + e.getMessage());
			e.printStackTrace();
		}

		return this.poi;
	}

	/**
	 * If the file does not exist it will be created and filled.
	 */
	private void createOrOpenDBFile() {

		// Create or open File
		this.db = new Database();
		try {
			db.open(this.dbFilePath, 0666);
		} catch (Exception e) {
			// Log.e(LOG_TAG, e.getMessage());
			e.printStackTrace();
		}

		if (!isValidDataBase()) {
			try {
				// Log.d(LOG_TAG, "Creating tables");
				createTables();
			} catch (Exception e) {
				// TODO Android error handling
			}
		}
	}

	private void createTables() throws Exception {
		// db.open() created a new file, so let's create its tables
		this.db.exec("DROP TABLE IF EXISTS poi_data;", null);
		this.db.exec("DROP TABLE IF EXISTS poi_categories;", null);
		this.db.exec("DROP INDEX IF EXISTS poi_categories_index;", null);
		this.db.exec(
				"CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);",
				null);
		this.db.exec(
				"CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));",
				null);
		// TODO model child / parent relationships for categories
		this.db.exec(
				"CREATE TABLE poi_categories (id INTEGER, name VARCHAR, PRIMARY KEY (id));",
				null);
		// this.db.exec(
		// "CREATE INDEX poi_categories_index ON poi_categories (id);",
		// null);
	}

	/**
	 * 
	 * @return True if the database is a valid POI database.
	 */
	private boolean isValidDataBase() {
		try {
			this.isValidDBStatement = db.prepare("SELECT count(name) "
					+ "FROM sqlite_master " + "WHERE name IN "
					+ "('poi_index', 'poi_data', 'poi_categories');");
		} catch (Exception e1) {
			// TODO Android error handling
		}

		// Check for table names
		// TODO Is it necessary to get the tables meta data as well?
		int numTables = 0;
		try {
			if (this.isValidDBStatement.step()) {
				numTables = this.isValidDBStatement.column_int(0);
			}
		} catch (Exception e) {
			// TODO Android error handling
		}

		return numTables == SQLitePoiPersistenceManager.NUMBER_OF_TABLES;
	}

	@Override
	public PoiCategoryManager getCategoryManager() {
		return this.cm;
	}

	@Override
	public void setCategoryManager(PoiCategoryManager categoryManager) {
		this.cm = categoryManager;

	}

}
