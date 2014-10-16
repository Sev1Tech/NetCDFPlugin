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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.gce.netcdf.GrdDataEncapsulator;
import org.geotools.gce.netcdf.NetCdfUtil;
import org.geotools.util.logging.Logging;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;

/**
 * An abstract ReadStrategy that provides some common instance variables and methods necessary for all ReadStrategies.
 * 
 * @author Yancy Matherne <yancy.matherne@geocent.com>
 */
public abstract class AbstractReadStrategy implements ReadStrategy {

    private static final Logger LOG = Logging.getLogger(AbstractReadStrategy.class);

    protected Variable variable;

    private float scaleFactor = 1.0f;

    private float addOffset = 0.0f;

    private float missingValue = Float.NaN;

    private float fillValue = Float.NaN;

    private int elevationIndex = -1;

    private String elevationVariableNameInFile = null;

    private int timeIndex = -1;

    private String timeVariableNameInFile = null;

    private int runtimeIndex = -1;

    private String runtimeVariableNameInFile = null;

    /**
     * Create a String that will be used as the parameter to the Variable.read() method. The String is a comma delimited list where each element
     * represents a Dimension on the Variable. Each Dimension element can be a single index or a colon-separated range of indices.
     * 
     * @param longitudeParameter
     * @param latitudeParameter
     * @return String
     */
    protected String getReadParameter(String longitudeParameter, String latitudeParameter) {
        StringBuilder readParameter = new StringBuilder();

        // Get the dimensions for this Variable.
        List<Dimension> dimensions = variable.getDimensions();

        // Loop through the dimensions used by this variable and create the parameter String in
        // the correct order.
        for (int dimensionIndex = 0; dimensionIndex < dimensions.size(); dimensionIndex++) {
            if (dimensionIndex == getLongitudeDimensionIndex()) {
                readParameter.append(longitudeParameter);
            } else if (dimensionIndex == getLatitudeDimensionIndex()) {
                readParameter.append(latitudeParameter);
            } else if (dimensionIndex == getTimeDimensionIndex()) {
                readParameter.append(timeIndex);
            } else if (dimensionIndex == getElevationDimensionIndex()) {
                readParameter.append(elevationIndex);
            } else if (dimensionIndex == getRuntimeDimensionIndex()) {
                readParameter.append(runtimeIndex);
            } else {
                if (LOG.isLoggable(Level.SEVERE)) {
                    LOG.log(Level.SEVERE,
                            "Encountered an unexpected dimension [{0}] for this Variable [{1}].",
                            new Object[] { dimensions.get(dimensionIndex), variable });
                }
            }

            // Append a comma except on the last one.
            if (dimensionIndex < dimensions.size() - 1) {
                readParameter.append(", ");
            }
        }

        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "NetCDF Variable [{0}] read parameter: {1}", new Object[] {
                    variable.getShortName(), readParameter });
        }

        return readParameter.toString();
    }

    /**
     * Create a new Index object to pull a Variable value for a specific coordinate from the Array returned by the Variable.read() method call.
     * 
     * @param readArray - The multi-dimensional Array returned by the Variable.read() method call.
     * @param longitudeIndex - The longitude index in the readArray
     * @param latitudeIndex - The latitude index in the readArray
     * @return Index
     */
    protected Index getReadArrayIndex(Array readArray, int longitudeIndex, int latitudeIndex) {
        // Create an array of indices that is the size of the number of dimensions on the variable.
        int[] indices = new int[variable.getRank()];

        // Get the index object for the readArray
        Index arrayIndex = readArray.getIndex();

        // Since (at least for now) we are only going to be reading a two-dimensional slice of the
        // data, only set the longitude and latitude indices, and assume the other dimensions to
        // be the 0th index.
        indices[getLongitudeDimensionIndex()] = longitudeIndex;
        indices[getLatitudeDimensionIndex()] = latitudeIndex;

        arrayIndex.set(indices);

        return arrayIndex;
    }

    /**
     * Variable values can be stored as Shorts to conserve space. The Variable can have a scaleFactor and an addOffset to adjust the Short value back
     * into its original Float value.
     * 
     * @param value
     * @return float
     */
    protected float getAdjustedValue(float value) {
        return (value * scaleFactor) + addOffset;
    }

    /**
     * Check to see if the value is the Missing Value specified in the Variable's Attributes.
     * 
     * @param value
     * @return boolean
     */
    protected boolean isMissingValue(float value) {
        return Math.abs(value - missingValue) < .0000001;
    }

    /**
     * Check to see if the value is the Fill Value specified in the Variable's Attributes.
     * 
     * @param value
     * @return boolean
     */
    protected boolean isFillValue(float value) {
        return Math.abs(value - fillValue) < .0000001;
    }

    /**
     * Get the minimum value for a Map.
     * 
     * @param coordinates - Map where the keys are request indices and the values are file indices.
     * @return Integer
     */
    protected Integer getMin(Map<Integer, Integer> coordinates) {
        return Collections.min(coordinates.values());
    }

    /**
     * Get the maximum value for a Map.
     * 
     * @param coordinates - Map where the keys are request indices and the values are file indices.
     * @return Integer
     */
    protected Integer getMax(Map<Integer, Integer> coordinates) {
        return Collections.max(coordinates.values());
    }

    public void setVariable(Variable variable) {
        this.variable = variable;

        // Set the scale factor if this Variable has one.
        Attribute scaleFactorAttribute = this.variable.findAttribute("scale_factor");
        if (null != scaleFactorAttribute) {
            this.scaleFactor = scaleFactorAttribute.getNumericValue().floatValue();
        }

        // Set the add offset if this Variable has one.
        Attribute addOffsetAttribute = this.variable.findAttribute("add_offset");
        if (null != addOffsetAttribute) {
            this.addOffset = addOffsetAttribute.getNumericValue().floatValue();
        }

        // Set the missing value if this Variable has one.
        Attribute missingValueAttribute = this.variable.findAttribute("missing_value");
        if (null != missingValueAttribute) {
            this.missingValue = missingValueAttribute.getNumericValue().floatValue();
        }

        // Set the fill value if this Variable has one.
        Attribute fillValueAttribute = this.variable.findAttribute("_FillValue");
        if (null != fillValueAttribute) {
            this.fillValue = fillValueAttribute.getNumericValue().floatValue();
        }
    }

    public void setElevationIndex(int index) {
        this.elevationIndex = index;
    }

    @Override
    public void setElevationVariableNameInFile(String elevationVariableNameInFile) {
        this.elevationVariableNameInFile = elevationVariableNameInFile;
    }

    public void setTimeIndex(int index) {
        this.timeIndex = index;
    }

    @Override
    public void setTimeVariableNameInFile(String timeVariableNameInFile) {
        this.timeVariableNameInFile = timeVariableNameInFile;
    }

    public void setRuntimeIndex(int index) {
        this.runtimeIndex = index;
    }

    @Override
    public void setRuntimeVariableNameInFile(String runtimeVariableNameInFile) {
        this.runtimeVariableNameInFile = runtimeVariableNameInFile;
    }

    public int getElevationDimensionIndex() {
        return NetCdfUtil.getVariableDimensionIndexByName(variable, elevationVariableNameInFile,
                NetCdfUtil.ELEVATION_VARIABLE_NAMES);
    }

    @Override
    public int getTimeDimensionIndex() {
        return NetCdfUtil.getVariableDimensionIndexByName(variable, timeVariableNameInFile,
                NetCdfUtil.TIME_VARIABLE_NAMES);
    }

    public int getRuntimeDimensionIndex() {
        return NetCdfUtil.getVariableDimensionIndexByName(variable, runtimeVariableNameInFile,
                NetCdfUtil.RUNTIME_VARIABLE_NAMES);
    }

    public int getLongitudeDimensionIndex() {
        return NetCdfUtil.getVariableDimensionIndexByName(variable, NetCdfUtil.LON_VARIABLE_NAMES);
    }

    public int getLatitudeDimensionIndex() {
        return NetCdfUtil.getVariableDimensionIndexByName(variable, NetCdfUtil.LAT_VARIABLE_NAMES);
    }

    public void read(Map<Integer, Integer> longitudes, Map<Integer, Integer> latitudes,
            GrdDataEncapsulator data) throws IOException, InvalidRangeException {
    }
}
