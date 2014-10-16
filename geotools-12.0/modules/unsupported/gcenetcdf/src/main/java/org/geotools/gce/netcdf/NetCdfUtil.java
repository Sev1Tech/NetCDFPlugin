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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geotools.util.logging.Logging;

import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * Provides static utility objects for NetCDF project.
 */
public class NetCdfUtil {

    private static final Logger LOG = Logging.getLogger(NetCdfUtil.class);

    public static final float MIN_LON = -180.0f;

    public static final float MAX_LON = 180.0f;

    public static final float MIN_LAT = -90.0f;

    public static final float MAX_LAT = 90.0f;

    public static final int BOUNDS_INDEX_MIN_LONGITUDE = 0;

    public static final int BOUNDS_INDEX_MAX_LONGITUDE = 1;

    public static final int BOUNDS_INDEX_MIN_LATITUDE = 2;

    public static final int BOUNDS_INDEX_MAX_LATITUDE = 3;

    public static final int NOT_FOUND = -1;

    /**
     * http://mindprod.com/jgloss/floatingpoint.html
     */
    public static final double FLOATING_POINT_EPSILON = 0.0000001;

    public static final String ELEVATION_SURFACE = "SFC";

    public static final String ELEVATION_VARIABLE_NAME_DEPTH = "depth";

    public static final String ELEVATION_VARIABLE_NAME_HEIGHT = "height";

    public static final Collection<String> ELEVATION_VARIABLE_NAMES_MAY_BE_SURFACE = Arrays.asList(
            ELEVATION_VARIABLE_NAME_DEPTH, ELEVATION_VARIABLE_NAME_HEIGHT);

    /**
     * The delimiter used for representing a domain list as a string.
     */
    public static final String LIST_AS_STRING_DELIMITER = ",";

    /**
     * Default Collections of names that we will watch for in NetCDF files.
     */
    public static final Collection<String> LON_VARIABLE_NAMES = Arrays.asList("lon", "longitude");

    public static final Collection<String> LAT_VARIABLE_NAMES = Arrays.asList("lat", "latitude");

    public static final Collection<String> ELEVATION_VARIABLE_NAMES = Arrays.asList(
            ELEVATION_VARIABLE_NAME_DEPTH, ELEVATION_VARIABLE_NAME_HEIGHT, "pressure", "sigma");

    public static final Collection<String> RUNTIME_VARIABLE_NAMES = Arrays.asList("runtime");

    public static final Collection<String> TIME_VARIABLE_NAMES = Arrays.asList("time");

    public static final Collection<String> TAU_VARIABLE_NAMES = Arrays.asList("tau");

    public static final Collection<String> TIME_ORIGIN_ATTRIBUTE_NAMES = Arrays
            .asList("time_origin");

    public static final Collection<String> TIME_UNIT_ATTRIBUTE_NAMES = Arrays.asList("units",
            "time_units");

    // Util class, access statically, do not instantiate.
    private NetCdfUtil() {
        throw new AssertionError();
    }

    /**
     * Get a file variable by preferred name, or if not provided or found, by default names.
     */
    public static Variable getFileVariableByName(NetcdfFile netCdfFile, String preferredName,
            Collection<String> defaultNames) {
        Variable result = null;

        if (netCdfFile != null) {
            if (preferredName != null) {
                result = netCdfFile.findVariable(preferredName);
            }
            if (result == null) {
                result = getFileVariableByName(netCdfFile, defaultNames);
            }
        }
        return result;
    }

