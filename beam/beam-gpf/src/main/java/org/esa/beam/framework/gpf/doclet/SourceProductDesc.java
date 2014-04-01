/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.doclet;

import com.sun.javadoc.FieldDoc;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

import java.lang.reflect.Field;

public class SourceProductDesc extends FieldDesc{
    private final SourceProduct annotation;

    SourceProductDesc(Field field, FieldDoc fieldDoc, SourceProduct annotation) {
        super(field, fieldDoc);
        this.annotation = annotation;
    }

    public SourceProduct getAnnotation() {
        return annotation;
    }

    @Override
    public String getShortDescription() {
        return annotation.description();
    }
}