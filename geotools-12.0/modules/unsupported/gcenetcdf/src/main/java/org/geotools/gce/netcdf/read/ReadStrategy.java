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

import java.io.IOException;
import java.util.Map;

import org.geotools.gce.netcdf.GrdDataEncapsulator;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;

/**
 * Strategy pattern for reading values from NetCDF files.
 * 
 * @author Yancy Matherne <yancy.matherne@geocent.com>
 */
public interface ReadStrategy {

    /**
     * Reads the desired grid from the NetCDF Variable into the GrdDataEncapsulator.
     * 
     * @param longitudeIndices - Map where the keys are request indices and the values are file indices.
     * @param latitudeIndices - Map where the keys are request indices and the values are file indices.
     * @param data - Object to store the image data to return in the response
     * @throws IOException
     * @throws InvalidRangeException
     */
    void read(Map<Integer, Integer> longitudeIndices, Map<Integer, Integer> latitudeIndices,
            GrdDataEncapsulator data) throws IOException, InvalidRangeException;

    /**
     * Set the NetCDF Variable to read.
     * 
     * @param variable
     */
    void setVariable(Variable variable);

    /**
     * Set the index for which Elevation value to use in the read.
     * 
     * @param index
     */
    void setElevationIndex(int index);

    void setElevationVariableNameInFile(String elevationVariableNameInFile);

    /**
     * Set the index for which Time value to use in the read.
     * 
     * @param index
     */
    void setTimeIndex(int index);

    void setTimeVariableNameInFile(String timeVariableNameInFile);

    /**
     * Set the index for which Runtime value to use in the read.
     * 
     * @param index
     */
    void setRuntimeIndex(int index);

    void setRuntimeVariableNameInFile(String runtimeVariableNameInFile);

    /**
     * Get the Elevation dimension index for this NetCDF Variable.
     * 
     * @param variable
     * @return int
     */
    int getElevationDimensionIndex();

    /**
     * Get the Time dimension index for this NetCDF Variable.
     * 
     * @param variable
     * @return int
     */
    int getTimeDimensionIndex();

    /**
     * Get the Runtime dimension index for this NetCDF Variable.
     * 
     * @param variable
     * @return int
     */
    int getRuntimeDimensionIndex();

    /**
     * Get the Longitude dimension index for this NetCDF Variable.
     * 
     * @param variable
     * @return int
     */
    int getLongitudeDimensionIndex();

    /**
     * Get the Latitude dimension index for this NetCDF Variable.
     * 
     * @param variable
     * @return int
     */
    int getLatitudeDimensionIndex();
}
