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

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class POIWriterTask implements Sink {
	private static final Logger LOGGER = Logger.getLogger(POIWriterTask.class.getName());
	private static final String VERSION = "0.3-experimental";

	// Parameters
	private final String outputFilePath;
	private final RAMPoiStore poiStore;

	// Temporary variables
	String[] data;

	// Accepted categories
	private final CategoryFilter categoryFilter;

	// Statistics
	private int nodesAdded = 0;
	private int nodesSkipped = 0;

	public POIWriterTask(String outputFilePath) {
		LOGGER.info("Mapsforge mapfile writer version " + VERSION);
		LOGGER.setLevel(Level.FINE);

		this.outputFilePath = outputFilePath;

		this.poiStore = new RAMPoiStore();

		// Set accepted categories
		this.categoryFilter = new SimpleCategoryFilter();
		this.categoryFilter.addCategory(CategoryResolver.Categories.AMENITY_ROOT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		int numPOIsWritten = poiStore.getNumberOfPOIs();
		LOGGER.info("Writing POIs to file...");
		poiStore.writeToSQLiteDB(outputFilePath);
		LOGGER.info("Finished writing " + numPOIsWritten + " POIs to file.");
		LOGGER.info("Skipped " + this.nodesSkipped + " nodes.");
		LOGGER.info("Added " + this.nodesAdded + " nodes.");
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
			String tag = "TYPE";
			String name = "NAME";

			// Set to true when valid key value pair has been found
			boolean accepted = false;
			for (Iterator<Tag> it = n.getTags().iterator(); it.hasNext();) {
				t = it.next();

				// Determine the POI's type
				if (t.getKey().equals("amenity")) {
					tag = t.getKey() + "=" + t.getValue();
				}

				// Determine the POI's name
				if (t.getKey().equals("name")) {
					name = t.getValue();
				}

				// Apply white list filter
				try {
					accepted = categoryFilter.isAcceptedCategory(CategoryResolver.getPoiCategoryByTag(t.getKey(), t.getValue()));
				} catch (UnknownCategoryException e) {
					// ignore it
				}

			}

			// Add POI if its category is whitelisted
			if (accepted) {
				poiStore.addPOI(n.getId(), n.getLatitude(), n.getLongitude(), tag, name);
				// System.out.println("Node added: " + tag);
				++this.nodesAdded;
			} else {
				++this.nodesSkipped;
			}
		}
	}
}
