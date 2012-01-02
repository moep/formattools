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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.Rect;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Bound;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

/**
 * An Osmosis plugin that reads OpenStreetMap data and converts it to a mapsforge binary file.
 * 
 * @author bross
 */
public class MapFileWriterTask implements Sink {
	private static final int VERSION_BINARY_FORMAT = 3;
	private static final String VERSION_LIBRARY = "0.2.4";

	private static final Logger LOGGER = Logger.getLogger(MapFileWriterTask.class.getName());

	private TileBasedDataStore tileBasedGeoObjectStore;
	static OSMTagMapping TAG_MAPPING;

	// temporary node data
	// IndexStore<Long, MapNode> indexStore;

	// Accounting
	private int amountOfNodesProcessed = 0;
	private int amountOfWaysProcessed = 0;
	private int amountOfRelationsProcessed = 0;
	private int amountOfMultipolygons = 0;

	// configuration parameters
	private File outFile;
	private GeoCoordinate mapStartPosition;
	private boolean debugInfo;
	// private boolean waynodeCompression;
	private boolean pixelFilter;
	private boolean polygonClipping;
	private boolean wayClipping;
	private String comment;
	private ZoomIntervalConfiguration zoomIntervalConfiguration;
	private String type;
	private int bboxEnlargement;
	private String preferredLanguage;

