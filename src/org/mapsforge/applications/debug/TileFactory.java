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
package org.mapsforge.applications.debug;

public class TileFactory {
	public static Tile getTileFromRawData(byte[] rawData, byte zoomInterval, MapFile mapFile) {

		Tile t = new Tile();
		Serializer s = new Serializer(rawData);

		System.out.println("Reading tile header");
		// H E A D E R

		// Tile signature (32B, optional)
		// ###TileStart
		if (mapFile.isDebugFlagSet()) {
			String signature = s.getNextString(32);
			System.out.println("Tile signature: " + signature);
			t.setTileSignature(signature);
		}

		// Zoom table (variable)
		for (int row = mapFile.getMinimalZoomLevel()[zoomInterval]; row <= mapFile
				.getMaximalZoomLevel()[zoomInterval]; row++) {
			t.addZoomTableRow(row, s.getNextShort(), s.getNextShort());
		}

		// First way offset (VBE-U)
		t.setFirstWayOffset(s.getNextVBEUInt());

		// FINISHED TILE //

		// P O I s
		System.out.println("This tile has " + t.getCumulatedNumberOfPoisOnZoomlevel(mapFile
				.getMaximalZoomLevel()[zoomInterval]) + " POIs");
		for (int poi = 0; poi < t.getCumulatedNumberOfPoisOnZoomlevel(mapFile
				.getMaximalZoomLevel()[zoomInterval]); poi++) {
			t.addPOI(getNextPOI(s, mapFile));
		}

		System.out.println("Ways");
		// W A Y S
		System.out.println("This tile has " + t.getCumulatedNumberOfWaysOnZoomLevel(mapFile
				.getMaximalZoomLevel()[zoomInterval]) + " ways");
		for (int way = 0; way < t.getCumulatedNumberOfWaysOnZoomLevel(mapFile
				.getMaximalZoomLevel()[zoomInterval]); way++) {
			t.addWay(getNextWay(s, mapFile));
		}

		return t;

	}

	private static POI getNextPOI(Serializer s, MapFile mapFile) {
		POI p = new POI();
		// POI signature (32B, optional)
		if (mapFile.isDebugFlagSet()) {
			String signature = s.getNextString(32);
			// System.out.println("POI Signature: " + signature);
			p.setPoiSignature(signature);
		}

		// Position (2 * VBE-S)
		p.setPosition(s.getNextVBESInt(), s.getNextVBESInt());

		// Special byte (1B)
		p.setSpecialByte(s.getNextByte());

		// POI tags (variable)
		for (byte j = 0; j < p.getAmountOfTags(); j++) {
			p.addTagID(s.getNextVBEUInt());
		}

		// Flags (1B)
		p.setFlags(s.getNextByte());

		// POI name (variable, optional)
		if (p.isPOINameFlagSet()) {
			p.setName(s.getNextString());
		}

		// POI elevation (VBE-S, optional)
		if (p.isElevationFlagSet()) {
			p.setElevation(s.getNextVBESInt());
		}

		// House number (String, optional)
		if (p.isHouseNumberFlagSet()) {
			p.setHouseNumber(s.getNextString());
		}

		return p;
	}

	private static Way getNextWay(Serializer s, MapFile mapFile) {
		Way w = new Way();

		// Way signature (32B, optional)
		if (mapFile.isDebugFlagSet()) {
			String signature = s.getNextString(32);
			// System.out.println("Way signature: " + signature);
			w.setWaySignature(signature);
		}

		// Way size (VBE-U)
		w.setWaySize(s.getNextVBEUInt());

		// Sub tile bitmap (2B)
		w.setSubTileBitmap(s.getNextByte(), s.getNextByte());

		// Special byte 1 (1B)
		w.setSpecialByte1(s.getNextByte());

		// Special byte 2 (1B)
		w.setSpecialByte2(s.getNextByte());

		// Way type bitmap (1B)
		w.setWayTypeBitmap(s.getNextByte());

		// Tag ID (n * VBE-U)
		for (byte tag = 0; tag < w.getAmountOfTags(); tag++) {
			w.addTagID(s.getNextVBEUInt());
		}

		// Way node amount (VBE-U)
		w.setWayNodeAmount(s.getNextVBEUInt());

		// First way node (2*VBE-S)
		w.setFirstWayNodeLatDiff(s.getNextVBESInt());
		w.setFirstWayNodeLonDiff(s.getNextVBESInt());

		// Way nodes (n*2*VBE-S)
		for (int i = 0; i < w.getWayNodeAmount() - 1; i++) {
			w.addWayNodesLatDiff(s.getNextVBESInt());
			w.addWayNodesLonDiff(s.getNextVBESInt());
		}

		// Flags (1B)
		w.setFlags(s.getNextByte());

		// Name (String, optional)
		if (w.isWayFlagSet()) {
			w.setName(s.getNextString());
		}

		// Reference (String, optional)
		if (w.isReferenceFlagSet()) {
			w.setReference(s.getNextString());
		}

		// Label position (2*VBE-S, optional)
		if (w.isLabelPositionFlagSet()) {
			w.setLabelPositionLatDiff(s.getNextVBESInt());
			w.setLabelPositionLonDiff(s.getNextVBESInt());
		}

		// Multipolygon (variable, optional)
		// TODO save multipolygon values
		if (w.isMultipolygonFlagSet()) {
			int amountOfInnerWays = s.getNextVBEUInt();

			// Remaining inner way nodes
			for (int i = 0; i < amountOfInnerWays; ++i) {
				int amountOfInnerWayNodes = s.getNextVBEUInt();

				for (int j = 0; j < amountOfInnerWayNodes; j++) {
					s.getNextVBESInt();
					s.getNextVBESInt();
				}

			}
		}

		return w;
	}
}
