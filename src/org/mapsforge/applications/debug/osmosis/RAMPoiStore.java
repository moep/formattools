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
import java.util.logging.Logger;

/**
 * A container class for storing POI data in a set.
 * 
 * @author Karsten Groll
 * 
 */
public class RAMPoiStore {
	// Number of data sets written to the db at once
	private static final int BATCH_SIZE = 25000;
	private static Logger LOGGER = Logger.getLogger(POIWriterTask.class.getName());
	private List<POI> pois;

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
	 * @param data
	 *            The nodes tags stored as an array of <code>key=value</code> strings.
	 */
	public void addPOI(long id, double lat, double lon, String[] data) {
		pois.add(new POI(id, lat, lon, data));
	}

	/**
	 * Writes the POI data into an SQLite DB.
	 */
	public void writeToSQLiteDB() {

		Connection conn = null;
		PreparedStatement pStmt = null;
		PreparedStatement pStmt2 = null;
		Statement stmt = null;
		try {
			Class.forName("SQLite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite://home/moep/poi.db");
			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			pStmt = conn.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
			pStmt2 = conn.prepareStatement("INSERT INTO poi_data VALUES (?, ?, ?);");

			// CREATE TABLES
			System.out.println("CREATE TABLES");
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_index;");
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_data;");
			stmt.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);");
			stmt.executeUpdate("CREATE TABLE poi_data (id LONG, key TEXT, value TEXT);");
			conn.commit();

			// INSERT
			LOGGER.fine("Batches: " + ((int) (this.pois.size() / BATCH_SIZE)));
			// TODO batch size;
			int numAttributes = 0;
			int processed = 0;
			long id = 0;
			for (POI p : pois) {
				// index
				// pStmt.setLong(1, id++);
				// pStmt.setDouble(2, 1.0);
				// pStmt.setDouble(3, 1.0);
				// pStmt.setDouble(4, 1.0);
				// pStmt.setDouble(5, 1.0);
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
				numAttributes = p.getKeys().length;
				for (int i = 0; i < numAttributes; i++) {
					pStmt2.setLong(1, p.getID());
					pStmt2.setString(2, p.getKeys()[i]);
					pStmt2.setString(3, p.getValues()[i]);
					pStmt2.addBatch();
				}

				// ++processed;
				// if (processed == BATCH_SIZE) {
				// System.out.println("COMMIT");
				// pStmt.executeBatch();
				// pStmt2.executeBatch();
				// conn.commit();
				// processed = 0;
				// }

			}

			System.out.println("COMMIT");
			pStmt.executeBatch();
			pStmt2.executeBatch();
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
		private String[] keys;
		private String[] values;

		/**
		 * @param id
		 *            The node's ID.
		 * @param lat
		 *            The node's latitude.
		 * @param lon
		 *            The node's longitude.
		 * @param data
		 *            The nodes tags stored as an array of <code>key=value</code> strings.
		 */
		POI(long id, double lat, double lon, String[] data) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;

			this.keys = new String[data.length];
			this.values = new String[data.length];
			for (int i = 0; i < data.length; i++) {
				keys[i] = data[i].split("=")[0];
				values[i] = data[i].split("=")[1];
			}
		}

		public long getID() {
			return this.id;
		}

		public double getLat() {
			return lat;
		}

		public double getLon() {
			return lon;
		}

		public String[] getKeys() {
			return keys;
		}

		public String[] getValues() {
			return values;
		}

	}

}
