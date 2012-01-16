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
package org.mapsforge.storage.atoms;

/**
 * A (reusable) class for storing simple way information such as its coordinates and its type.
 * 
 * @author Karsten Groll.
 * 
 */
public class Way {
	/**
	 * The way's absolute coordinates as latitude / longitude pairs (
	 * <code>[lat_0, lon_0, ...,lat_n, lon_n]</code>).
	 */
	private long[] coordinates;
	private String name;
	private int hash = -1;
	private long id = 0;

	public Way(long[] coordinates, String name, long id) {
		this.coordinates = coordinates;
		this.name = name;
		this.id = id;
		rehash();
	}

	public Way(long[] coordinates, String name) {
		this(coordinates, name, 0);
	}

	public long[] getCoordinates() {
		return this.coordinates;
	}

	public String getName() {
		return this.name;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void rehash() {
		if (this.coordinates == null) {
			this.hash = -1;
			return;
		}

		int prime = 31;
		this.hash = 0;

		for (int i = 0; i < coordinates.length; i++) {
			this.hash = (int) (prime * this.hash + coordinates[i]);
		}

	}

	@Override
	public int hashCode() {
		return this.hash;
	}

	@Override
	public boolean equals(Object way) {
		return way instanceof Way && this.hashCode() == way.hashCode();
	}

	public static void main(String[] args) {
		Way w = new Way(new long[] { 52000000, 13000000, 52100000, 13100000 }, null);
		Way w2 = new Way(new long[] { 52000000, 13000000, 52100000, 13200000 }, null);
		System.out.println(w.equals(w2));
	}

}
