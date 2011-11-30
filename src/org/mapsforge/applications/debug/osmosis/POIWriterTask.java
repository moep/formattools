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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.storage.poi.PoiCategory;
import org.mapsforge.storage.poi.PoiCategoryFilter;
import org.mapsforge.storage.poi.PoiCategoryManager;
import org.mapsforge.storage.poi.SimplePoiCategoryFilter;
import org.mapsforge.storage.poi.UnknownPoiCategoryException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class POIWriterTask implements Sink {
	private static final Logger LOGGER = Logger.getLogger(POIWriterTask.class.getName());
	private static final String VERSION = "0.3-experimental";

	// For debug purposes only (at least for now)
	private static final boolean INCLUDE_META_DATA = false;

	// Parameters
	private final String outputFilePath;
	private final String categoryConfigPath;

	// Temporary variables
	String[] data;

	// Available categories
	private final PoiCategoryManager cm;

	// Mappings
	private final TagMappingResolver tagMappingResolver;

	// Accepted categories
	private final PoiCategoryFilter categoryFilter;

	// Statistics
	private int nodesAdded = 0;

	// Database
	private Connection conn = null;
	private PreparedStatement pStmt = null;
	private PreparedStatement pStmt2 = null;
	private PreparedStatement pStmt3 = null;
	private Statement stmt = null;

	public POIWriterTask(String outputFilePath, String categoryConfigPath) {
		LOGGER.info("Mapsforge mapfile writer version " + VERSION);
		LOGGER.setLevel(Level.FINE);

		this.outputFilePath = outputFilePath;
		this.categoryConfigPath = categoryConfigPath;

		// Get categories defined in XML
		this.cm = new XMLPoiCategoryManager(categoryConfigPath);

		// Get tag -> POI mapper
		this.tagMappingResolver = new TagMappingResolver(categoryConfigPath, this.cm);

		// Set accepted categories
		this.categoryFilter = new SimplePoiCategoryFilter();
		try {
			this.categoryFilter.addCategory(this.cm.getRootCategory());
		} catch (UnknownPoiCategoryException e) {
			LOGGER.warning("Could not add category to filter: " + e.getMessage());
		}

		// Create database and add categories
		try {
			prepareDatabase(outputFilePath);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (UnknownPoiCategoryException e) {
			e.printStackTrace();
		}

	}

	private void prepareDatabase(String path) throws ClassNotFoundException, SQLException, UnknownPoiCategoryException {
		Class.forName("SQLite.JDBC");
		this.conn = DriverManager.getConnection("jdbc:sqlite:/" + path);
		this.conn.setAutoCommit(false);

		this.stmt = conn.createStatement();
		this.pStmt = conn.prepareStatement("INSERT INTO poi_index VALUES (?, ?, ?, ?, ?);");
		this.pStmt2 = conn.prepareStatement("INSERT INTO poi_data VALUES (?, ?, ?);");
		this.pStmt3 = conn.prepareStatement("INSERT INTO poi_categories VALUES (?, ?, ?);");

		// CREATE TABLES
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_index;");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_data;");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS poi_categories;");
		// stmt.executeUpdate("DROP INDEX IF EXISTS poi_categories_index;");
		this.stmt.executeUpdate("CREATE VIRTUAL TABLE poi_index USING rtree(id, minLat, maxLat, minLon, maxLon);");
		this.stmt.executeUpdate("CREATE TABLE poi_data (id LONG, data BLOB, category INT, PRIMARY KEY (id));");
		// TODO model child / parent relationships for categories (foreign keys)
		this.stmt.executeUpdate("CREATE TABLE poi_categories (id INTEGER, name VARCHAR, parent INTEGER, PRIMARY KEY (id));");
		// stmt.executeUpdate("CREATE INDEX poi_categories_index ON poi_categories (id);");
		this.conn.commit();

		// INSERT CATEGORIES
		PoiCategory root = this.cm.getRootCategory();
		this.pStmt3.setLong(1, root.getID());
		this.pStmt3.setBytes(2, root.getTitle().getBytes());
		this.pStmt3.setNull(3, 0);
		this.pStmt3.addBatch();

		Stack<PoiCategory> children = new Stack<PoiCategory>();
		children.push(root);
		while (!children.isEmpty()) {
			for (PoiCategory c : children.pop().getChildren()) {
				this.pStmt3.setLong(1, c.getID());
				this.pStmt3.setBytes(2, c.getTitle().getBytes());
				this.pStmt3.setInt(3, c.getParent().getID());
				this.pStmt3.addBatch();
				children.push(c);
			}
		}
		this.pStmt3.executeBatch();
		this.conn.commit();

	}

	private void writePOI(long id, double latitude, double longitude, HashMap<String, String> poiData, PoiCategory category) {
		try {
			// Index data
			pStmt.setLong(1, id);
			pStmt.setDouble(2, latitude);
			pStmt.setDouble(3, longitude);
			pStmt.setDouble(4, latitude);
			pStmt.setDouble(5, longitude);

			// POI data
			pStmt2.setLong(1, id);

			// If all important data should be written to db
			if (INCLUDE_META_DATA) {
				pStmt2.setBytes(2, tagsToString(poiData).getBytes());
			} else {

				// If name tag is set
				if (poiData.get("name") != null) {
					pStmt2.setBytes(2, poiData.get("name").getBytes());
				} else {
					pStmt2.setNull(2, 0);
				}
			}

			pStmt2.setInt(3, category.getID());
			pStmt2.addBatch();
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void commit() {
		try {
			pStmt.executeBatch();
			pStmt2.executeBatch();
			conn.commit();
			this.conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		commit();
		LOGGER.info("Added " + this.nodesAdded + " POIs");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {
		// do nothing here
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void process(EntityContainer entityContainer) {
		Entity e = entityContainer.getEntity();
		LOGGER.finest("Processing entity: " + e.toString());

		if (e instanceof Node) {
			processNode((Node) e);
		}
	}

	private void processNode(Node n) {
		// TODO filter tags
		// Only add nodes that have data
		if (n.getTags().size() != 0) {

			// for (String key : n.getMetaTags().keySet()) {
			// System.out.println("Meta tag: " + key + " -> " + n.getMetaTags().get(key));
			// }

			Tag t = null;
			PoiCategory pc = null;
			HashMap<String, String> tagMap = new HashMap<String, String>(20, 1.0f);
			// String name = null;
			// String url = null;
			String tag = null;

			// Get nodes tag and name / URL
			for (Iterator<Tag> it = n.getTags().iterator(); it.hasNext();) {
				t = it.next();

				// Determine the POI's type
				if (t.getKey().equals("amenity")) {
					tag = t.getKey() + "=" + t.getValue();
				}

				// Determine the POI's name
				// if (t.getKey().equals("name")) {
				// name = t.getValue();
				// }

				tagMap.put(t.getKey(), t.getValue());
			}

			// Check if there is a POI category for this tag and add POI to DB
			try {
				// Get category from tag
				if (tag != null) {
					pc = this.tagMappingResolver.getCategoryFromTag(tag);
				}

				// Add node if its category matches
				if (pc != null && this.categoryFilter.isAcceptedCategory(pc)) {

					writePOI(n.getId(), n.getLatitude(), n.getLongitude(), tagMap, pc);

					++this.nodesAdded;
				}
			} catch (UnknownPoiCategoryException e) {
				LOGGER.warning("The '" + tag + "' refers to a POI that does not exist: " + e.getMessage());
			}
		}
	}

	private String tagsToString(HashMap<String, String> tagMap) {
		StringBuilder sb = new StringBuilder();

		for (String key : tagMap.keySet()) {

			// Skip some tags
			if (key.equalsIgnoreCase("amenity") ||
					key.equalsIgnoreCase("created_by")) {
				continue;
			}

			sb.append(key).append('=').append(tagMap.get(key)).append(';');
		}

		return sb.toString();
	}

}
