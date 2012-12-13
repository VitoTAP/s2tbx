package org.esa.beam.dataio.s3;

/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.glevel.MultiLevelModel;
import com.bc.ceres.glevel.support.AbstractMultiLevelSource;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;

import javax.media.jai.BorderExtender;
import javax.media.jai.BorderExtenderConstant;
import javax.media.jai.Interpolation;
import javax.media.jai.operator.BorderDescriptor;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.media.jai.operator.TranslateDescriptor;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

public class SourceImageScaler {

    public static MultiLevelImage scaleMultiLevelImage(MultiLevelImage masterImage, MultiLevelImage sourceImage,
                                                       float[] scalings, float[] scaleTranslations, float[] offsets,
                                                       RenderingHints renderingHints, double noDataValue) {

        final ScaledMultiLevelSource multiLevelSource = new ScaledMultiLevelSource(masterImage, sourceImage,
                                                                                   scalings, scaleTranslations,
                                                                                   offsets, renderingHints,
                                                                                   noDataValue);
        return new DefaultMultiLevelImage(multiLevelSource);
    }

    private static class ScaledMultiLevelSource extends AbstractMultiLevelSource {

        private final MultiLevelImage sourceImage;
        private final float[] scalings;
        private float[] scaleTranslations;
        private final RenderingHints renderingHints;
        private final double noDataValue;
        private final float[] offsets;
        private final MultiLevelImage masterImage;

        private ScaledMultiLevelSource(MultiLevelImage masterImage, MultiLevelImage sourceImage, float[] scalings,
                                       float[] scaleTranslations, float[] offsets, RenderingHints renderingHints,
                                       double noDataValue) {
            super(masterImage.getModel());
            this.masterImage = masterImage;
            this.sourceImage = sourceImage;
            this.scalings = scalings;
            this.scaleTranslations = scaleTranslations;
            this.renderingHints = renderingHints;
            this.noDataValue = noDataValue;
            this.offsets = offsets;
        }

        @Override
        protected RenderedImage createImage(int targetLevel) {
            final int masterWidth = masterImage.getImage(targetLevel).getWidth();
            final int masterHeight = masterImage.getImage(targetLevel).getHeight();
            final MultiLevelModel sourceModel = sourceImage.getModel();
            final MultiLevelModel targetModel = getModel();
            final double targetScale = targetModel.getScale(targetLevel);
            final int sourceLevel = sourceModel.getLevel(targetScale);
            final double sourceScale = sourceModel.getScale(sourceLevel);
            final RenderedImage image = sourceImage.getImage(sourceLevel);
            final float scaleRatio = (float) (sourceScale / targetScale);
            if (scaleTranslations == null) {
                scaleTranslations = new float[]{0f, 0f};
            }
            RenderedImage renderedImage = image;
            final float xScale = scalings[0] * scaleRatio;
            final float yScale = scalings[1] * scaleRatio;
            if (xScale != 1.0f && yScale != 1.0f) {
                renderedImage = ScaleDescriptor.create(image, xScale, yScale, scaleTranslations[0], scaleTranslations[1],
                                                       Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                       renderingHints);
            }
            final float scaledXOffset = (offsets != null) ? (float)(offsets[0] / targetScale) : 0f;
            final float scaledYOffset = (offsets != null) ? (float)(offsets[1] / targetScale) : 0f;
            if (!Double.isNaN(noDataValue)) {
                final int padX = Math.round(Math.abs(scaledXOffset));
                final int padY = Math.round(Math.abs(scaledYOffset));
                final BorderExtender borderExtender = new BorderExtenderConstant(new double[]{noDataValue});
                renderedImage = BorderDescriptor.create(renderedImage, padX, padX, padY, padY, borderExtender,
                                                        renderingHints);
            }
            if (scaledXOffset != 0.0f || scaledYOffset != 0.0f) {
                renderedImage = TranslateDescriptor.create(renderedImage, scaledXOffset, scaledYOffset, null,
                                                           renderingHints);
            }
            renderedImage = CropDescriptor.create(renderedImage, 0.0f, 0.0f, (float) masterWidth, (float) masterHeight,
                                                  renderingHints);
            return renderedImage;
        }

    }

}
