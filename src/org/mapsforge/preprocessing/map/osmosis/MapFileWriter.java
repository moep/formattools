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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.mapsforge.core.GeoCoordinate;
import org.mapsforge.core.MercatorProjection;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDNode;
import org.mapsforge.preprocessing.map.osmosis.TileData.TDWay;
import org.mapsforge.storage.dataExtraction.MapFileMetaData;
import org.mapsforge.storage.tile.TileDataContainer;
import org.mapsforge.storage.tile.TilePersistenceManager;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Writes the binary file format for mapsforge maps.
 * 
 * @author bross
 */
class MapFileWriter {

	private static final int BYTES_SHORT = 2;
	// private static final int BYTES_INTEGER = 4;

	private static final int DEBUG_BLOCK_SIZE = 32;

	private static final String DEBUG_INDEX_START_STRING = "+++IndexStart+++";

	private static final int SIZE_ZOOMINTERVAL_CONFIGURATION = 19;

	// private static final int PIXEL_COMPRESSION_MAX_DELTA = 5;

	private static final int BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE = 5;

	private static final String MAGIC_BYTE = "mapsforge binary OSM";

	private static final int OFFSET_FILE_SIZE = 28;

	private static final CoastlineHandler COASTLINE_HANDLER = new CoastlineHandler();

	// DEBUG STRINGS
	private static final String DEBUG_STRING_POI_HEAD = "***POIStart";
	private static final String DEBUG_STRING_POI_TAIL = "***";
	private static final String DEBUG_STRING_TILE_HEAD = "###TileStart";
	private static final String DEBUG_STRING_TILE_TAIL = "###";
	private static final String DEBUG_STRING_WAY_HEAD = "---WayStart";
	private static final String DEBUG_STRING_WAY_TAIL = "---";

	// bitmap flags for pois and ways
	private static final short BITMAP_NAME = 128;

	// bitmap flags for pois
	private static final short BITMAP_ELEVATION = 64;
	private static final short BITMAP_HOUSENUMBER = 32;

	// bitmap flags for ways
	private static final short BITMAP_REF = 64;
	private static final short BITMAP_LABEL = 32;
	// private static final short BITMAP_MULTIPOLYGON = 16;
	// private static final short BITMAP_POLYGON = 8;

	// bitmap flags for file features
	private static final short BITMAP_DEBUG = 128;
	private static final short BITMAP_MAP_START_POSITION = 64;

	private static final int BITMAP_INDEX_ENTRY_WATER = 0x80;

	private static final Logger LOGGER = Logger.getLogger(MapFileWriter.class.getName());

	private static final String PROJECTION = "Mercator";

	// private static final byte MAX_ZOOMLEVEL_PIXEL_FILTER = 11;

	// private static final byte MIN_ZOOMLEVEL_POLYGON_CLIPPING = 8;

	private static final Charset UTF8_CHARSET = Charset.forName("utf8");

	private static final float PROGRESS_PERCENT_STEP = 10f;

	// data
	private TileBasedDataStore dataStore;

	private static final TileInfo TILE_INFO = TileInfo.getInstance();
	// private static final CoastlineHandler COASTLINE_HANDLER = new CoastlineHandler();

	// IO
	private static final int HEADER_BUFFER_SIZE = 0x100000; // 1MB
	private static final int MIN_TILE_BUFFER_SIZE = 0xF00000; // 15MB
	private static final int COMPRESSED_TILES_BUFFER_SIZE = 0x3200000; // 50MB
	private static final int TILE_BUFFER_SIZE = 0xA00000; // 10MB
	private static final int WAY_BUFFER_SIZE = 0x100000; // 1MB
	private static final int POI_BUFFER_SIZE = 0x100000; // 1MB

	// private final RandomAccessFile randomAccessFile;
	private TilePersistenceManager tilePersistenceManager;
	private MapFileMetaData mapFileMetaData;

	// accounting
	private double tilesProcessed = 0;
	private double amountOfTilesInPercentStep;
	private long emptyTiles = 0;
	private long maxTileSize = 0;
	private long cumulatedTileSizeOfNonEmptyTiles = 0;
	private int maxWaysPerTile = 0;
	private int cumulatedNumberOfWaysInTiles = 0;