    /**
     * No known rule for which value to return if more than one variable name found. Current implementation will return the first one found.
     */
    public static Variable getFileVariableByName(NetcdfFile netCdfFile,
            Collection<String> variableNames) {
        if (netCdfFile != null) {
            Variable result = null;
            for (String variableName : variableNames) {
                result = netCdfFile.findVariable(variableName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * No known rule for which value to return if more than one attribute name found. Current implementation will return the first one found.
     */
    public static Attribute getVariableAttributeByName(Variable var,
            Collection<String> attributeNames) {
        if (var != null) {
            for (Attribute attribute : var.getAttributes()) {
                for (String attributeName : attributeNames) {
                    if (attributeName.equalsIgnoreCase(attribute.getName())) {
                        return attribute;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Get a variable dimension index by preferred name, or if not provided or found, by default names.
     */
    public static int getVariableDimensionIndexByName(Variable var, String preferredName,
            Collection<String> defaultNames) {
        int result = NOT_FOUND;

        if (var != null) {
            // look for preferred.
            if (preferredName != null) {
                int dimensionIndex = 0;
                for (Dimension dimension : var.getDimensions()) {
                    if (dimension.getName().equals(preferredName)) {
                        result = dimensionIndex;
                        break;
                    }
                    dimensionIndex++;
                }
            }
            // if not found, look for default.
            if (result == NOT_FOUND) {
                result = getVariableDimensionIndexByName(var, defaultNames);
            }
        }

        return result;
    }

    /**
     * No known rule for which value to return if more than one dimension name found. Current implementation will return the first one found. Returns
     * -1 if not found.
     */
    public static int getVariableDimensionIndexByName(Variable var,
            Collection<String> dimensionNames) {
        if (var != null) {
            int dimensionIndex = 0;
            for (Dimension dimension : var.getDimensions()) {
                for (String dimensionName : dimensionNames) {
                    if (dimension.getName().equals(dimensionName)) {
                        return dimensionIndex;
                    }
                }
                dimensionIndex++;
            }
        }

        return NOT_FOUND;
    }

    /**
     * limitation: so far, only tested for variable with 1D String array.
     */
    public static List<Object> getVariableCachedData(Variable var) {
        List<Object> result = new ArrayList<Object>();
        if (var != null) {
            try {
                IndexIterator iter = var.read().getIndexIterator();
                while (iter.hasNext()) {
                    result.add(iter.next());
                }
            } catch (IOException e) {
                LOG.info("Problem reading cached data for " + var.getName());
            }
        }

        return result;
    }

    /**
     * limitation: so far, only tested for variable with 1D String array.
     */
    public static String getVariableCachedDataAsString(Variable var) {
        String result = null;
        Collection<Object> cachedData = getVariableCachedData(var);
        if (cachedData != null) {
            result = getDomainListAsString(cachedData);
        }

        return result;
    }

    /**
     * limitation: so far, only tested for variable with 1D String array.
     */
    public static int getIndexOfMatchInVariableCachedData(Variable var, String targetValue) {
        if (var != null && targetValue != null) {
            try {
                IndexIterator iter = var.read().getIndexIterator();
                while (iter.hasNext()) {
                    if (targetValue.equals(iter.next())) {
                        return iter.getCurrentCounter()[0];
                    }
                }
            } catch (IOException e) {
                LOG.info("Problem reading cached data for " + var.getName());
            }
        }

        return NetCdfUtil.NOT_FOUND;
    }

    public static Date getDateFromFileAttributeString(String input) {
        Date result = null;

        String dateString = getDateStringFromExpectedFileAttributeString(input);

        // Note: order is important!
        List<SimpleDateFormat> netCdfFormats = Arrays.asList(NetCdfDateFormatUtil.getDateFormat1(),
                NetCdfDateFormatUtil.getDateFormat2());

        for (SimpleDateFormat netCdfFormat : netCdfFormats) {
            try {
                result = netCdfFormat.parse(dateString);
            } catch (ParseException e) {
                LOG.warning("Unparseable date (NetCDF style) " + input);
            }
        }

        return result;
    }

    /**
     * strip the attribute value down to just the date. designed for a netcdf file attribute in expected format: "hour since 2000-01-01 00:00:00" to
     * reduce it to "2000-01-01 00:00:00".
     */
    public static String getDateStringFromExpectedFileAttributeString(String input) {
        return input.replaceAll("\\p{Alpha}", "").trim();
    }

    public static Date getDateFromOutputStyleString(String input) {
        Date result = null;
        try {

            result = NetCdfDateFormatUtil.getDateFormat3().parse(input);
        } catch (ParseException e) {
            LOG.warning("Unparseable date (Output style) " + input);
        }

        return result;
    }

    /**
     * Returns elements in a <code>Collection</code> joined together using the <code>LIST_AS_STRING_DELIMITER</code> delimiter as a
     * <code>String</code>.
     * 
     * @param collection the <code>Collection</code> whose elements will be joined together as a <code>String</code>
     * 
     * @return String contains a delimited <code>List</code>
     */
    public static String getDomainListAsString(Collection<?> collection) {
        return StringUtils.join(collection.iterator(), LIST_AS_STRING_DELIMITER);
    }
}
