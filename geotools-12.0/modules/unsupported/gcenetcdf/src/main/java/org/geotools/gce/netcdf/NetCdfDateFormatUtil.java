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

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Provides static DateFormat utility objects for NetCDF project.
 */
public class NetCdfDateFormatUtil {

    // private static final Logger LOG = Logging.getLogger(NetCdfDateFormatUtil.class);

    // private ThreadLocal<SimpleDateFormat> netcdfDateFormat1 = new ThreadLocal<SimpleDateFormat>();

    private static final String STRING_DATE_FORMAT_1 = "yyyy-MM-dd HH:mm:ss";

    // private ThreadLocal<SimpleDateFormat> netcdfDateFormat2 = new ThreadLocal<SimpleDateFormat>();

    private static final String STRING_DATE_FORMAT_2 = "yyyy-MM-dd HH:mm";

    // private ThreadLocal<SimpleDateFormat> outputDateFormat = new ThreadLocal<SimpleDateFormat>();

    private static final String STRING_DATE_FORMAT_3 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final TimeZone NETCDF_TIME_ZONE = TimeZone.getTimeZone("GMT");

    // Util class, access statically, do not instantiate.
    private NetCdfDateFormatUtil() {
        throw new AssertionError();
    }

    /**
     * @return the nETCDF_DATE_FORMAT_1
     */
    // public SimpleDateFormat getNetcdfDateFormat1() {
    // SimpleDateFormat sdf = netcdfDateFormat1.get();
    // if (sdf == null) {
    // sdf = new SimpleDateFormat(STRING_DATE_FORMAT_1);
    // sdf.setTimeZone(NETCDF_TIME_ZONE);
    // netcdfDateFormat1.set(sdf);
    // }
    // return sdf;
    // }

    /**
     * @return SimpleDateFormat yyyy-MM-dd HH:mm:ss
     */
    public static SimpleDateFormat getDateFormat1() {
        SimpleDateFormat sdf = new SimpleDateFormat(STRING_DATE_FORMAT_1);
        sdf.setTimeZone(NETCDF_TIME_ZONE);
        return sdf;
    }

    /**
     * @return the nETCDF_DATE_FORMAT_2
     */
    // public SimpleDateFormat getNetcdfDateFormat2() {
    // SimpleDateFormat sdf = netcdfDateFormat2.get();
    // if (sdf == null) {
    // sdf = new SimpleDateFormat(STRING_DATE_FORMAT_2);
    // sdf.setTimeZone(NETCDF_TIME_ZONE);
    // netcdfDateFormat1.set(sdf);
    // }
    // return sdf;
    // }

    /**
     * @return SimpleDateFormat yyyy-MM-dd HH:mm
     */
    public static SimpleDateFormat getDateFormat2() {
        SimpleDateFormat sdf = new SimpleDateFormat(STRING_DATE_FORMAT_2);
        sdf.setTimeZone(NETCDF_TIME_ZONE);
        return sdf;
    }

    /**
     * @return the oUTPUT_DATE_FORMAT
     */
    // public SimpleDateFormat getOutputDateFormat() {
    // SimpleDateFormat sdf = outputDateFormat.get();
    // if (sdf == null) {
    // sdf = new SimpleDateFormat(STRING_OUTPUT_DATE_FORMAT);
    // sdf.setTimeZone(NETCDF_TIME_ZONE);
    // netcdfDateFormat1.set(sdf);
    // }
    // return sdf;
    // }

    /**
     * @return SimpleDateFormat yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
     */
    public static SimpleDateFormat getDateFormat3() {
        SimpleDateFormat sdf = new SimpleDateFormat(STRING_DATE_FORMAT_3);
        sdf.setTimeZone(NETCDF_TIME_ZONE);
        return sdf;
    }
}