	private int posZoomIntervalConfig;
	final int bboxEnlargement;

	MapFileWriter(TileBasedDataStore dataStore, TilePersistenceManager tpm, int bboxEnlargement) {
		super();
		this.dataStore = dataStore;
		this.tilePersistenceManager = tpm;
		this.mapFileMetaData = new MapFileMetaData();
		amountOfTilesInPercentStep = Math.ceil(dataStore.cumulatedNumberOfTiles() / PROGRESS_PERCENT_STEP);
		this.bboxEnlargement = bboxEnlargement;
	}

	final void writeFile(long date, int version, short tilePixel, String comment, boolean debugStrings,
			boolean polygonClipping, boolean wayClipping, boolean pixelCompression,
			GeoCoordinate mapStartPosition, String preferredLanguage) throws IOException {

		// CONTAINER HEADER
		long totalHeaderSize = writeContainerHeader(date, version, tilePixel, comment, debugStrings,
				mapStartPosition, preferredLanguage);

		int amountOfZoomIntervals = dataStore.getZoomIntervalConfiguration().getNumberOfZoomIntervals();

		// SUB FILES
		// for each zoom interval write a sub file
		long currentFileSize = totalHeaderSize;
		for (int i = 0; i < amountOfZoomIntervals; i++) {
			// SUB FILE INDEX AND DATA
			writeSubfile(i, debugStrings, polygonClipping, wayClipping,
					pixelCompression);
			writeSubfileMetaDataToContainerHeader(i);
		}

		// randomAccessFile.seek(posZoomIntervalConfig);
		// randomAccessFile.write(containerB);

		// WRITE FILE SIZE TO HEADER
		// long fileSize = randomAccessFile.length();
		// randomAccessFile.seek(OFFSET_FILE_SIZE);
		// randomAccessFile.writeLong(fileSize);

		this.tilePersistenceManager.setMetaData(this.mapFileMetaData);
		// randomAccessFile.close();

		LOGGER.info("Finished writing file.");

		LOGGER.fine("number of empty tiles: " + emptyTiles);
		LOGGER.fine("percentage of empty tiles: " + (float) emptyTiles / dataStore.cumulatedNumberOfTiles());
		LOGGER.fine("cumulated size of non-empty tiles: " + cumulatedTileSizeOfNonEmptyTiles);
		LOGGER.fine("average tile size of non-empty tile: " + (float) cumulatedTileSizeOfNonEmptyTiles
				/ (dataStore.cumulatedNumberOfTiles() - emptyTiles));
		LOGGER.fine("maximum size of a tile: " + maxTileSize);
		LOGGER.fine("cumulated number of ways in all non-empty tiles: " + cumulatedNumberOfWaysInTiles);
		LOGGER.fine("maximum number of ways in a tile: " + maxWaysPerTile);
		LOGGER.fine("average number of ways in non-empty tiles: " + (float) cumulatedNumberOfWaysInTiles
				/ (dataStore.cumulatedNumberOfTiles() - emptyTiles));
	}

	// private void writeByteArray(int pos, byte[] array, ByteBuffer buffer) {
	// int currentPos = buffer.position();
	// buffer.position(pos);
	// buffer.put(array);
	// buffer.position(currentPos);
	// }

	private static void writeUTF8(String string, ByteBuffer buffer) {
		buffer.put(Serializer.getVariableByteUnsigned(string.getBytes(UTF8_CHARSET).length));
		buffer.put(string.getBytes(UTF8_CHARSET));
	}

