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
package org.mapsforge.applications.debug.osmosis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Logger;

import org.mapsforge.storage.debug.CategoryResolver;
import org.mapsforge.storage.debug.PoiCategory;
import org.mapsforge.storage.debug.UnknownCategoryException;

/**
 * A container class for storing POI data in a set an serializing it.
 * 
 * @author Karsten Groll
 * 
 */
public class RAMPoiStore {
	// Number of data sets written to the db at once
	private static final int BATCH_SIZE = 25000;
	private static final Logger LOGGER = Logger.getLogger(POIWriterTask.class.getName());
	private List<POI> pois;

	/**
	 * Default constructor.
	 */
	public RAMPoiStore() {
		this.pois = new LinkedList<POI>();
	}

	/**
	 * Adds a POI to the temporary data storage.
	 * 
	 * @param id
	 *            The node's id.
	 * @param lat
	 *            The node's latitude.
	 * @param lon
	 *            The node's longitude.
	 * @param tag
	 *            The nodes type as a key / value pair. (E.g. 'amenity=fuel');
	 * @param name
	 *            The POIs name
	 */
	public void addPOI(long id, double lat, double lon, String tag, String name) {
		pois.add(new POI(id, lat, lon, tag, name));
	}

	/**
	 * Writes the POI data into an SQLite DB.
	 * 
	 * @param path
	 *            Path to the SQLite file that should be written.
	 */
	public void writeToSQLiteDB(String path) {

		LOGGER.info("Writing SQLite POI data to " + path);

		// Write data using native SQLite
		Connection conn = null;
		PreparedStatement pStmt = null;
		PreparedStatement pStmt2 = null;
		PreparedStatement pStmt3 = null;
		Statement stmt = null;
		try {
			Class.forName("SQLite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:/" + path);
			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			pStmt = conn.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
			pStmt2 = conn.prepareStatement("INSERT INTO poi_data VALUES (?, ?, ?);");
			pStmt3 = conn.prepareStatement("INSERT INTO poi_categories VALUES (?, ?, ?);");

			// CREATE TABLES
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_index;");
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_data;");
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_categories;");
			// stmt.executeUpdate("DROP INDEX IF EXISTS poi_categories_index;");
			stmt.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);");
			stmt.executeUpdate("CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));");
			// TODO model child / parent relationships for categories (foreign keys)
			stmt.executeUpdate("CREATE TABLE poi_categories (id INTEGER, name VARCHAR, parent INTEGER, PRIMARY KEY (id));");
			// stmt.executeUpdate("CREATE INDEX poi_categories_index ON poi_categories (id);");
			conn.commit();

			// INSERT CATEGORIES
			PoiCategory root = CategoryResolver.getRootCategory();
			pStmt3.setLong(1, root.getID());
			pStmt3.setBytes(2, root.getTitle().getBytes());
			pStmt3.setNull(3, 0);
			pStmt3.addBatch();

			Stack<PoiCategory> children = new Stack<PoiCategory>();
			children.push(root);
			while (!children.isEmpty()) {
				for (PoiCategory c : children.pop().getChildren()) {
					pStmt3.setLong(1, c.getID());
					pStmt3.setBytes(2, c.getTitle().getBytes());
					pStmt3.setInt(3, c.getParent().getID());
					pStmt3.addBatch();
					children.push(c);
				}
			}

			// INSERT DATA
			int numBatches = (int) Math.ceil(this.pois.size() / BATCH_SIZE);
			LOGGER.fine("Batches: " + numBatches);
			int processed = 0;
			int numCommits = 0;
			for (POI p : pois) {
				// index
				pStmt.setLong(1, p.getID());
				pStmt.setDouble(2, p.getLat());
				pStmt.setDouble(3, p.getLat());
				pStmt.setDouble(4, p.getLon());
				pStmt.setDouble(5, p.getLon());

				// System.out.println("ID: " + p.getID() + " lat: " + p.getLat() + " lon: " +
				// p.getLon());

				// stmt.executeUpdate("INSERT INTO poi_index VALUES (" + p.getID() + ", "
				// + p.getLat() + ", " + p.getLat() + ", " + p.getLon() + ", "
				// + p.getLon() + ");");
				pStmt.addBatch();

				// data
				pStmt2.setLong(1, p.getID());
				pStmt2.setBytes(2, p.getName().getBytes());
				pStmt2.setInt(3, p.getCategoryID());
				pStmt2.addBatch();

				// Category

				++processed;
				if (processed == BATCH_SIZE) {
					++numCommits;
					LOGGER.fine("COMMIT " + numCommits + " of " + numBatches);
					pStmt.executeBatch();
					pStmt2.executeBatch();
					pStmt.clearBatch();
					pStmt2.clearBatch();
					conn.commit();
					processed = 0;
				}

			}

			LOGGER.fine("COMMIT " + numCommits + " of " + numBatches);
			pStmt.executeBatch();
			pStmt2.executeBatch();
			pStmt3.executeBatch();
			conn.commit();

			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return Number of POIs in this storage.
	 */
	public int getNumberOfPOIs() {
		return this.pois.size();
	}

	/**
	 * Container class for POIs. This class is read-only.
	 * 
	 */
	private class POI {
		private long id;
		private double lat;
		private double lon;
		private String tag;
		private String name;

		/**
		 * Adds a POI to the temporary data storage.
		 * 
		 * @param id
		 *            The node's id.
		 * @param lat
		 *            The node's latitude.
		 * @param lon
		 *            The node's longitude.
		 * @param tag
		 *            The nodes type tag (e.g. amenity=fuel)
		 * @param name
		 *            The POIs name
		 */
		POI(long id, double lat, double lon, String tag, String name) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.tag = tag;
			this.name = name;
		}

		public long getID() {
			return this.id;
		}

		public double getLat() {
			return this.lat;
		}

		public double getLon() {
			return this.lon;
		}

		public String getTag() {
			return this.tag;
		}

		public String getName() {
			return this.name;
		}

		public int getCategoryID() {
			int id = -1;
			try {
				id = (CategoryResolver.getPoiCategoryByTag(this.tag)).getID();
			} catch (UnknownCategoryException e) {
				// do nothing
			}

			return id;
		}

	}

}
