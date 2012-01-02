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
package org.mapsforge.preprocessing.map.osmosis;

import java.util.List;

import org.mapsforge.preprocessing.map.osmosis.DeltaEncoder.Encoding;

/**
 * Class to store a WayDataBlock. Each WayDataBlock can store one way and a list of corresponding inner
 * ways. Simple ways and simple polygons have zero inner ways while multi polygons have one or more
 * inner ways.
 * 
 * @author sahin
 * 
 */
class WayDataBlock {
	private final List<Integer> outerWay;
	private final List<List<Integer>> innerWays;
	private final Encoding encoding;

	WayDataBlock(List<Integer> outerWay, List<List<Integer>> innerWays) {
		this.outerWay = outerWay;
		this.innerWays = innerWays;
		encoding = Encoding.NONE;
	}

	WayDataBlock(List<Integer> outerWay, List<List<Integer>> innerWays, Encoding encoding) {
		super();
		this.outerWay = outerWay;
		this.innerWays = innerWays;
		this.encoding = encoding;
	}

	public Encoding getEncoding() {
		return encoding;
	}

	public List<Integer> getOuterWay() {
		return outerWay;

	}

	public List<List<Integer>> getInnerWays() {
		return innerWays;
	}

}