	// TODO cast to void
	private long writeContainerHeader(long date, int version, short tilePixel, String comment,
			boolean debugStrings, GeoCoordinate mapStartPosition, String preferredLanguage) throws IOException {

		// get metadata for the map file
		int numberOfZoomIntervals = dataStore.getZoomIntervalConfiguration().getNumberOfZoomIntervals();

		LOGGER.fine("writing meta data");
		LOGGER.fine("Bounding box for file: " + dataStore.getBoundingBox().maxLatitudeE6 + ", "
				+ dataStore.getBoundingBox().minLongitudeE6 + ", " + dataStore.getBoundingBox().minLatitudeE6
				+ ", " + dataStore.getBoundingBox().maxLongitudeE6);

		ByteBuffer containerHeaderBuffer = ByteBuffer.allocate(HEADER_BUFFER_SIZE);

		// FILE VERSION
		this.mapFileMetaData.setFileVersion("" + version);

		// DATE OF CREATION
		this.mapFileMetaData.setDateOfCreation(date);

		// BOUNDING BOX
		this.mapFileMetaData.setBoundingBox(dataStore.getBoundingBox().minLatitudeE6,
				dataStore.getBoundingBox().minLongitudeE6,
				dataStore.getBoundingBox().maxLatitudeE6,
				dataStore.getBoundingBox().maxLatitudeE6);

		// TILE SIZE
		this.mapFileMetaData.setTileSize(tilePixel);

		// PROJECTION
		this.mapFileMetaData.setProjection(PROJECTION);

		// PREFERRED LANGUAGE
		// TODO leads to zero length string, but according to specification this is correct
		if (preferredLanguage == null) {
			this.mapFileMetaData.setLanguagePreference("");
		}
		else {
			this.mapFileMetaData.setLanguagePreference(preferredLanguage);
		}

		// FLAGS
		byte flags = 0;

		// MAP START POSITION
		if (mapStartPosition != null) {
			containerHeaderBuffer.putInt(mapStartPosition.getLatitudeE6());
			containerHeaderBuffer.putInt(mapStartPosition.getLongitudeE6());
			flags = (byte) (flags | 0x08);
		}

		// START ZOOM LEVEL
		// TODO Start zoom level

		// POI TAGS
		String[] poiMappings = new String[(short) MapFileWriterTask.TAG_MAPPING.optimizedPoiIds().size()];
		System.out.println("Number of POI tags: " + poiMappings.length);
		System.out.println("Size of key set: " + MapFileWriterTask.TAG_MAPPING.optimizedPoiIds().keySet().size());

		// retrieves tag ids in order of frequency, most frequent come first
		short i = 0;
		for (short tagId : MapFileWriterTask.TAG_MAPPING.optimizedPoiIds().keySet()) {
			OSMTag tag = MapFileWriterTask.TAG_MAPPING.getPoiTag(tagId);
			// poiMappings[tagId] = tag.tagKey();
			poiMappings[i++] = tag.tagKey();
		}
		this.mapFileMetaData.setPOIMappings(poiMappings);

		// WAY TAGS
		containerHeaderBuffer.putShort((short) MapFileWriterTask.TAG_MAPPING.optimizedWayIds().size());
		String[] wayMappings = new String[(short) MapFileWriterTask.TAG_MAPPING.optimizedWayIds().size()];
		i = 0;
		for (short tagId : MapFileWriterTask.TAG_MAPPING.optimizedWayIds().keySet()) {
			OSMTag tag = MapFileWriterTask.TAG_MAPPING.getWayTag(tagId);
			// wayMappings[tagId] = tag.tagKey();
			wayMappings[i++] = tag.tagKey();
		}
		this.mapFileMetaData.setWayTagMappings(wayMappings);

		// AMOUNT OF ZOOM INTERVALS
		this.mapFileMetaData.setAmountOfZoomIntervals((byte) numberOfZoomIntervals);

		// ZOOM INTERVAL CONFIGURATION: SKIP COMPUTED AMOUNT OF BYTES
		this.mapFileMetaData.prepareZoomIntervalConfiguration();
		// TODO Write zoom interval configuration to db

		// COMMENT
		if (comment != null && !comment.equals("")) {
			this.mapFileMetaData.setComment(comment);
		} else {
			this.mapFileMetaData.setComment("i <3 mapsforge");
		}

		return containerHeaderBuffer.position();
	}

	// TODO find new method name
	private void writeSubfileMetaDataToContainerHeader(int i) {
		// HEADER META DATA FOR SUB FILE
		// write zoom interval configuration to header
		byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(i);
		byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(i);
		byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(i);

		this.mapFileMetaData.setZoomIntervalConfiguration(i, baseZoomCurrentInterval, minZoomCurrentInterval,
				maxZoomCurrentInterval, TileDataContainer.TILE_TYPE_VECTOR);
	}

