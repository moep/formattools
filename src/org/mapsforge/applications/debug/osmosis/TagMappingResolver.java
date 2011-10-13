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

import java.io.File;
import java.util.HashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.mapsforge.applications.debug.osmosis.jaxb.TagMappings;
import org.mapsforge.applications.debug.osmosis.jaxb.TagMappings.Mapping;
import org.mapsforge.storage.debug.PoiCategory;
import org.mapsforge.storage.debug.PoiCategoryManager;
import org.mapsforge.storage.debug.UnknownCategoryException;

/**
 * This class maps a given tag (e.g. amenity=restaurant) to a certain {@link PoiCategory}. The mapping
 * configuration is read from a XML file.
 * 
 * @author Karsten Groll
 * 
 */
class TagMappingResolver {
	private final PoiCategoryManager categoryManager;

	/** Maps a tag to a category's title */
	private HashMap<String, String> tagMap;

	/**
	 * 
	 * @param configFilePath
	 *            Path to the XML file containing the tag to POI mappings.
	 */
	TagMappingResolver(String configFilePath, PoiCategoryManager categoryManager) {
		this.categoryManager = categoryManager;
		this.tagMap = new HashMap<String, String>();

		// Read root category from XML
		final File f = new File(configFilePath);

		JAXBContext ctx = null;
		Unmarshaller um = null;
		TagMappings mappings = null;

		try {
			ctx = JAXBContext.newInstance(TagMappings.class);
			um = ctx.createUnmarshaller();
			mappings = (TagMappings) um.unmarshal(f);
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		System.out.println("Adding tag mappings");
		for (Mapping m : mappings.getMapping()) {
			System.out.println(m.getTag() + " ==> " + m.getCategoryName());
			this.tagMap.put(m.getTag(), m.getCategoryName());
		}

	}

	PoiCategory getCategoryFromTag(String tag) throws UnknownCategoryException {
		String categoryName = this.tagMap.get(tag);
		// Tag not found?
		if (categoryName == null) {
			return null;
		}

		PoiCategory ret = this.categoryManager.getPoiCategoryByTitle(categoryName);

		return ret;
	}
}