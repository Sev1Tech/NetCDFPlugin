/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.gce.netcdf;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.media.jai.DataBufferFloat;

import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.Envelope;

/**
 * Provides a place to assemble data and then generate a WritableRaster and Envelope based on that data.
 * 
 * Intended as a support object for the NetCDFReader, particularly to generate a GridCoverage2D object as the result of the NetCDFReader read method.
 */
public class GrdDataEncapsulator implements Serializable {

    private static final int N_BITS_32 = 32;

    private static final int _89 = 89;

    private static final int _179 = 179;

    private static final int MINUS_89 = -_89;

    private static final int MINUS_179 = -_179;

    private static final int MAX_LAT_90 = 90;

    private static final int MIN_LAT_MINUS_90 = -MAX_LAT_90;

    private static final int MAX_LON_180 = 180;

    private static final int MIN_LON_MINUS_180 = -MAX_LON_180;

    private static final long serialVersionUID = 5274683324298458872L;

    private Float[][] imageArray;

    private int imageWidth;

    private int imageHeight;

    private double gridLeftLon;

    private double gridRightLon;

    private double gridUpLat;

    private double gridLowLat;

    private List<Double> latList;

    private List<Double> lonList;

    public GrdDataEncapsulator(ParamInformation paramInput) {
        this.latList = new LinkedList<Double>();
        this.lonList = new LinkedList<Double>();

        /*
         * We need to create a buffer for the resulting image. The size of the image is dictated by the WMS request
         */
        imageWidth = Math.abs(paramInput.getDim().getSpan(0));
        imageHeight = Math.abs(paramInput.getDim().getSpan(1));
        imageArray = new Float[imageWidth][imageHeight];

        /*
         * We want to initialize the points to NaN, When these values are being populated, we will look at the "best" files first (the ones with the
         * latest initial time and the highest resolution, that way the "best" data will go into the data encapsulator first, if the NCParser detects
         * that a value has already been added in a specific position, it will skip that position
         */
        for (int i = 0; i < imageWidth; i++) {
            for (int j = 0; j < imageHeight; j++) {
                imageArray[i][j] = Float.NaN;
            }
        }

        /*
         * We need to store the bounding region that we're interested in for this particular request
         */
        Rectangle2D rec = paramInput.getRequestedEnvelope().toRectangle2D();
        gridLeftLon = rec.getMinX() < MIN_LON_MINUS_180 ? MIN_LON_MINUS_180 : rec.getMinX();
        gridRightLon = rec.getMaxX() > MAX_LON_180 ? MAX_LON_180 : rec.getMaxX();
        gridUpLat = rec.getMaxY() > MAX_LAT_90 ? MAX_LAT_90 : rec.getMaxY();
        gridLowLat = rec.getMinY() < MIN_LAT_MINUS_90 ? MIN_LAT_MINUS_90 : rec.getMinY();

//        if (Math.abs(gridLeftLon - gridRightLon) < 0.05) {
//            /*
//             * geoserver complains about envelopes being too small, we need to give a bigger envelope
//             */
//            if (gridLeftLon > MINUS_179) {
//                gridLeftLon = gridLeftLon + -1;
//            } else if (gridRightLon < _179) {
//                gridRightLon = gridRightLon - 1;
//            }
//        }
//        if (Math.abs(gridUpLat - gridLowLat) < 0.05) {
//            if (gridLowLat > MINUS_89) {
//                gridLowLat = gridLowLat + -1;
//            } else if (gridUpLat < _89) {
//                gridUpLat = gridUpLat + 1;
//            }
//        }

        double gridLonSpan = Math.abs((gridRightLon - gridLeftLon)) / imageWidth;
        double gridLatSpan = Math.abs(gridUpLat - gridLowLat) / imageHeight;

        // gridLatSpan / 2 added by Sam Foster
        // to handle GetFeautreInfo request offset problem
        for (int i = 0; i < imageHeight; i++) {
            latList.add(gridLowLat + (gridLatSpan * i) + (gridLatSpan / 2));
        }

        // gridLonSpan / 2 added by Sam Foster
        // to handle GetFeautreInfo request offset problem
        for (int i = 0; i < imageWidth; i++) {
            lonList.add(gridLeftLon + (gridLonSpan * i) + (gridLonSpan / 2));
        }
    }

    public List<Double> getDesiredLats() {
        return latList;
    }

    public List<Double> getDesiredLons() {
        return lonList;
    }

    /**
     * Provides an Envelope based on this class's grid properties.
     */
    public Envelope getGeneralEnvelope() {
        final Envelope returnedEnv = new Envelope2D(DefaultGeographicCRS.WGS84, gridLeftLon,
                gridLowLat, (gridRightLon - gridLeftLon), (gridUpLat - gridLowLat));
        return returnedEnv;
    }

    /**
     * Provides a WritableRaster based on this class's imageArray and imageWidth and imageHeight.
     */
    public WritableRaster getWritableRaster() {
        int imageBufferLen = imageWidth * imageHeight;
        float[] imageBuffer = new float[imageBufferLen];

        int k = 0;
        for (int height = 0; height < imageArray[0].length; height++) {
            for (int width = 0; width < imageArray.length; width++) {
                imageBuffer[k] = imageArray[width][height];
                k++;
            }
        }

        int[] nBits = { N_BITS_32 };
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorModel cm = new ComponentColorModel(cs, nBits, false, true, Transparency.OPAQUE,
                DataBuffer.TYPE_FLOAT);
        SampleModel sm = cm.createCompatibleSampleModel(imageWidth, imageHeight);
        WritableRaster raster = Raster.createWritableRaster(sm, new DataBufferFloat(imageBuffer,
                imageBuffer.length), new Point(0, 0));
        return raster;
    }

    public Float[][] getImageArray() {
        return imageArray;
    }

    public void setImageArray(Float[][] imageArray) {
        this.imageArray = imageArray.clone();
    }

    public double getGridLeftLon() {
        return gridLeftLon;
    }

    public void setGridLeftLon(double gridLeftLon) {
        this.gridLeftLon = gridLeftLon;
    }

    public double getGridRightLon() {
        return gridRightLon;
    }

    public void setGridRightLon(double gridRightLon) {
        this.gridRightLon = gridRightLon;
    }

    public double getGridUpLat() {
        return gridUpLat;
    }

    public void setGridUpLat(double gridUpLat) {
        this.gridUpLat = gridUpLat;
    }

    public double getGridLowLat() {
        return gridLowLat;
    }

    public void setGridLowLat(double gridLowLat) {
        this.gridLowLat = gridLowLat;
    }
}
