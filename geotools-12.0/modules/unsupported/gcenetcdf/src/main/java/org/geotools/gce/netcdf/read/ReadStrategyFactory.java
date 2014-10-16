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
package org.geotools.gce.netcdf.read;

import java.util.Collections;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;
import ucar.ma2.Array;

/**
 * Provides a static getReadStrategy method for getting the most appropriate ReadStrategy for the situation.
 * 
 * @author Yancy Matherne <yancy.matherne@geocent.com>
 */
public class ReadStrategyFactory {

    private static final Logger LOG = Logging.getLogger(ReadStrategyFactory.class);

    /**
     * Threshold that determines at which point to switch from the UnderSamplingReadStrategy to the OverSamplingReadStrategy when using sampling
     * factor.
     */
    public static final double SAMPLING_FACTOR_THRESHOLD = .7;

    /**
     * Threshold that determines at which point to switch from the UnderSamplingReadStrategy to the OverSamplingReadStrategy when using the file's
     * grid size.
     */
    public static final long FILE_GRID_SIZE_THRESHOLD = 200000;

    /**
     * Determines the sampling factor for the current request.
     * 
     * Computed as the resolution of the request divided by the resolution of the file for the specific bounding boxed region.
     * 
     * @return double
     */
    public static double getSamplingFactor(int numberOfRequestPoints, int numberOfFilePoints) {
        return (double) numberOfRequestPoints / numberOfFilePoints;
    }

    /**
     * Determines which ReadStrategy to return based on a threshold function.
     * 
     * @param longitudeIndices - Map where the keys are request indices and the values are file indices.
     * @param latitudeIndices - Map where the keys are request indices and the values are file indices.
     * @param fileLongitudes - Array of all the longitude values in the file.
     * @param fileLatitudes - Array of all the latitude values in the file.
     * @return ReadStrategy
     */
    public static ReadStrategy getReadStrategy(Map<Integer, Integer> longitudeIndices,
            Map<Integer, Integer> latitudeIndices, Array fileLongitudes, Array fileLatitudes) {
        int numberOfRequestLongitudes = longitudeIndices.size();
        int numberOfRequestLatitudes = latitudeIndices.size();

        int numberOfFileLongitudes = Collections.max(longitudeIndices.values())
                - Collections.min(longitudeIndices.values());
        int numberOfFileLatitudes = Collections.max(latitudeIndices.values())
                - Collections.min(latitudeIndices.values());

        return getReadStrategy(numberOfRequestLongitudes, numberOfRequestLatitudes,
                numberOfFileLongitudes, numberOfFileLatitudes);
    }

    /*
     * Determines which ReadStrategy to return based on a threshold function.
     * 
     * @param numberOfRequestLongitudes - Width of the request image
     * 
     * @param numberOfRequestLatitudes - Height of the request image
     * 
     * @param numberOfFileLongitudes - Width of the file grid
     * 
     * @param numberOfFileLatitudes - Height of the file grid
     * 
     * @return ReadStrategy
     */
    private static ReadStrategy getReadStrategy(int numberOfRequestLongitudes,
            int numberOfRequestLatitudes, int numberOfFileLongitudes, int numberOfFileLatitudes) {

        if (LOG.isLoggable(Level.INFO)) {
            LOG.log(Level.INFO, "Request dimensions [{0},{1}]. File dimensions [{2},{3}].",
                    new Object[] { numberOfRequestLongitudes, numberOfRequestLatitudes,
                            numberOfFileLongitudes, numberOfFileLatitudes });
        }

        return getReadStrategy(numberOfRequestLongitudes * numberOfRequestLatitudes,
                numberOfFileLongitudes * numberOfFileLatitudes);
    }

    /*
     * Determines which ReadStrategy to return based on a threshold function.
     * 
     * @param numberOfRequestPoints - Area of the request image
     * 
     * @param numberOfFilePoints - Area of the file grid
     * 
     * @return ReadStrategy
     */
    private static ReadStrategy getReadStrategy(int numberOfRequestPoints, int numberOfFilePoints) {
        if (LOG.isLoggable(Level.INFO)) {
            double samplingFactor = getSamplingFactor(numberOfRequestPoints, numberOfFilePoints);

            LOG.log(Level.INFO, "Sampling Factor is {0} and the Threshold is {1}", new Object[] {
                    samplingFactor, SAMPLING_FACTOR_THRESHOLD });

            LOG.log(Level.INFO, "File Grid Size is {0} and the Threshold is {1}", new Object[] {
                    numberOfFilePoints, FILE_GRID_SIZE_THRESHOLD });
        }

        // This really needs to be engineered better. Need to find the right mix of memory usage
        // and request speed. The OverSampling strategy is (almost?) always faster as it only needs
        // to read the file once. But reading too many points at once causes an Out Of Memory
        // Exception. Reading all the points in a high resolution file for a low resolution request
        // is unnecessary since most of the data points won't be used anyway.

        // Was going to use sampling factor as the threshold, but for the NCML layers the sampling
        // factor can easily be low (just saw .165, maybe the threshold should be lower than that?).
        // And there is a serious speed degredation when using UnderSampingReadStrategy on NCML
        // layers. So I figured the real problem is just the shear number of points being requested.
        // We always want to be as fast as possible and only want to slow down when memory
        // consumption becomes catastrophic. So switching to an implementation where we decide which
        // strategy to use based on the number of points in the file for the bounding box requested.
        if (numberOfFilePoints > FILE_GRID_SIZE_THRESHOLD) {
            return new UnderSamplingReadStrategy();
        } else {
            return new OverSamplingReadStrategy();
        }
    }
}
