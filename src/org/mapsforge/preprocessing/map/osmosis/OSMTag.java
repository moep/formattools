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

class OSMTag {

	private static final String KEY_VALUE_SEPARATOR = "=";

	private final short id;
	private final String key;
	private final String value;
	private final byte zoomAppear;
	private final boolean renderable;

	OSMTag(short id, String key, String value, byte zoomAppear, boolean renderable) {
		super();
		this.id = id;
		this.key = key;
		this.value = value;
		this.zoomAppear = zoomAppear;
		this.renderable = renderable;
	}

	OSMTag(short id, String key, String value, byte zoomAppear) {
		super();
		this.id = id;
		this.key = key;
		this.value = value;
		this.zoomAppear = zoomAppear;
		this.renderable = true;
	}

	static OSMTag fromOSMTag(OSMTag osmTag, short newID) {
		return new OSMTag(newID, osmTag.getKey(), osmTag.getValue(), osmTag.getZoomAppear(),
				osmTag.isRenderable());
	}

	short getId() {
		return id;
	}

	String getKey() {
		return key;
	}

	String getValue() {
		return value;
	}

	byte getZoomAppear() {
		return zoomAppear;
	}

	boolean isRenderable() {
		return renderable;
	}

	boolean isCoastline() {
		return key.equals("natural") && value.equals("coastline");
	}

	String tagKey() {
		return key + KEY_VALUE_SEPARATOR + value;
	}

	static String tagKey(String key, String value) {
		return key + KEY_VALUE_SEPARATOR + value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OSMTag other = (OSMTag) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "OSMTag [id=" + id + ", key=" + key + ", value=" + value + ", zoomAppear="
				+ zoomAppear + ", renderable=" + renderable + "]";
	}

}
