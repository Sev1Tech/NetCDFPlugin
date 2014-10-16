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
 * A nearest neighbor implementation for the strategy for finding the index of the coordinate value in a coordinate array from a NetCDF file. Computes
 * a rough index value based on the resolution of the first two points, then brute force checks neighboring values to find the true closest.
 * 
 * @author Yancy
 */
public abstract class NearestNeighbor implements IndexingStrategy {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logging.getLogger(NearestNeighbor.class);

    protected int checkNeighbors(Array coordArray, double desiredCoord, int startingIndex) {
        // If we are at the beginning of the array, avoid index out of bounds.
        if (startingIndex == 0) {
            return findClosestIndexToTheRight(coordArray, desiredCoord, startingIndex);
        }
        // If we are at the end of the array, avoid index out of bounds.
        if (startingIndex == (coordArray.getSize() - 1)) {
            return findClosestIndexToTheLeft(coordArray, desiredCoord, startingIndex);
        }

        // If we are in the middle somewhere, check both sides.
        double prevCoord = coordArray.getDouble(startingIndex - 1);
        double currCoord = coordArray.getDouble(startingIndex);
        double nextCoord = coordArray.getDouble(startingIndex + 1);

        double prevRez = getAbsoluteDifference(desiredCoord, prevCoord);
        double currRez = getAbsoluteDifference(desiredCoord, currCoord);
        double nextRez = getAbsoluteDifference(desiredCoord, nextCoord);

        if (prevRez <= currRez && prevRez < nextRez) {
            return findClosestIndexToTheLeft(coordArray, desiredCoord, startingIndex - 1);
        } else if (nextRez < currRez && nextRez < prevRez) {
            return findClosestIndexToTheRight(coordArray, desiredCoord, startingIndex + 1);
        }

        return startingIndex;
    }

    protected int findClosestIndexToTheRight(Array coordArray, double desiredCoord,
            int startingIndex) {
        if (startingIndex == (coordArray.getSize() - 1)) {
            return startingIndex;
        }

        double currCoord = coordArray.getDouble(startingIndex);
        double nextCoord = coordArray.getDouble(startingIndex + 1);
        double currRez = getAbsoluteDifference(desiredCoord, currCoord);
        double nextRez = getAbsoluteDifference(desiredCoord, nextCoord);

        if (nextRez < currRez) {
            return findClosestIndexToTheRight(coordArray, desiredCoord, startingIndex + 1);
        } else {
            return startingIndex;
        }
    }

    protected int findClosestIndexToTheLeft(Array coordArray, double desiredCoord, int startingIndex) {
        if (startingIndex == 0) {
            return startingIndex;
        }

        double prevCoord = coordArray.getDouble(startingIndex - 1);
        double currCoord = coordArray.getDouble(startingIndex);
        double prevRez = getAbsoluteDifference(desiredCoord, prevCoord);
        double currRez = getAbsoluteDifference(desiredCoord, currCoord);

        if (prevRez <= currRez) {
            return findClosestIndexToTheLeft(coordArray, desiredCoord, startingIndex - 1);
        } else {
            return startingIndex;
        }
    }

    /*
     * Returns the absolute value of the difference of two numbers.
     */
    protected double getAbsoluteDifference(double a, double b) {
        return Math.abs(a - b);
    }

    public int getCoordinateIndex(Array coordArray, double desiredCoord) {
        // TODO Auto-generated method stub
        return 0;
    }

}
