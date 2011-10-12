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

/**
 * A category manager is a storage for {@link PoiCategory}s. It manages the categories' hierarchy in a
 * tree structure. Adding and deleting categories should be done via this interface.
 * 
 * @author Karsten Groll
 * 
 */
public interface PoiCategoryManager {

	// /**
	// * Loads a category configuration from a file or a database.
	// */
	// public void loadCategories();

	/**
	 * Stores a category configuration in a file or a database.
	 */
	public void storeCategories();

	/**
	 * 
	 * @param id
	 *            A categories ID.
	 * @return The category having this ID or null.
	 */
	public PoiCategory getPoiCategoryByID(int id);

	/**
	 * 
	 * @param title
	 *            The category's title
	 * @return The category c with <code>c.title.equalsIgnoreCase(title)</code>.
	 */
	public PoiCategory getPoiCategoryByTitle(String title);

	/**
	 * 
	 * @return The tree's root category or null if the tree is empty.
	 */
	public PoiCategory getRootCategory();
}
