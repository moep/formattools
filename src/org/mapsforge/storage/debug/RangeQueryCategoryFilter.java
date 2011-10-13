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

public class RangeQueryCategoryFilter extends SimpleCategoryFilter {

	@Override
	public boolean isAcceptedCategory(PoiCategory category) {
		return super.isAcceptedCategory(category);
	}

	@Override
	public void addCategory(PoiCategory category) {
		super.addCategory(category);

	}

	/**
	 * 
	 * @return A string like <code>WHERE id BETWEEN 2 AND 5 OR BETWEEN 10 AND 12</code>.
	 */
	String getSQLWhereClauseString() {
		int[] intervals = getCategoryIDIntervals();

		if (intervals.length == 0) {
			return "";
		}

		StringBuilder sb = new StringBuilder();
		sb.append(" WHERE ");
		// foreach interval
		for (int i = 0; i < intervals.length; i += 2) {
			System.out.println("Processing interval: (" + intervals[i] + ", " + intervals[i + 1] + ")");
			sb.append("id BETWEEN ").append(intervals[i]).append(" AND ").append(intervals[i + 1]);

			// append OR if it is not the last interval
			if (i != intervals.length - 2) {
				sb.append(" OR ");
			}
		}

		return sb.toString();
	}

	private int[] getCategoryIDIntervals() {
		int[] ret = new int[this.whiteList.size() * 2];

		int i = 0;
		PoiCategory[] siblings = null;

		for (PoiCategory c : this.whiteList) {
			siblings = getSiblings(c);
			ret[i] = siblings[0].getID();
			ret[i + 1] = siblings[1].getID();
			i += 2;
		}

		return ret;
	}

	/**
	 * 
	 * @param c1
	 *            Category whose siblings should be returned.
	 * @return Array: [left sibling, right sibling]
	 */
	private PoiCategory[] getSiblings(PoiCategory c1) {
		PoiCategory ret[] = new PoiCategory[2];
		ret[0] = c1;
		ret[1] = c1;

		for (PoiCategory c : c1.getParent().getChildren()) {

			if (c.getID() < c1.getID()) {
				ret[0] = c;
			}

			if (c.getID() > c1.getID()) {
				ret[1] = c;
			}
		}

		return ret;
	}

	/**
	 * 
	 * @return Array of POI categories sorted by their IDs in descending order.
	 */
	private PoiCategory[] getSortedCategories() {
		int[] ids = new int[this.whiteList.size()];
		PoiCategory[] ret = new PoiCategory[ids.length];

		for (int i = 0; i < ids.length; i++) {
			// ids[i] = whiteList.get(i).getID();
		}

		return ret;

	}

}
