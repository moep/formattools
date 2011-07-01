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
package org.mapsforge.applications.debug;

/**
 * A simple tuple data structure that can be used without getters or setters.
 * 
 * @author Karsten Groll
 * 
 * @param <T1>
 *            The first element's type.
 * @param <T2>
 *            The second element's type.
 */
public class Tuple<T1, T2> {
	/** The tuple's first element. */
	public T1 e1;
	/** The tuple's last element. */
	public T2 e2;

	/**
	 * 
	 * @param e1
	 *            The tuple's first element.
	 * @param e2
	 *            The tuple's second element.
	 */
	public Tuple(T1 e1, T2 e2) {
		this.setE1(e1);
		this.setE2(e2);
	}

	/**
	 * Returns the first element.
	 * 
	 * @return the first element.
	 */
	public T1 getE1() {
		return e1;
	}

	/**
	 * Sets the first element.
	 * 
	 * @param e1
	 *            the first element.
	 */
	public void setE1(T1 e1) {
		this.e1 = e1;
	}

	/**
	 * Returns the second element.
	 * 
	 * @return the second element.
	 */
	public T2 getE2() {
		return e2;
	}

	/**
	 * Sets the second element.
	 * 
	 * @param e2
	 *            the second element.
	 */
	public void setE2(T2 e2) {
		this.e2 = e2;
	}
}