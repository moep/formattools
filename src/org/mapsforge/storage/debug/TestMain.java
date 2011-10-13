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

import SQLite3.Exception;

public class TestMain {

	public static void traverseTree(PoiCategory cat) {
		System.out.println("<category title=\"" + cat.getTitle() + "\">");

		for (PoiCategory child : cat.getChildren()) {
			traverseTree(child);
		}

		System.out.println("</category>");
	}

	public static void main(String[] args) throws Exception {
		PoiCategory root = CategoryResolver.getRootCategory();

		// traverseTree(root);
		for (String key : CategoryResolver.categoryMap.keySet()) {
			System.out
					.println("<tns:Mapping categoryName=\""
							+ CategoryResolver.categoryMap.get(key).getTitle() + "\" tag=\""
							+ key
							+ "\" />");
		}

	}
}
