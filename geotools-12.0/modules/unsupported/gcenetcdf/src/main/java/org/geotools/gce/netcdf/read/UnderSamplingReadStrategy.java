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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.gce.netcdf.GrdDataEncapsulator;
import org.geotools.util.logging.Logging;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;

/**
 * ReadStrategy that will read data for a Variable with individual NetCDF file accesses for each coordinate.
 * 
 * @author Yancy Matherne <yancy.matherne@geocent.com>
 */
public class UnderSamplingReadStrategy extends AbstractReadStrategy {

    private static final Logger LOG = Logging.getLogger(UnderSamplingReadStrategy.class);

    @Override
    public void read(Map<Integer, Integer> longitudes, Map<Integer, Integer> latitudes,
            GrdDataEncapsulator data) throws IOException, InvalidRangeException {

        for (Map.Entry<Integer, Integer> latEntry : latitudes.entrySet()) {
            int latIndex = latEntry.getValue();

            for (Map.Entry<Integer, Integer> lonEntry : longitudes.entrySet()) {
                int lonIndex = lonEntry.getValue();

                // Read the Variable from the NetCDF file.
                // This returns a one-dimensional UCAR array.
                Array array = variable.read(getReadParameter(String.valueOf(lonIndex),
                        String.valueOf(latIndex)));

                // Since we are reading each point individually, the current value is always at the
                // 0th index.
                float dataValue = array.getFloat(0);
                float adjustedValue = getAdjustedValue(dataValue);

                // Don't add the point if it's a missing or fill value.
                // The point will just be NaN in the result coverage.
                if (isMissingValue(dataValue) || isMissingValue(adjustedValue)
                        || isFillValue(dataValue) || isFillValue(adjustedValue)) {
                    continue;
                }

                // Get the indices to use in the data object.
                int dataLonIndex = lonEntry.getKey();
                int dataLatIndex = (data.getDesiredLats().size() - 1) - latEntry.getKey();

                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Setting the data at lon[{0}] lat[{1}] to {2}",
                            new Object[] { dataLonIndex, dataLatIndex, adjustedValue });
                }

                // Add the current adjusted value from the file to the data object.
                data.getImageArray()[dataLonIndex][dataLatIndex] = adjustedValue;
            }
        }
    }
}