	// TODO Change return type to void
	private long writeSubfile(final int zoomIntervalIndex,
			final boolean debugStrings, // final boolean waynodeCompression,
			final boolean polygonClipping, final boolean wayClipping, final boolean pixelCompression) {

		LOGGER.fine("writing data for zoom interval " + zoomIntervalIndex + ", number of tiles: "
				+ dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal()
				* dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical());

		TileCoordinate upperLeft = dataStore.getTileGridLayout(zoomIntervalIndex).getUpperLeft();
		int lengthX = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesHorizontal();
		int lengthY = dataStore.getTileGridLayout(zoomIntervalIndex).getAmountTilesVertical();

		final byte minZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMinZoom(
				zoomIntervalIndex);
		final byte maxZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getMaxZoom(
				zoomIntervalIndex);
		final byte baseZoomCurrentInterval = dataStore.getZoomIntervalConfiguration().getBaseZoom(
				zoomIntervalIndex);
		// byte maxMaxZoomlevel = dataStore.getZoomIntervalConfiguration().getMaxMaxZoom();

		int tileAmountInBytes = lengthX * lengthY * BYTE_AMOUNT_SUBFILE_INDEX_PER_TILE;
		int indexBufferSize = tileAmountInBytes
				+ (debugStrings ? DEBUG_INDEX_START_STRING.getBytes().length : 0);

		// Buffer for all tile data
		// TODO Make re-usable
		ByteBuffer tileBuffer = ByteBuffer.allocate(TILE_BUFFER_SIZE);
		ByteBuffer poiBuffer = ByteBuffer.allocate(POI_BUFFER_SIZE);
		ByteBuffer wayBuffer = ByteBuffer.allocate(WAY_BUFFER_SIZE);

		// loop over tiles (row-wise)
		// TODO make constant
		final int batchSize = 1000;
		int processed = 0;
		TileDataContainer tileDataContainer = null;

		// Batch of binary tiles
		Vector<TileDataContainer> tiles = new Vector<TileDataContainer>(batchSize);
		// Amount of bytes written to tile buffer
		for (int tileY = upperLeft.getY(); tileY < upperLeft.getY() + lengthY; tileY++) {
			for (int tileX = upperLeft.getX(); tileX < upperLeft.getX() + lengthX; tileX++) {
				// System.out.println("writing data for tile (" + tileX + ", " + tileY + ")");
				tileBuffer.clear();
				// ***************** TILE INDEX SEGMENT ********************

				TileCoordinate currentTileCoordinate = new TileCoordinate(tileX, tileY, baseZoomCurrentInterval);
				int currentTileLat = GeoCoordinate.doubleToInt(MercatorProjection.tileYToLatitude(
						currentTileCoordinate.getY(), currentTileCoordinate.getZoomlevel()));
				int currentTileLon = GeoCoordinate.doubleToInt(MercatorProjection.tileXToLongitude(
						currentTileCoordinate.getX(), currentTileCoordinate.getZoomlevel()));

				// TODO Handling of water tiles
				// if (TILE_INFO.isWaterTile(currentTileCoordinate)) {
				// indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				// } else {
				// // the TileInfo class may produce false negatives for tiles on zoom level
				// // greater than TileInfo.TILE_INFO_ZOOMLEVEL
				// // we need to run the coastline algorithm to detect whether the tile is
				// // completely covered by water or not
				// if (currentTileCoordinate.getZoomlevel() > TileInfo.TILE_INFO_ZOOMLEVEL) {
				// if (COASTLINE_HANDLER.isWaterTile(currentTileCoordinate,
				// dataStore.getCoastLines(currentTileCoordinate))) {
				// indexBytes[0] |= BITMAP_INDEX_ENTRY_WATER;
				// }
				// }
				// }

				// ***************** TILE DATA SEGMENT ********************

				// get data for tile
				TileData currentTile = dataStore.getTile(zoomIntervalIndex, tileX, tileY);

				// ***************** POIs ********************
				// write amount of POIs and ways for each zoom level
				Map<Byte, List<TDNode>> poisByZoomlevel = currentTile.poisByZoomlevel(minZoomCurrentInterval,
						maxZoomCurrentInterval);
				Map<Byte, List<TDWay>> waysByZoomlevel = currentTile.waysByZoomlevel(minZoomCurrentInterval,
						maxZoomCurrentInterval);

				// If tile is not empty
				if (poisByZoomlevel.size() > 0 || waysByZoomlevel.size() > 0) {

					// Tile signature
					if (debugStrings) {
						// write tile header
						StringBuilder sb = new StringBuilder();
						sb.append(DEBUG_STRING_TILE_HEAD).append(tileX).append(",").append(tileY)
								.append(DEBUG_STRING_TILE_TAIL);
						tileBuffer.put(sb.toString().getBytes());
						// append withespaces so that block has 32 bytes
					}

					// Zoom table
					int entitiesPerZoomLevelTablePosition = tileBuffer.position();
					short[][] entitiesPerZoomLevel = new short[maxZoomCurrentInterval -
							minZoomCurrentInterval
							+ 1][2];

					// clear poi buffer
					poiBuffer.clear();

					// write POIs for each zoom level beginning with lowest zoom level
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;
						List<TDNode> pois = poisByZoomlevel.get(Byte.valueOf(zoomlevel));
						if (pois == null)
							continue;
						for (TDNode poi : pois) {
							if (debugStrings) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_POI_HEAD).append(poi.getId())
										.append(DEBUG_STRING_POI_TAIL);
								poiBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes().length, poiBuffer);
							}

							// write poi features to the file
							poiBuffer.put(Serializer.getVariableByteSigned(poi.getLatitude() -
									currentTileLat));
							poiBuffer
									.put(Serializer.getVariableByteSigned(poi.getLongitude() - currentTileLon));

							// write byte with layer and tag amount info
							poiBuffer.put(infoBytePoiLayerAndTagAmount(poi));

							// write tag ids to the file
							if (poi.getTags() != null) {
								for (short tagID : poi.getTags()) {
									poiBuffer.put(Serializer
											.getVariableByteUnsigned(MapFileWriterTask.TAG_MAPPING
													.optimizedPoiIds().get(Short.valueOf(tagID)).intValue()));
								}
							}

							// write byte with bits set to 1 if the poi has a name, an elevation
							// or a housenumber
							poiBuffer.put(infoBytePOI(poi.getName(), poi.getElevation(),
									poi.getHouseNumber()));

							if (poi.getName() != null && poi.getName().length() > 0) {
								writeUTF8(poi.getName(), poiBuffer);

							}
							if (poi.getElevation() != 0) {
								poiBuffer.put(Serializer.getVariableByteSigned(poi.getElevation()));
							}
							if (poi.getHouseNumber() != null && poi.getHouseNumber().length() > 0) {
								writeUTF8(poi.getHouseNumber(), poiBuffer);
							}

							// increment count of POIs on this zoom level
							entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][0]++;
						}
					} // end for loop over POIs

					// ***************** WAYS ********************
					wayBuffer.put("::waystart::".getBytes());
					// loop over all relevant zoom levels
					for (byte zoomlevel = minZoomCurrentInterval; zoomlevel <= maxZoomCurrentInterval; zoomlevel++) {
						int indexEntitiesPerZoomLevelTable = zoomlevel - minZoomCurrentInterval;
						List<TDWay> ways = waysByZoomlevel.get(Byte.valueOf(zoomlevel));
						if (ways == null)
							continue;

						for (TDWay way : ways) {
							wayBuffer.clear();

							WayPreprocessingResult wpr = preprocessWay(way, pixelCompression,
									polygonClipping,
									wayClipping, currentTileCoordinate);

							if (wpr == null) {
								// exclude this way
								continue;
							}

							if (debugStrings) {
								StringBuilder sb = new StringBuilder();
								sb.append(DEBUG_STRING_WAY_HEAD).append(way.getId())
										.append(DEBUG_STRING_WAY_TAIL);
								tileBuffer.put(sb.toString().getBytes());
								// append withespaces so that block has 32 bytes
								appendWhitespace(DEBUG_BLOCK_SIZE - sb.toString().getBytes().length, tileBuffer);
							}

							// write subtile bitmask of way
							wayBuffer.putShort(wpr.getSubtileMask());

							// write byte with layer and tag amount
							wayBuffer.put(infoByteWayLayerAndTagAmount(way));

							// write tag ids
							if (way.getTags() != null) {
								for (short tagID : way.getTags()) {
									wayBuffer.put(Serializer.getVariableByteUnsigned(mappedWayTagID(tagID)));
								}
							}

							// write a byte with flags for existence of name, ref and label position
							wayBuffer.put(infoByteWayFeatures(way, wpr.getLabelPosition() != null));

							// // if the way has a name, write it to the file
							if (way.getName() != null && !way.getName().isEmpty()) {
								writeUTF8(way.getName(), wayBuffer);
							}

							// if the way has a ref, write it to the file
							if (way.getRef() != null && !way.getRef().isEmpty()) {
								writeUTF8(way.getRef(), wayBuffer);
							}

							if (wpr.getLabelPosition() != null) {
								int firstWayStartLat = wpr.getWayDataBlocks().get(0).getOuterWay().get(0)
										.intValue();
								int firstWayStartLon = wpr.getWayDataBlocks().get(0).getOuterWay().get(1)
										.intValue();

								wayBuffer.put(Serializer.getVariableByteSigned(wpr.getLabelPosition()
										.getLatitudeE6() - firstWayStartLat));
								wayBuffer.put(Serializer.getVariableByteSigned(wpr.getLabelPosition()
										.getLongitudeE6() - firstWayStartLon));
							}

							// write the amount of way data blocks
							wayBuffer.put((byte) wpr.getWayDataBlocks().size());

							// write the way data blocks

							// case 1: simple way or simple polygon --> the way block consists of
							// exactly one way
							// case 2: multi polygon --> the way consists of exactly one outer way and
							// one or more inner ways
							for (WayDataBlock wayDataBlock : wpr.getWayDataBlocks()) {

								// write the amount of coordinate blocks
								// we have at least one block (potentially interpreted as outer way) and
								// possible blocks for inner ways
								if (wayDataBlock.getInnerWays() != null
										&& !wayDataBlock.getInnerWays().isEmpty())
									// multi polygon: outer way + number of inner ways
									wayBuffer.put((byte) (1 + wayDataBlock.getInnerWays().size()));
								else
									// simply a single way (not a multi polygon)
									wayBuffer.put((byte) 1);

								// write block for (outer/simple) way
								writeWay(wayDataBlock.getOuterWay(), currentTileLat, currentTileLon, wayBuffer);

								// write blocks for inner ways
								if (wayDataBlock.getInnerWays() != null
										&& !wayDataBlock.getInnerWays().isEmpty())
									for (List<Integer> innerWayCoordinates : wayDataBlock.getInnerWays())
										writeWay(innerWayCoordinates, currentTileLat, currentTileLon, wayBuffer);

							}

							// write size of way to tile buffer
							tileBuffer.put(Serializer.getVariableByteUnsigned(wayBuffer.position()));
							// write way data to tile buffer
							tileBuffer.put(wayBuffer.array(), 0, wayBuffer.position());

							// increment count of ways on this zoom level
							entitiesPerZoomLevel[indexEntitiesPerZoomLevelTable][1]++;
						}
					} // end for loop over ways
					wayBuffer.put("::wayend::".getBytes());

					// ZOOM TABLE
					tileBuffer.put(":::TODO: Zoom Table:::".getBytes());

					// FIRST WAY OFFSET
					tileBuffer.put(Serializer.getVariableByteUnsigned(tileBuffer.position()));

					// POI DATA
					tileBuffer.put(poiBuffer.array(), 0, poiBuffer.position());

					// WAY DATA
					tileBuffer.put(wayBuffer.array(), 0, wayBuffer.position());

					// int tileSize = tileBuffer.position();

					// update the zoom level table
					// write cumulated number of POIs and ways for this tile on each zoom level
					// short[] cumulatedCounts = new short[2];
					// for (short[] entityCount : entitiesPerZoomLevel) {
					// cumulatedCounts[0] += entityCount[0];
					// cumulatedCounts[1] += entityCount[1];
					// tileBuffer.putShort(cumulatedCounts[0]);
					// tileBuffer.putShort(cumulatedCounts[1]);
					// }
					//
					// if (maxWaysPerTile < cumulatedCounts[1])
					// maxWaysPerTile = cumulatedCounts[1];
					// cumulatedNumberOfWaysInTiles += cumulatedCounts[1];
					//
					// tileBuffer.position(tileSize);

					// currentSubfileOffset += tileBuffer.position();
					//
					// // accounting
					// if (maxTileSize < tileSize)
					// maxTileSize = tileSize;
					// if (tileSize > 0)
					// cumulatedTileSizeOfNonEmptyTiles += tileSize;
					//
					// // add tile to tiles buffer
					// compressedTilesBuffer.put(tileBuffer.array(), 0, tileSize);
					//
					// // if necessary, allocate new buffer
					// if (compressedTilesBuffer.remaining() < MIN_TILE_BUFFER_SIZE) {
					// // randomAccessFile.write(compressedTilesBuffer.array(), 0,
					// // compressedTilesBuffer.position());
					// compressedTilesBuffer.clear();
					// }

					// COMMIT BATCH
					byte[] tile = new byte[tileBuffer.position()];
					tileBuffer.rewind();
					tileBuffer.get(tile);
					tiles.add(new TileDataContainer(tile, TileDataContainer.TILE_TYPE_VECTOR, tileY, tileY,
							(byte) zoomIntervalIndex));
					++processed;
					if (processed == batchSize) {
						this.tilePersistenceManager.insertOrUpdateTiles(tiles);
						tiles.clear();
						processed = 0;
					}
				} // end if clause checking if tile is empty or not
				else {
					emptyTiles++;
				}
				// ###################################

				tilesProcessed++;
				if (tilesProcessed % amountOfTilesInPercentStep == 0) {
					LOGGER.info("written " + (tilesProcessed / amountOfTilesInPercentStep)
							* PROGRESS_PERCENT_STEP + "% of file");
				}

			} // end for loop over tile columns
		} // /end for loop over tile rows

		// write remaining tiles
		this.tilePersistenceManager.insertOrUpdateTiles(tiles);

		// return size of sub file in bytes
		return 0;
	}// end writeSubfile()

	private static void writeWay(List<Integer> wayNodes, int currentTileLat, int currentTileLon,
			ByteBuffer buffer) {
		// write the amount of way nodes to the file
		// wayBuffer
		buffer.put(Serializer.getVariableByteUnsigned(wayNodes.size() / 2));

		// write the way nodes:
		// the first node is always stored with four bytes
		// the remaining way node differences are stored according to the
		// compression type
		writeWayNodes(wayNodes, currentTileLat, currentTileLon, buffer);
	}

	private static void writeWayNodes(List<Integer> waynodes, int currentTileLat, int currentTileLon,
			ByteBuffer buffer) {
		if (!waynodes.isEmpty() && waynodes.size() % 2 == 0) {
			Iterator<Integer> waynodeIterator = waynodes.iterator();
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLat));
			buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue() - currentTileLon));

			while (waynodeIterator.hasNext()) {
				buffer.put(Serializer.getVariableByteSigned(waynodeIterator.next().intValue()));
			}
		}
	}

	private static void appendWhitespace(int amount, ByteBuffer buffer) {
		for (int i = 0; i < amount; i++) {
			buffer.put((byte) ' ');
		}
	}

	private WayPreprocessingResult preprocessWay(TDWay way, boolean simplify, boolean clipPolygons,
			boolean clipWays, TileCoordinate tile) {

		// TODO more sophisticated clipping of polygons needed
		// we have a problem when clipping polygons which border needs to be rendered
		// the problem does not occur with polygons that do not have a border
		// imagine an administrive border, such a polygon is not filled, but its border is rendered
		// in case the polygon spans multiple base zoom tiles, clipping introduces connections between
		// nodes that haven't existed before (exactly at the borders of a base tile)
		// in case of filled polygons we do not care about these connections
		// polygons that represent a border must be clipped as simple ways and not as polygons

		List<TDWay> innerways = dataStore.getInnerWaysOfMultipolygon(way.getId());
		// wayDataBlockList = GeoUtils.preprocessWay(way, null, clipPolygons, simplify, clipWays, tile,
		// bboxEnlargement);
		Geometry geometry = GeoUtils.preprocessWay(way, innerways, clipPolygons, clipWays, simplify, tile,
				bboxEnlargement);
		List<WayDataBlock> blocks = GeoUtils.toWayDataBlockList(geometry);
		if (blocks == null) {
			return null;
		}
		if (blocks.isEmpty()) {
			LOGGER.finer("empty list of way data blocks after preprocessing way: " + way.getId());
			return null;
		}
		short subtileMask = GeoUtils.computeBitmask(geometry, tile, bboxEnlargement);

		// check if the polygon is completely contained in the current tile
		// in that case clipped polygon equals the original polygon
		// as a consequence we do not try to compute a label position
		// this is left to the renderer for more flexibility
		GeoCoordinate centroid = null;
		if (GeoUtils.coveredByTile(geometry, tile, bboxEnlargement)) {
			centroid = GeoUtils.computeCentroid(geometry);
		}

		// TODO chose between delta or double delta encoding
		blocks = DeltaEncoder.encode(blocks);

		return new WayPreprocessingResult(blocks, centroid, subtileMask);
	}

	private static int mappedWayTagID(short original) {
		return MapFileWriterTask.TAG_MAPPING.optimizedWayIds().get(Short.valueOf(original)).intValue();
	}

	private static byte infoBytePoiLayerAndTagAmount(TDNode node) {
		byte layer = node.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = node.getTags() == null ? 0 : (short) node.getTags().length;

		return (byte) (layer << 4 | tagAmount);
	}

	private static byte infoByteWayLayerAndTagAmount(TDWay way) {
		byte layer = way.getLayer();
		// make sure layer is in [0,10]
		layer = layer < 0 ? 0 : layer > 10 ? 10 : layer;
		short tagAmount = way.getTags() == null ? 0 : (short) way.getTags().length;

		return (byte) (layer << 4 | tagAmount);
	}

	private static byte infoByteOptmizationParams(boolean debug, boolean mapStartPosition) {
		byte infoByte = 0;

		if (debug)
			infoByte |= BITMAP_DEBUG;
		if (mapStartPosition)
			infoByte |= BITMAP_MAP_START_POSITION;

		return infoByte;
	}

	private static byte infoBytePOI(String name, int elevation, String housenumber) {
		byte infoByte = 0;

		if (name != null && name.length() > 0) {
			infoByte |= BITMAP_NAME;
		}
		if (elevation != 0) {
			infoByte |= BITMAP_ELEVATION;
		}
		if (housenumber != null && housenumber.length() > 0) {
			infoByte |= BITMAP_HOUSENUMBER;
		}
		return infoByte;
	}

	private static byte infoByteWayFeatures(TDWay way, boolean hasLabelPosition) {
		byte infoByte = 0;

		if (way.getName() != null && !way.getName().isEmpty()) {
			infoByte |= BITMAP_NAME;
		}
		if (way.getRef() != null && !way.getRef().isEmpty()) {
			infoByte |= BITMAP_REF;
		}
		if (hasLabelPosition) {
			infoByte |= BITMAP_LABEL;
		}

		return infoByte;
	}

	private class WayPreprocessingResult {

		final List<WayDataBlock> wayDataBlocks;
		final GeoCoordinate labelPosition;
		final short subtileMask;

		WayPreprocessingResult(List<WayDataBlock> wayDataBlocks, GeoCoordinate labelPosition, short subtileMask) {
			super();
			this.wayDataBlocks = wayDataBlocks;
			this.labelPosition = labelPosition;
			this.subtileMask = subtileMask;
		}

		public List<WayDataBlock> getWayDataBlocks() {
			return wayDataBlocks;
		}

		public GeoCoordinate getLabelPosition() {
			return labelPosition;
		}

		public short getSubtileMask() {
			return subtileMask;
		}

	}

}
