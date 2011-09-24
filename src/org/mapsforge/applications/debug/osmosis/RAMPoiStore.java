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

import java.util.LinkedList;
import java.util.List;

/**
 * A container class for storing POI data in a set.
 * 
 * @author Karsten Groll
 * 
 */
public class RAMPoiStore {
	List<POI> pois;

	public RAMPoiStore() {
		this.pois = new LinkedList<POI>();
	}

	/**
	 * Adds a POI to the temporary data storage.
	 * 
	 * @param lat
	 *            The node's latitude.
	 * @param lon
	 *            The node's longitude.
	 * @param data
	 *            The nodes tags stored as an array of <code>key=value</code> strings.
	 */
	public void addPOI(double lat, double lon, String[] data) {
		pois.add(new POI(lat, lon, data));
	}

	/**
	 * Writes the POI data into an SQLite DB.
	 */
	public void writeToSQLiteDB() {

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
		private double lat;
		private double lon;
		private String[] keys;
		private String[] values;

		/**
		 * 
		 * @param lat
		 *            The node's latitude.
		 * @param lon
		 *            The node's longitude.
		 * @param data
		 *            The nodes tags stored as an array of <code>key=value</code> strings.
		 */
		POI(double lat, double lon, String[] data) {
			this.lat = lat;
			this.lon = lon;

			this.keys = new String[data.length];
			this.values = new String[data.length];
			for (int i = 0; i < data.length; i++) {
				keys[i] = data[i].split("=")[0];
				values[i] = data[i].split("=")[1];
			}
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
