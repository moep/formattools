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

import org.mapsforge.poi.PoiCategory;

/**
 * White list category filter that allows all categories and their sub-categories in the white list.
 * 
 * @author Karsten Groll
 * 
 */
public class SimpleCategoryFilter implements CategoryFilter {

	private final List whiteList;

	/**
	 * Default constructor.
	 */
	public SimpleCategoryFilter() {
		whiteList = new LinkedList<PoiCategory>();
	}

	/**
	 * Adds a POI category to the white list. A parent category (e.g. amenity_food) automatically white
	 * lists its sub-categories like amenity_food_fast_food.
	 * 
	 * @param category
	 *            The category to be added to the white list.
	 */
	public void addCategory(PoiCategory category) {
		this.whiteList.add(category);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAcceptedCategory(PoiCategory category) {
		// Found category
		if (this.whiteList.contains(category)) {
			return true;
		} else {
			// Search for parent category
			return isAcceptedCategory(category.getParent());
		}
	}

}
