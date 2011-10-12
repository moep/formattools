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
package org.mapsforge.storage.debug;

public class TestMain {
	public static void main(String[] args) {
		RangeQueryCategoryFilter cf = new RangeQueryCategoryFilter();
		PoiCategory pc1 = new DoubleLinkedPoiCategory("root", null);
		PoiCategory pc2 = new DoubleLinkedPoiCategory("c1", pc1);
		PoiCategory pc3 = new DoubleLinkedPoiCategory("c2", pc1);

		cf.addCategory(pc2);
		cf.addCategory(pc3);

		System.out.println("String: " + cf.getSQLWhereClauseString());
	}
}
