package org.esa.s2tbx.dataio.gdal.reader.plugins;

/**
 * Reader plugin for products using the GDAL library.
 *
 * @author Jean Coravu
 */
public class JP2OpenJPEGDriverProductReaderPlugIn extends AbstractDriverProductReaderPlugIn {

    public JP2OpenJPEGDriverProductReaderPlugIn() {
        super("JP2OpenJPEG", "JPEG-2000 driver based on OpenJPEG library");

        addExtensin(".jp2");
        addExtensin(".j2k");
    }
}
