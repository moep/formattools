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
 * A container class for storing POI data in a set an serializing it.
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
	 * @param tag
	 *            The nodes type as a key / value pair. (E.g. 'amenity=fuel');
	 * @param name
	 *            The POIs name
	 */
	public void addPOI(long id, double lat, double lon, String tag, String name) {
		// TODO use mapping instead of hash
		pois.add(new POI(id, lat, lon, tag.hashCode(), name));
	}

	/**
	 * Writes the POI data into an SQLite DB.
	 * 
	 * @param path
	 *            Path to the SQLite file that should be written.
	 */
	public void writeToSQLiteDB(String path) {
		// Determine OS
		if (!System.getProperty("os.name").equals("Linux")) {
			LOGGER.severe("UNSUPPORTED OS / ARCHITECTURE");
			LOGGER.info("At the moment we do only support Linux x86 and amd64.");
		} else {
			// Set library path for os
			String arch = System.getProperty("sun.arch.data.model");
			// TODO Don't attach entry instead of overwriting
			System.setProperty("java.library.path",
					System.getProperty("user.dir") +
							"/lib/jni/linux_" +
							(arch.equals("32") ? "x86" : "amd64"));

			LOGGER.fine("java.library.path was set to '" + System.getProperty("java.library.path")
					+ "'");
		}

		LOGGER.info("Writing SQLite POI data to " + path);

		// Write data using native SQLite
		Connection conn = null;
		PreparedStatement pStmt = null;
		PreparedStatement pStmt2 = null;
		Statement stmt = null;
		try {
			Class.forName("SQLite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:/" + path);
			conn.setAutoCommit(false);

			stmt = conn.createStatement();
			pStmt = conn.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
			pStmt2 = conn.prepareStatement("INSERT INTO poi_data VALUES (?, ?);");

			// CREATE TABLES
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_index;");
			stmt.executeUpdate("DROP TABLE IF EXISTS poi_data;");
			stmt.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);");
			stmt.executeUpdate("CREATE TABLE poi_data (id LONG, data BLOB, PRIMARY KEY (id));");
			conn.commit();

			// INSERT
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
				pStmt2.addBatch();

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
		private int type;
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
		 *            The nodes type as an integer value
		 * @param name
		 *            The POIs name
		 */
		POI(long id, double lat, double lon, int tag, String name) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.type = tag;
			this.name = name;
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

		public int getType() {
			return type;
		}

		public String getName() {
			return name;
		}

	}

}
