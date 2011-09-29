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

import java.util.HashMap;

import org.mapsforge.poi.PoiCategory;

/**
 * The category resolver delivers a {@link PoiCategory} object for a given tag.
 * 
 * @author Karsten Groll
 * 
 */
public class CategoryResolver {

	private static final HashMap<String, PoiCategory> categoryMap = new HashMap<String, PoiCategory>();

	// See: http://wiki.openstreetmap.org/w/index.php?title=Key:amenity&oldid=608028

	static class Categories {
		// Root category
		static final PoiCategory ROOT = new SimplePoiCategory("All categories", null);
		// Amenity
		static final PoiCategory AMENITY_ROOT = new SimplePoiCategory("Amenity root category", ROOT);
		// Amenity : food
		static final PoiCategory AMENITY_SUSTENANCE = new SimplePoiCategory("Food amenities", AMENITY_ROOT);
		static final PoiCategory AMENITY_SUSTENANCE_RESTAURANT = new SimplePoiCategory("Restaurant", AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_FOOD_COURT = new SimplePoiCategory(
				"Area with several different food counters",
				AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_FAST_FOOD = new SimplePoiCategory("Fast food restaurant", AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_DRINKING_WATER = new SimplePoiCategory("A source of drinking water",
				AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_BBQ = new SimplePoiCategory("A public grill for cooking meat or vegetables",
				AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_PUB = new SimplePoiCategory(
				"A place selling alcoholic beverages and sometimes even food",
				AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_BAR = new SimplePoiCategory("A place for getting drunk at",
				AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_CAFE = new SimplePoiCategory("A cafe", AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_BEERGARDEN = new SimplePoiCategory("A beer garden.", AMENITY_SUSTENANCE);
		static final PoiCategory AMENITY_SUSTENANCE_ICE_CREAM = new SimplePoiCategory("A shop that sells ice cream.",
				AMENITY_SUSTENANCE);
		// Amenity : education
		static final PoiCategory AMENITY_EDUCATION = new SimplePoiCategory("Education institutes", AMENITY_ROOT);
		// TODO add categories
		// Amenity : transportation
		static final PoiCategory AMENITY_TRANSPORTATION = new SimplePoiCategory("Education institutes", AMENITY_ROOT);
		// TODO add categories
		// Amenity : financial
		static final PoiCategory AMENITY_FINANCIAL = new SimplePoiCategory("Financial institutes", AMENITY_ROOT);
		// TODO add categories
		// Amenity : healthcare
		static final PoiCategory AMENITY_HEALTHCARE = new SimplePoiCategory("Healthcare", AMENITY_ROOT);
		// TODO add categories
		// Amenity : entertainment
		static final PoiCategory AMENITY_ENTERTAINMENT = new SimplePoiCategory("Entertainment, arts and culture", AMENITY_ROOT);
		// TODO add categories
		// Amenity : education
		static final PoiCategory AMENITY_OTHER = new SimplePoiCategory("Other amenities", AMENITY_ROOT);
		// TODO add categories

		// TODO non-amenity categories
	}

	static {

		//
		// Mappings
		//

		// Root category
		categoryMap.put("*", Categories.ROOT);

		// Amenity
		categoryMap.put("amenity=*", Categories.AMENITY_ROOT);
		categoryMap.put("amenity=", Categories.AMENITY_ROOT);

		// Amenity : food
		categoryMap.put("amenity=[food]", Categories.AMENITY_SUSTENANCE);
		categoryMap.put("amenity=[sustenance]", Categories.AMENITY_SUSTENANCE);

		categoryMap.put("amenity=restaurant", Categories.AMENITY_SUSTENANCE_RESTAURANT);
		categoryMap.put("amenity=food_court", Categories.AMENITY_SUSTENANCE_FOOD_COURT);
		categoryMap.put("amenity=fast_food", Categories.AMENITY_SUSTENANCE_FAST_FOOD);
		categoryMap.put("amenity=drinking_water", Categories.AMENITY_SUSTENANCE_DRINKING_WATER);
		categoryMap.put("amenity=bbq", Categories.AMENITY_SUSTENANCE_BBQ);
		categoryMap.put("amenity=pub", Categories.AMENITY_SUSTENANCE_PUB);
		categoryMap.put("amenity=bar", Categories.AMENITY_SUSTENANCE_BAR);
		categoryMap.put("amenity=cafe", Categories.AMENITY_SUSTENANCE_CAFE);
		categoryMap.put("amenity=biergarten", Categories.AMENITY_SUSTENANCE_BEERGARDEN);
		categoryMap.put("amenity=ice_cream", Categories.AMENITY_SUSTENANCE_ICE_CREAM);
	}

	/**
	 * 
	 * @param key
	 *            The tags key (e.g. 'amenity')
	 * @param value
	 *            The tags value (e.g. 'fuel');
	 * @return The submost category for a given tag.
	 * @throws UnknownCategoryException
	 *             when there is no matching category.
	 */
	static PoiCategory getPoiCategoryByTag(String key, String value) throws UnknownCategoryException {
		if (!categoryMap.containsKey(key + "=" + value)) {
			throw new UnknownCategoryException();
		}

		return categoryMap.get(key + "=" + value);
	}
}
