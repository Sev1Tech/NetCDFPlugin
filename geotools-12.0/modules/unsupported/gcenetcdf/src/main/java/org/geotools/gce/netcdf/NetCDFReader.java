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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.gce.netcdf.fileparser.NetCDFFileInspector;
import org.geotools.gce.netcdf.log.LogUtil;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Provides a GridCoverageReader for NetCDF data files. NetCDF data files can contain many dimensions, and this currently handles elevation, time and
 * reference time.
 */
public class NetCDFReader extends AbstractGridCoverage2DReader implements GridCoverageReader {

    public static final String HAS_DIM_REFERENCE_TIME_DOMAIN = "HAS_REFERENCE_TIME_DOMAIN";

    public static final String DIM_REFERENCE_TIME_DOMAIN = "REFERENCE_TIME_DOMAIN";

    /**
     * name of parameter hint that may hold additional information about the dimension. first use is for any non-default variable and attribute names
     * used for the dimensions in the file. protected only for unit test.
     */
    protected static final String HINT_DIMENSION_ATTRIBUTE_NAME = "Attribute";

    /**
     * name of parameter hint that may hold the dimension's default value.
     */
    private static final String HINT_DIMENSION_DEFAULT_VALUE_NAME = "DefaultValue";

    private static final Logger LOG = Logging.getLogger(NetCDFReader.class);

    private NetCDFFileInspector fileInsp;

    /**
     * constructor.
     */
    public NetCDFReader(File cdmFile, Hints hints) {
        Date methodBeginDate = new Date();

        fileInsp = new NetCDFFileInspector(cdmFile);
        coverageName = cdmFile.getName();

        // Set up a bunch of properties needed by the super class:
        crs = calculateCoordinateReferenceSystem();

        // originalEnvelope contains the bounds of the file
        originalEnvelope = fileInsp.getOriginalEnvelope(crs);

        // originalGridRange is a GridEnvelope2D created from the dimensions
        // Rectangle. Basically the number of lat and lon points in the file.
        originalGridRange = fileInsp.getOriginalGridRange();

        // highestRes is a double array containing the data resolution of the file.
        // Basically the lat/lon range divided by the number of lat/lon points.
        highestRes = calculateHighestResolution(originalEnvelope, fileInsp.getOriginalDim(), crs);

        numOverviews = 0;

        LogUtil.logElapsedTime(LOG, methodBeginDate, this.getFileInsp().getFileName());
    }

    private CoordinateReferenceSystem calculateCoordinateReferenceSystem() {
        CoordinateReferenceSystem result = null;

        try {
            // TODO support other coords
            result = CRS.decode("EPSG:4326");
            
        } catch (NoSuchAuthorityCodeException e1) {
            LOG.log(Level.SEVERE, "Unable to get CRS EPSG:4326.", e1);
        } catch (FactoryException e1) {
            LOG.log(Level.SEVERE, "Unable to get CRS EPSG:4326.", e1);
        }

        return result;
    }

    private double[] calculateHighestResolution(GeneralEnvelope envelope, Rectangle2D rectangle2D,
            CoordinateReferenceSystem crs) {
        double[] result = null;

        try {
            // TODO fix setting this resolution; research highestRes
            result = getResolution(envelope, rectangle2D, crs);
        } catch (DataSourceException e) {
            LOG.log(Level.SEVERE, "Unable to get resolution for NetCDF coverage.", e);
        }
        return result;
    }

    @Override
    public GridCoverage2D read(GeneralParameterValue[] params) throws IOException {
        Date methodBeginDate = new Date();

        ParamInformation paramInfo = paramReader(params);
        if (params == null) {
            throw new IllegalArgumentException("Params must not be null");
        }

        GrdDataEncapsulator ncData = this.getFileInsp().parseFiles(paramInfo);

        final GridCoverageFactory factory = new GridCoverageFactory(hints);
        GridCoverage2D coverage = factory.create(this.coverageName, ncData.getWritableRaster(),
                ncData.getGeneralEnvelope());

        LogUtil.logElapsedTime(LOG, methodBeginDate, this.getFileInsp().getFileName());

        return coverage;

    }

