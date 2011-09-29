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

import org.mapsforge.poi.PoiCategory;

/**
 * A simple POI category representation.
 * 
 * @author Karsten Groll
 * 
 */
public class SimplePoiCategory implements PoiCategory {
	private String title;
	private PoiCategory parent;

	SimplePoiCategory(String title, PoiCategory parent) {
		this.title = title;
		this.parent = parent;
	}

	@Override
	public String getTitle() {
		return this.title;
	}

	@Override
	public PoiCategory getParent() {
		return this.parent;
	}

}
