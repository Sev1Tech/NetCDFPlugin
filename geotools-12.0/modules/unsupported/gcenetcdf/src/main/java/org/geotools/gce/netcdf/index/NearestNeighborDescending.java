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
package org.geotools.gce.netcdf.index;

import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

import ucar.ma2.Array;

/**
 * A nearest neighbor implementation for the strategy for finding the index of the coordinate value in a descending coordinate array from a NetCDF
 * file. Computes a rough index value based on the resolution of the first two points, then brute force checks neighboring values to find the true
 * closest.
 * 
 * @author Yancy
 */
public class NearestNeighborDescending extends NearestNeighbor {

    private static final Logger LOG = Logging.getLogger(NearestNeighborDescending.class);

    /**
     * Find the index of the closest coordinate value in an array.
     * 
     * @param coordArray
     * @param desiredCoord
     * @return int
     */
    @Override
    public int getCoordinateIndex(Array coordArray, double desiredCoord) {
        if (coordArray.getRank() != 1) {
            LOG.severe("ERROR passed a non-1D array into a function that wasn't expecting it");
            return -1;
        }

        int minArrayIndex = (int) coordArray.getSize() - 1;
        int maxArrayIndex = 0;

        // Get the file's first coordinate
        double minArrayCoord = coordArray.getDouble(minArrayIndex);

        // Get the file's last coordinate
        double maxArrayCoord = coordArray.getDouble(maxArrayIndex);

        // Get the distance between the array's first two coordinates to use as
        // a grid resolution.
        double resolution = getAbsoluteDifference(coordArray.getDouble(0), coordArray.getDouble(1));
        // If the desired coordinate is less than the array's first
        // coordinate, and the coordinate array is always sorted so that the
        // values decrease, then the desired value cant be in the array.
        if (desiredCoord < minArrayCoord) {
            // We are assuming nearest neighbor here.
            // So check if the desired coordinate is within half the resolution
            // of the array's first coordinate (i.e., half the resolution on
            // either side belongs to the original grid point).
            if (getAbsoluteDifference(desiredCoord, minArrayCoord) < (resolution / 2)) {
                return minArrayIndex;
            } else {
                return -1;
            }
        }

        // If the desired coordinate is greater than the array's last
        // coordinate, and the coordinate array is always sorted so that the
        // values decrease, then the desired value cant be in the array.
        if (desiredCoord > maxArrayCoord) {
            // We are assuming nearest neighbor here.
            // So check if the desired coordinate is within half the resolution
            // of the array's last coordinate (i.e., half the resolution on
            // either side belongs to the original grid point).
            if (getAbsoluteDifference(desiredCoord, maxArrayCoord) <= (resolution / 2)) {
                return maxArrayIndex;
            } else {
                return -1;
            }
        }

        // The desired coordinate is somewhere after the array's first
        // coordinate. Using math, the array's first coordinate and
        // resolution, find the index of the desired coordinate.
        int index = (int) coordArray.getSize()
                - (int) Math.round((desiredCoord - minArrayCoord) / resolution);
        // Check for index out of bounds.
        if (index < 0) {
            // We already checked the desired coordinate's value against the
            // first coordinate value's in the array. So the value must be in
            // here. We must be off because of an inconsistent grid resolution
            // in the file.
            index = maxArrayIndex;
        } else if (index >= coordArray.getSize()) {
            // We already checked the desired coordinate's value against the
            // last coordinate value's in the array. So the value must be in
            // here. We must be off because of an inconsistent grid resolution
            // in the file.
            index = minArrayIndex;
        }

        // We have seen NetCDFs where the spacing isn't consistent throughout
        // the entire file. So, check the neighboring coordinates to make sure
        // we are at the true closest index.
        return checkNeighbors(coordArray, desiredCoord, index);
    }

}