    /**
     * read the parameters and set variables in the returned ParamInformation and also set the 'variable name in file' properties of the NetCDFReader.
     * 
     * paramReader, toNativeCrs and toReferencedEnvelope are heavily based on the ArcSDEGridCoverage2DReaderJAI class in geotools.
     * 
     * protected instead of private only for unit test.
     */
    protected ParamInformation paramReader(GeneralParameterValue[] params) {
        ParamInformation parsedParams = new ParamInformation();

        if (params == null) {
            throw new IllegalArgumentException("No GeneralParameterValue given to read operation");
        }

        GeneralEnvelope reqEnvelope = null;
        GridEnvelope dim = null;

        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                LOG.log(Level.INFO, "Parameter was null");
                continue;
            }
            final ParameterValue<?> param = (ParameterValue<?>) params[i];
            final String name = param.getDescriptor().getName().getCode();
            if (name.equals(NetCDFFormat.TIME.getName().toString())) {
                if (param.getValue() != null) {
                    parsedParams.setTime(getParameterValueAsDate(param.getValue(),
                            NetCDFFormat.TIME.getName().toString()));
                }
                // get a non-default file variable name, if provided.
                // this.fileInsp.setTimeVariableNameInFile(param.getHints().get(ATTRIBUTE));
                this.fileInsp.setTimeVariableNameInFile(getHintValueIfGetHintsMethodIsAvailable(
                        param, HINT_DIMENSION_ATTRIBUTE_NAME));
            } else if (name.equals(NetCDFFormat.REFERENCE_TIME.getName().toString())) {
                if (param.getValue() != null) {
                    parsedParams.setReferenceTime(getParameterValueAsDate(param.getValue(),
                            NetCDFFormat.REFERENCE_TIME.getName().toString()));
                }
                // get a non-default file variable name, if provided.
                // this.fileInsp.setRuntimeVariableNameInFile(param.getHints().get(ATTRIBUTE));
                this.fileInsp.setRuntimeVariableNameInFile(getHintValueIfGetHintsMethodIsAvailable(
                        param, HINT_DIMENSION_ATTRIBUTE_NAME));
                // TODO: this is just a start, for reference time, we have other file specific names to consider.
                // if no runtime var in file, we then look for tau var, and its time_origin attribute, and then global time_origin attribute.
            } else if (name.equals(NetCDFFormat.ELEVATION.getName().toString())) {
                parsedParams.setElevation(getElevationParameterValue(param,
                        parsedParams.getParameter()));
                // get a non-default file variable name, if provided.
                // this.fileInsp.setElevationVariableNameInFile(param.getHints().get(ATTRIBUTE));
                this.fileInsp
                        .setElevationVariableNameInFile(getHintValueIfGetHintsMethodIsAvailable(
                                param, HINT_DIMENSION_ATTRIBUTE_NAME));
            } else if (name.equals(NetCDFFormat.PARAMETER.getName().toString())) {
                parsedParams.setParameter((String) param.getValue());
            } else if (name.equals(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString())) {
                final GridGeometry2D gg = (GridGeometry2D) param.getValue();
                reqEnvelope = new GeneralEnvelope((Envelope) gg.getEnvelope2D());

                final GeneralEnvelope coverageEnvelope = getOriginalEnvelope();
                CoordinateReferenceSystem nativeCrs = coverageEnvelope
                        .getCoordinateReferenceSystem();
                CoordinateReferenceSystem requestCrs = reqEnvelope.getCoordinateReferenceSystem();
                if (!CRS.equalsIgnoreMetadata(nativeCrs, requestCrs)) {
                    ReferencedEnvelope nativeCrsEnv;
                    nativeCrsEnv = toNativeCrs(reqEnvelope, nativeCrs);
                    reqEnvelope = new GeneralEnvelope(nativeCrsEnv);
                }

                dim = gg.getGridRange2D();
            } else if (name.equals(AbstractGridFormat.OVERVIEW_POLICY.getName().toString())) {
                OverviewPolicy overviewPolicy = (OverviewPolicy) param.getValue();
                parsedParams.setOverviewPolicy((overviewPolicy == null) ? OverviewPolicy.NEAREST
                        : overviewPolicy);
            } else {
                LOG.log(Level.INFO, "During request for 'read', parameter name '" + name
                        + "' was not handled.");
            }
        }

        // more work to do if either reqEnvelope and dim are null.
        if (reqEnvelope == null && dim == null) {
            reqEnvelope = getOriginalEnvelope();
            dim = getOriginalGridRange();
        }

        if (reqEnvelope == null) {
            reqEnvelope = getOriginalEnvelope();
        }
        if (dim == null) {
            final GeneralEnvelope adjustedGRange;
            try {
                MathTransform gridToWorld = getOriginalGridToWorld(PixelInCell.CELL_CENTER);
                MathTransform worldToGrid = gridToWorld.inverse();
                adjustedGRange = CRS.transform(worldToGrid, reqEnvelope);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            int xmin = (int) Math.floor(adjustedGRange.getMinimum(0));
            int ymin = (int) Math.floor(adjustedGRange.getMinimum(1));
            int xmax = (int) Math.ceil(adjustedGRange.getMaximum(0));
            int ymax = (int) Math.ceil(adjustedGRange.getMaximum(1));
            dim = new GridEnvelope2D(xmin, ymin, xmax - xmin, ymax - ymin);
        }

        // validate extent.
//        if (!reqEnvelope.intersects(getOriginalEnvelope(), true)) {
//            throw new IllegalArgumentException(
//                    "The requested extend does not overlap the coverage extent: "
//                            + getOriginalEnvelope());
//        }

        parsedParams.setRequestedEnvelope(reqEnvelope);
        parsedParams.setDim(dim);

        return parsedParams;

    }

    /**
     * our call to the ParameterValue class's getHints relies on our patched version of gt-referencing and gt-opengis libraries. The official 8.6
     * version does not include that method. this method checks for its existence before calling the method, so that this code can compile if
     * committed to the GeoTools project without our patch.
     * 
     * if we know the getHints() method is present in the GeoTools code, we can remove the use of this method and make a direct call on the getHints
     * method.
     * <ul>
     * <li>replace:</li>
     * <li>this.fileInsp.setTimeVariableNameInFile(getAttributeValueIfAvailable(param))</li>
     * <li>with:</li>
     * <li>this.fileInsp.setTimeVariableNameInFile(param.getHints().get(ATTRIBUTE));</li>
     * </ul>
     * 
     * protected instead of private only for unit test.
     */
    protected String getHintValueIfGetHintsMethodIsAvailable(ParameterValue<?> parameterValue,
            String targetHint) {
        String result = null;
        String methodName = "getHints";
        Class<?>[] methodParameters = new Class[] {};
        try {
            Method method = parameterValue.getClass().getMethod(methodName, methodParameters);
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) method.invoke(parameterValue);
            result = map.get(targetHint);
        } catch (NoSuchMethodException e) {
            LOG.log(Level.INFO,
                    "ParameterValue class does not have getHints method, can not get {0}.",
                    targetHint);
        } catch (SecurityException e) {
            LOG.log(Level.FINER, e.getMessage(), e);
        } catch (IllegalAccessException e) {
            LOG.log(Level.FINER, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.log(Level.FINER, e.getMessage(), e);
        } catch (InvocationTargetException e) {
            LOG.log(Level.FINER, e.getMessage(), e);
        }
        return result;
    }

    /**
     * @param param the request parameter information for elevation
     * @param parameter for logging msg. the parameter(variable) (too many meanings of the word 'parameter'!)
     */
    private Object getElevationParameterValue(ParameterValue<?> param, String parameter) {
        Object result = null;

        // first, look at the param's value. try first as Double and if not, then Object.
        result = getParameterValueAsDouble(param.getValue(), NetCDFFormat.ELEVATION.getName()
                .toString());
        if (result == null) {
            result = getParameterValueAsObject(param.getValue(), NetCDFFormat.ELEVATION.getName()
                    .toString());
        }

        // if not found, look for default value in the param's hints.
        if (result == null) {
            String defaultElevationAsString = getHintValueIfGetHintsMethodIsAvailable(param,
                    HINT_DIMENSION_DEFAULT_VALUE_NAME);
            Double defaultElevationAsDouble = convertStringToDouble(defaultElevationAsString);
            result = (defaultElevationAsDouble != null) ? defaultElevationAsDouble
                    : defaultElevationAsString;
        }

        // if still not found, log. note that the parameter may not be known yet.
        if (result == null) {
            LOG.log(Level.INFO,
                    "Could not get elevation or default elevation from request for NetCDFReader read.  file:parameter ({0}:{1})",
                    new Object[] { this.fileInsp.getFileName(), parameter });
        }

        return result;
    }

    private Date getParameterValueAsDate(Object parameterValue, String msgLabel) {
        Date result = null;

        List<?> values = (List<?>) parameterValue;
        if (!values.isEmpty()) {
            Object firstValue = values.get(0);
            if (firstValue instanceof Date) {
                result = (Date) firstValue;
            } else if (firstValue instanceof String) {
                SimpleDateFormat sdf = NetCdfDateFormatUtil.getDateFormat3();
                try {
                    result = sdf.parse((String) firstValue);
                } catch (ParseException e) {
                    LOG.log(Level.WARNING, "Could not parse date for request parameter " + msgLabel
                            + ": " + firstValue + ".", e);
                }
            }
        }
        if (values.size() > 1) {
            LOG.log(Level.INFO, "Requested Parameter for " + msgLabel
                    + " list has more than one value: {0}.  Using first.", values.size());
        }
        return result;
    }

    private Double getParameterValueAsDouble(Object parameterValue, String msgLabel) {
        Double result = null;

        List<?> values = (List<?>) parameterValue;
        if (!values.isEmpty()) {
            Object firstValue = values.get(0);
            if (firstValue instanceof Number) {
                result = (Double) firstValue;
            } else if (firstValue instanceof String) {
                result = convertStringToDouble((String) firstValue);
            }
            if (values.size() > 1) {
                LOG.log(Level.INFO, "Requested Parameter for " + msgLabel
                        + " list has more than one value: {0}.  Using first.", values.size());
            }
        }

        return result;
    }

    private Object getParameterValueAsObject(Object parameterValue, String msgLabel) {
        Object result = null;

        List<?> values = (List<?>) parameterValue;
        if (!values.isEmpty()) {
            result = values.get(0);
        }
        if (values.size() > 1) {
            LOG.log(Level.INFO, "Requested Parameter for " + msgLabel
                    + " list has more than one value: {0}.  Using first.", values.size());
        }
        return result;
    }

    /**
     * @return Double if successful, null if not.
     */
    private Double convertStringToDouble(String value) {
        Double result = null;

        if (value != null) {
            try {
                result = Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                // no action required. result already set to null.
            }
        }

        return result;
    }

    private static ReferencedEnvelope toNativeCrs(final GeneralEnvelope requestedEnvelope,
            final CoordinateReferenceSystem nativeCRS) {

        ReferencedEnvelope reqEnv = toReferencedEnvelope(requestedEnvelope);

        if (!CRS.equalsIgnoreMetadata(nativeCRS, reqEnv.getCoordinateReferenceSystem())) {
            try {
                reqEnv = reqEnv.transform(nativeCRS, true);
            } catch (FactoryException fe) {
                throw new IllegalArgumentException("Unable to find a reprojection from requested "
                        + "coordsys to native coordsys for this request", fe);
            } catch (TransformException te) {
                throw new IllegalArgumentException("Unable to perform reprojection from requested "
                        + "coordsys to native coordsys for this request", te);
            }
        }
        return reqEnv;
    }

    private static ReferencedEnvelope toReferencedEnvelope(GeneralEnvelope envelope) {
        double minx = envelope.getMinimum(0);
        double maxx = envelope.getMaximum(0);
        double miny = envelope.getMinimum(1);
        double maxy = envelope.getMaximum(1);
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        ReferencedEnvelope refEnv = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        return refEnv;
    }

    /*
     * These two methods provide the support for Time and Elevation. Check out ImageMosaicReader from GeoTools-8 imagemosaic datasource module for
     * more inspiration.
     */
    @Override
    public String[] getMetadataNames() {
        final String[] parentNames = super.getMetadataNames();
        final List<String> metadataNames = new ArrayList<String>();
        metadataNames.add(TIME_DOMAIN);
        metadataNames.add(HAS_TIME_DOMAIN);
        metadataNames.add(TIME_DOMAIN_MINIMUM);
        metadataNames.add(TIME_DOMAIN_MAXIMUM);
        metadataNames.add(TIME_DOMAIN_RESOLUTION);
        metadataNames.add(ELEVATION_DOMAIN);
        metadataNames.add(ELEVATION_DOMAIN_MINIMUM);
        metadataNames.add(ELEVATION_DOMAIN_MAXIMUM);
        metadataNames.add(HAS_ELEVATION_DOMAIN);
        metadataNames.add(ELEVATION_DOMAIN_RESOLUTION);
        metadataNames.add(DIM_REFERENCE_TIME_DOMAIN);
        metadataNames.add(HAS_DIM_REFERENCE_TIME_DOMAIN);

        if (parentNames != null) {
            metadataNames.addAll(Arrays.asList(parentNames));
        }

        return metadataNames.toArray(new String[metadataNames.size()]);
    }

    @Override
    public String getMetadataValue(final String name) {
        final String superValue = super.getMetadataValue(name);
        if (superValue != null) {
            return superValue;
        }

        if (name.equalsIgnoreCase(HAS_ELEVATION_DOMAIN)) {
            SortedSet<Object> elevations = fileInsp.getElevations();
            if (elevations != null && elevations.size() > 0) {
                return String.valueOf(true);
            } else {
                return String.valueOf(false);
            }
        }

        /*
         * TODO: This is a huge assumption, our data will NOT always have time and reference time, we are just assuming it will always be 4d data for
         * simplicitiy, in the future this must be fixed!
         */
        if (name.equalsIgnoreCase(HAS_TIME_DOMAIN)) {
            return String.valueOf(true);
        }

        if (name.equalsIgnoreCase(HAS_DIM_REFERENCE_TIME_DOMAIN)) {
            return String.valueOf(true);
        }

        /* Get the time string */
        if (name.equalsIgnoreCase(TIME_DOMAIN)) {
            return fileInsp.getTimeString();
        }
        /* Get the elevation string */
        if (name.equalsIgnoreCase(ELEVATION_DOMAIN)) {
            return fileInsp.getElevationString();
        }
        /* Get the reference_time (model run time) string */
        if (name.equalsIgnoreCase(DIM_REFERENCE_TIME_DOMAIN)) {
            return fileInsp.getReferenceTimeString();
        }

        if (name.equalsIgnoreCase(TIME_DOMAIN_MINIMUM)) {
            return fileInsp.getTimeMinimum();
        }
        if (name.equalsIgnoreCase(TIME_DOMAIN_MAXIMUM)) {
            return fileInsp.getTimeMaximum();
        }

        if (name.equalsIgnoreCase(NetCDFFormat.NETCDF_PARAMETER_NAME)) {
            return fileInsp.getVariablesString();
        }
        return superValue;
    }

    public NetCDFFileInspector getFileInsp() {
        return fileInsp;
    }

    public Format getFormat() {
        return new NetCDFFormat();
    }
}