/*
 * Copyright (C) 2014-2015 CS-SI (foss-contact@thor.si.c-s.fr)
 * Copyright (C) 2014-2015 CS-Romania (office@c-s.ro)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.s2tbx.dataio.worldview2esa.internal;

import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelModel;
import com.bc.ceres.glevel.support.DefaultMultiLevelSource;
import org.esa.snap.core.datamodel.Band;

import javax.media.jai.*;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MosaicDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single banded multi-level mosaic image source.
 *
 * @author Denisa Stefanescu
 */
public class MosaicMultiLevelSource extends AbstractMultiLevelSource {

    private final Band[][] sourceBands;
    private final int imageWidth;
    private final int imageHeight;
    private final int tileWidth;
    private final int tileHeight;
    private final int numXTiles;
    private final int numYTiles;
    private final Logger logger;

    public MosaicMultiLevelSource(Band[][] sourceBands, int imageWidth, int imageHeight,
                                  int tileWidth, int tileHeight, int numTilesX, int numTilesY, int levels,
                                  AffineTransform transform) {
        super(new DefaultMultiLevelModel(levels,
                                         transform,
                                         imageWidth, imageHeight));
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.numXTiles = numTilesX;
        this.numYTiles = numTilesY;
        this.sourceBands = sourceBands;
        logger = Logger.getLogger(MosaicMultiLevelSource.class.getName());
    }

    /**
     * Creates a planar image corresponding of a tile identified by row and column, at the specified resolution.
     *
     * @param row   The row of the tile (0-based)
     * @param col   The column of the tile (0-based)
     * @param level The resolution level (0 = highest)
     */
    protected PlanarImage createTileImage(final int row, final int col, final int level) throws IOException {
        return (PlanarImage) sourceBands[row][col].getSourceImage().getImage(level);
    }

    @Override
    protected RenderedImage createImage(int level) {
        final List<RenderedImage> tileImages = Collections.synchronizedList(new ArrayList<>(numXTiles * numYTiles));
        final double factorX = 1.0 / Math.pow(2, level);
        final double factorY = 1.0 / Math.pow(2, level);
        for (int x = 0; x < numXTiles; x++) {
            for (int y = 0; y < numYTiles; y++) {
                PlanarImage opImage;
                try {
                    opImage = createTileImage(x, y, level);
                    if (opImage != null) {
                        opImage = TranslateDescriptor.create(opImage,
                                                             (float) (x * tileWidth * factorX),
                                                             (float) (y * tileHeight * factorY),
                                                             Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                             null);
                    }
                } catch (IOException ex) {
                    opImage = ConstantDescriptor.create((float) tileWidth, (float) tileHeight, new Number[]{0}, null);
                }
                tileImages.add(opImage);
            }
        }
        if (tileImages.isEmpty()) {
            logger.warning("No tile images for mosaic");
            return null;
        }

        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setMinX(0);
        imageLayout.setMinY(0);
        imageLayout.setTileWidth(JAI.getDefaultTileSize().width);
        imageLayout.setTileHeight(JAI.getDefaultTileSize().height);
        imageLayout.setTileGridXOffset(0);
        imageLayout.setTileGridYOffset(0);

        RenderedOp mosaicOp = MosaicDescriptor.create(tileImages.toArray(new RenderedImage[tileImages.size()]),
                                                      MosaicDescriptor.MOSAIC_TYPE_OVERLAY,
                                                      null, null, null, null,
                                                      new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout));
        int fittingRectWidth = scaleValue(imageWidth, level);
        int fittingRectHeight = scaleValue(imageHeight, level);

        Rectangle fitRect = new Rectangle(0, 0, fittingRectWidth, fittingRectHeight);
        final Rectangle destBounds = DefaultMultiLevelSource.getLevelImageBounds(fitRect, Math.pow(2.0, level));

        BorderExtender borderExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

        if (mosaicOp.getWidth() < destBounds.width || mosaicOp.getHeight() < destBounds.height) {
            int rightPad = destBounds.width - mosaicOp.getWidth();
            int bottomPad = destBounds.height - mosaicOp.getHeight();
            mosaicOp = BorderDescriptor.create(mosaicOp, 0, rightPad, 0, bottomPad, borderExtender, null);
        }

        return mosaicOp;
    }

    @Override
    public synchronized void reset() {
        super.reset();
        System.gc();
    }

    private int scaleValue(final int source, final int level) {
        int size = source >> level;
        int sizeTest = size << level;
        if (sizeTest < source) {
            size++;
        }
        return size;
    }
}
