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

package org.esa.s2tbx.dataio.spot6.dimap;

import com.bc.ceres.core.Assert;
import org.esa.s2tbx.dataio.metadata.GenericXmlMetadata;
import org.esa.s2tbx.dataio.metadata.XmlMetadataParser;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.utils.DateHelper;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a DIMAP volume metadata file, which points to individual components (products).
 * As of now, SPOT scene products have only one component.
 *
 * @author Cosmin Cara
 */
public class VolumeMetadata extends GenericXmlMetadata {

    final List<VolumeComponent> components;
    List<GenericXmlMetadata> componentMetadata;

    static class VolumeMetadataParser extends XmlMetadataParser<VolumeMetadata> {

        public VolumeMetadataParser(Class metadataFileClass) {
            super(metadataFileClass);
        }

        @Override
        protected ProductData inferType(String elementName, String value) {
            return ProductData.createInstance(value);
        }

        @Override
        protected boolean shouldValidateSchema() {
            return false;
        }
    }

    public static VolumeMetadata create(Path path) throws IOException {
        Assert.notNull(path);
        VolumeMetadata result = null;
        try (InputStream inputStream = Files.newInputStream(path)) {
            VolumeMetadataParser parser = new VolumeMetadataParser(VolumeMetadata.class);
            result = parser.parse(inputStream);
            result.setFileName(path.getFileName().toString());
            String[] titles = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TITLE);
            if (titles == null) {
                titles = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TITLE_ALT);
            }
            String[] types = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TYPE);
            if (types == null) {
                types = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TYPE_ALT);
            }
            String[] paths = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_PATH);
            if (paths == null) {
                paths = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_PATH_ALT);
            }
            String[] tnPaths = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TN_PATH);
            if (tnPaths == null) {
                tnPaths = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TN_PATH_ALT);
            }
            String[] tnFormats = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TN_FORMAT);
            if (tnFormats == null) {
                tnFormats = result.getAttributeValues(Spot6Constants.PATH_VOL_COMPONENT_TN_FORMAT_ALT);
            }
            if (titles != null && titles.length > 0) {
                for (int i = 0; i < titles.length; i++) {
                    VolumeComponent component = new VolumeComponent(path.getParent());
                    component.title = titles[i];
                    if (types != null && types.length == titles.length) {
                        component.type = types[i];
                    }
                    if (paths != null && paths.length == titles.length) {
                        try {
                            component.path = Paths.get(paths[i]);
                        } catch (InvalidPathException ignored) {
                            //component.path = Paths.get(URI.create(paths[i]));
                        }
                    }
                    if (tnPaths != null && tnPaths.length == titles.length) {
                        component.thumbnailPath = tnPaths[i];
                    }
                    if (tnFormats != null && tnFormats.length == titles.length) {
                        component.thumbnailFormat = tnFormats[i];
                    }
                    result.components.add(component);
                }
                result.componentMetadata = result.getNextLevelMetadata();
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return result;
    }

    public VolumeMetadata(String name) {
        super(name);
        components = new ArrayList<>();
        componentMetadata = new ArrayList<>();
    }

    @Override
    public String getFileName() {
        return this.name;
    }

    @Override
    public String getMetadataProfile() {
        return getAttributeValue(Spot6Constants.PATH_VOL_METADATA_PROFILE, Spot6Constants.METADATA_FORMAT);
    }

    public List<VolumeComponent> getComponents() {
        return components;
    }

    public List<VolumeComponent> getDimapComponents() {
        return this.components.stream().filter(current -> current.getType().equals(Spot6Constants.METADATA_FORMAT)).collect(Collectors.toList());
    }

    public List<VolumeMetadata> getVolumeMetadataList() {
        return componentMetadata.stream().filter(metadata -> metadata instanceof VolumeMetadata).map(metadata -> (VolumeMetadata) metadata).collect(Collectors.toList());
    }

    public List<ImageMetadata> getImageMetadataList() {
        return componentMetadata.stream().filter(metadata -> metadata instanceof ImageMetadata).map(metadata -> (ImageMetadata) metadata).collect(Collectors.toList());
    }

    public String getFormat() {
        return getAttributeValue(Spot6Constants.PATH_VOL_METADATA_FORMAT, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getFormatVersion() {
        return getAttributeValue(Spot6Constants.PATH_VOL_METADATA_FORMAT_VERSION, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getSubProfile() {
        return getAttributeValue(Spot6Constants.PATH_VOL_METADATA_SUBPROFILE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getLanguage() {
        return getAttributeValue(Spot6Constants.PATH_VOL_METADATA_LANGUAGE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getDatasetName() {
        return getAttributeValue(Spot6Constants.PATH_VOL_DATASET_NAME, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public Integer getDatasetId() {
        return Integer.parseInt(getAttributeValue(Spot6Constants.PATH_VOL_DATASET_ID, Spot6Constants.STRING_ONE));
    }

    public String getProducerName() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCER_NAME, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getProducerURL() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCER_URL, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getProducerContact() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCER_CONTACT, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getProducerAddress() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCER_ADDRESS, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getDistributorName() {
        return getAttributeValue(Spot6Constants.PATH_VOL_DISTRIBUTOR_NAME, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getDistributorURL() {
        return getAttributeValue(Spot6Constants.PATH_VOL_DISTRIBUTOR_URL, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getDistributorContact() {
        return getAttributeValue(Spot6Constants.PATH_VOL_DISTRIBUTOR_CONTACT, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getDistributorAddress() {
        return getAttributeValue(Spot6Constants.PATH_VOL_DISTRIBUTOR_ADDRESS, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public ProductData.UTC getProductionDate() {
        ProductData.UTC prodDate = null;
        String stringData = getAttributeValue(Spot6Constants.PATH_VOL_PRODUCTION_DATE, null);
        if (stringData != null && !stringData.isEmpty()) {
            prodDate = DateHelper.parseDate(stringData, Spot6Constants.SPOT6_UTC_DATE_FORMAT);
        }
        return prodDate;
    }

    public String getProductType() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCT_TYPE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getProductCode() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCT_CODE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getProductInfo() {
        return getAttributeValue(Spot6Constants.PATH_VOL_PRODUCT_INFO, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getJobId() {
        return getAttributeValue(Spot6Constants.PATH_VOL_JOB_ID, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getCustomerReference() {
        return getAttributeValue(Spot6Constants.PATH_VOL_CUSTOMER_REFERENCE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getInternalReference() {
        return getAttributeValue(Spot6Constants.PATH_VOL_INTERNAL_REFERENCE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getCommercialReference() {
        return getAttributeValue(Spot6Constants.PATH_VOL_COMMERCIAL_REFERENCE, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getCommercialItem() {
        return getAttributeValue(Spot6Constants.PATH_VOL_COMMERCIAL_ITEM, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    public String getComment() {
        return getAttributeValue(Spot6Constants.PATH_VOL_COMMENT, Spot6Constants.VALUE_NOT_AVAILABLE);
    }

    private List<GenericXmlMetadata> getNextLevelMetadata() {
        List<GenericXmlMetadata> subComponents = new ArrayList<>();
        List<VolumeComponent> dimapComponents = getDimapComponents();
        for (VolumeComponent component : dimapComponents) {
            GenericXmlMetadata componentMetadata = component.getComponentMetadata();
            if (componentMetadata instanceof VolumeMetadata) {
                subComponents.addAll(((VolumeMetadata)componentMetadata).getNextLevelMetadata());
            }
            subComponents.add(componentMetadata);
        }
        return subComponents;
    }
}
