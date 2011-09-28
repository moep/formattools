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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class POIWriterTask implements Sink {
	private static Logger LOGGER = Logger.getLogger(POIWriterTask.class.getName());
	private static final String VERSION = "0.3-experimental";

	private RAMPoiStore poiStore;

	// Temporary variables
	String[] data;

	public POIWriterTask() {
		LOGGER.info("Mapsforge mapfile writer version " + VERSION);
		LOGGER.setLevel(Level.FINE);

		this.poiStore = new RAMPoiStore();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void complete() {
		int numPOIsWritten = poiStore.getNumberOfPOIs();
		LOGGER.info("Writing POIs to file...");
		// TODO use parameter as path
		poiStore.writeToSQLiteDB("/home/moep/germany.poi");
		LOGGER.info("Finished writing " + numPOIsWritten + " POIs to file.");

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void release() {

	}

	// @Override
	// public void process(EntityContainer entityContainer) {
	// // TODO Auto-generated method stub
	//
	// }

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
		// Only add nodes that have data
		if (n.getTags().size() != 0) {
			// TODO read key / value pairs
			poiStore.addPOI(n.getId(), n.getLatitude(), n.getLongitude(), new String[] { "bla=blubb" });
		}
	}
}