	MapFileWriterTask(String outFile, String bboxString, String mapStartPosition, String comment,
			String zoomIntervalConfigurationString, boolean debugInfo, boolean pixelFilter,
			boolean polygonClipping, boolean wayClipping, String type, int bboxEnlargement, String tagConfFile,
			String preferredLanguage) {
		this.outFile = new File(outFile);
		if (this.outFile.isDirectory()) {
			throw new IllegalArgumentException("file parameter points to a directory, must be a file");
		}

		LOGGER.info("mapfile-writer version " + VERSION_LIBRARY);

		this.mapStartPosition = mapStartPosition == null ? null : GeoCoordinate.fromString(mapStartPosition);
		this.debugInfo = debugInfo;
		// this.waynodeCompression = waynodeCompression;
		this.pixelFilter = pixelFilter;
		this.polygonClipping = polygonClipping;
		this.wayClipping = wayClipping;
		this.comment = comment;
		if (tagConfFile == null) {
			TAG_MAPPING = new OSMTagMapping(MapFileWriterTask.class.getClassLoader().getResource(
					"org/mapsforge/preprocessing/map/osmosis/tag-mapping.xml"));
		} else {
			File tagConf = new File(tagConfFile);
			if (tagConf.isDirectory()) {
				throw new IllegalArgumentException("tag-conf-file points to a directory, must be a file");
			}
			try {
				TAG_MAPPING = new OSMTagMapping(tagConf.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException(e);
			}
		}

		Rect bbox = bboxString == null ? null : Rect.fromString(bboxString);
		this.zoomIntervalConfiguration = zoomIntervalConfigurationString == null ? ZoomIntervalConfiguration
				.getStandardConfiguration() : ZoomIntervalConfiguration
				.fromString(zoomIntervalConfigurationString);

		this.type = type;
		if (!type.equalsIgnoreCase("ram") && !type.equalsIgnoreCase("hd"))
			throw new IllegalArgumentException("type argument must equal ram or hd, found: " + type);

		if (bbox != null) {
			if (type.equalsIgnoreCase("ram"))
				this.tileBasedGeoObjectStore = RAMTileBasedDataStore.newInstance(bbox,
						zoomIntervalConfiguration, bboxEnlargement);
			else
				this.tileBasedGeoObjectStore = HDTileBasedDataStore.newInstance(bbox,
						zoomIntervalConfiguration, bboxEnlargement);
		}
		this.bboxEnlargement = bboxEnlargement;
		this.preferredLanguage = preferredLanguage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.openstreetmap.osmosis.core.lifecycle.Completable#complete()
	 */
	@Override
	public final void complete() {
		NumberFormat nfMegabyte = NumberFormat.getInstance();
		NumberFormat nfCounts = NumberFormat.getInstance();
		nfCounts.setGroupingUsed(true);
		nfMegabyte.setMaximumFractionDigits(2);

		LOGGER.info("completing read...");
		tileBasedGeoObjectStore.complete();

		LOGGER.info("start writing file...");

		try {
			if (outFile.exists() && !outFile.isDirectory()) {
				LOGGER.info("overwriting file " + outFile.getAbsolutePath());
				outFile.delete();
			}
			RandomAccessFile file = new RandomAccessFile(outFile, "rw");
			MapFileWriter mfw = new MapFileWriter(tileBasedGeoObjectStore, file, bboxEnlargement);
			// mfw.writeFileWithDebugInfos(System.currentTimeMillis(), 1, (short) 256);
			mfw.writeFile(System.currentTimeMillis(), VERSION_BINARY_FORMAT, (short) 256, comment, debugInfo,
					polygonClipping, wayClipping, pixelFilter, mapStartPosition, preferredLanguage);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "error while writing file", e);
		}

		LOGGER.info("finished...");
		LOGGER.fine("total processed nodes: " + nfCounts.format(amountOfNodesProcessed));
		LOGGER.fine("total processed ways: " + nfCounts.format(amountOfWaysProcessed));
		LOGGER.fine("total processed relations: " + nfCounts.format(amountOfRelationsProcessed));
		LOGGER.fine("total processed multipolygons: " + amountOfMultipolygons);

		System.gc();
		LOGGER.info("estimated memory consumption: "
				+ nfMegabyte
						.format(+((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / Math
								.pow(1024, 2))) + "MB");
	}

	@Override
	public final void release() {
		tileBasedGeoObjectStore.release();
	}

	@Override
	public final void process(EntityContainer entityContainer) {

		Entity entity = entityContainer.getEntity();

		switch (entity.getType()) {

			case Bound:
				Bound bound = (Bound) entity;
				if (tileBasedGeoObjectStore == null) {
					if (type.equalsIgnoreCase("ram"))
						tileBasedGeoObjectStore = RAMTileBasedDataStore.newInstance(bound.getBottom(),
								bound.getTop(), bound.getLeft(), bound.getRight(), zoomIntervalConfiguration,
								bboxEnlargement);
					else
						tileBasedGeoObjectStore = HDTileBasedDataStore.newInstance(bound.getBottom(),
								bound.getTop(), bound.getLeft(), bound.getRight(), zoomIntervalConfiguration,
								bboxEnlargement);
				}
				LOGGER.info("start reading data...");
				break;

			// *******************************************************
			// ****************** NODE PROCESSING*********************
			// *******************************************************
			case Node:

				if (tileBasedGeoObjectStore == null) {
					LOGGER.severe("No valid bounding box found in input data.\n"
							+ "Please provide valid bounding box via command "
							+ "line parameter 'bbox=minLat,minLon,maxLat,maxLon'.\n"
							+ "Tile based data store not initialized. Aborting...");
					throw new IllegalStateException("tile based data store not initialized, missing bounding "
							+ "box information in input data");
				}
				tileBasedGeoObjectStore.addNode((Node) entity);
				// hint to GC
				entity = null;
				amountOfNodesProcessed++;
				break;

			// *******************************************************
			// ******************* WAY PROCESSING*********************
			// *******************************************************
			case Way:
				tileBasedGeoObjectStore.addWay((Way) entity);
				entity = null;
				amountOfWaysProcessed++;
				break;

			// *******************************************************
			// ****************** RELATION PROCESSING*********************
			// *******************************************************
			case Relation:
				Relation currentRelation = (Relation) entity;
				tileBasedGeoObjectStore.addRelation(currentRelation);
				amountOfRelationsProcessed++;
				entity = null;
				break;
			default:
				System.out.println(entity.getTags());
		}

	}
}
