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
package org.geotools.gce.netcdf.fileparser;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.gce.netcdf.GrdDataEncapsulator;
import org.geotools.gce.netcdf.NetCdfDateFormatUtil;
import org.geotools.gce.netcdf.NetCdfUtil;
import org.geotools.gce.netcdf.ParamInformation;
import org.geotools.gce.netcdf.index.IndexingStrategy;
import org.geotools.gce.netcdf.index.NearestNeighborAscending;
import org.geotools.gce.netcdf.index.NearestNeighborDescending;
import org.geotools.gce.netcdf.read.ReadStrategy;
import org.geotools.gce.netcdf.read.ReadStrategyFactory;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.util.logging.Logging;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Provides file access to a NetCDF data file and a place to assemble data from that file.
 * 
 * Intended as a support object for the NetCDFReader, particularly to generate a GridCoverage2D object as the result of the NetCDFReader read method.
 */
public class NetCDFFileInspector {
	private static final int MAX_LON_360 = 360;

	private static final int MAX_LON_180 = 180;

	private static final int MIN_LON_MINUS_180 = -MAX_LON_180;

	private static final int MIN_INDEX = 0;

	private static final int MAX_INDEX = 1;

	private static final Logger LOG = Logging.getLogger(NetCDFFileInspector.class);

	/*
	 * The NetCDF file itself.
	 * 
	 * TODO: Do we really need the file? It appears that everytime we use it, we just get the absolute path from it. Why not just store the path?
	 */
	private File file;

	/*
	 * float array: [minimum longitude, maximum longitude, minimum latitude, maximum latitude]
	 * 
	 * In a (-180 to 180) coordinate system.
	 */
	private float[] bounds;

	/*
	 * A rectangle describing the size of the data in the file. [0, 0, number of longitude points, number of latitude points]
	 */
	private Rectangle originalDim;

	/*
	 * Whether or not this NetCDF uses a (0 to 360) world grid.
	 */
	private boolean isLongitude0to360;

	/**
	 * name of time variable to look for in NetCDF store. allows customization and not required. if not provided, we will look for default.
	 */
	private String timeVariableNameInFile = null;

	/**
	 * name of elevation variable to look for in NetCDF store. allows customization and not required. if not provided, we will look for default.
	 */
	private String elevationVariableNameInFile = null;

	/**
	 * name of runtime variable to look for in NetCDF store. allows customization and not required. if not provided, we will look for default.
	 */
	private String runtimeVariableNameInFile = null;

	/**
	 * name of tau variable to look for in NetCDF store. allows customization and not required. if not provided, we will look for default.
	 */
	private String tauVariableNameInFile = null;

	/**
	 * name of tau variable's time origin attribute to look for in NetCDF store. allows customization and not required. if not provided, we will look
	 * for default.
	 */
	private String tauVariableTimeOriginAttributeNameInFile = null;

	/**
	 * name of global time origin attribute to look for in NetCDF store. allows customization and not required. if not provided, we will look for
	 * default.
	 */
	private String globalTimeOriginAttributeNameInFile = null;

	public String getTimeVariableNameInFile() {
		return timeVariableNameInFile;
	}

	public void setTimeVariableNameInFile(String timeVariableNameInFile) {
		this.timeVariableNameInFile = timeVariableNameInFile;
	}

	public String getElevationVariableNameInFile() {
		return elevationVariableNameInFile;
	}

	public void setElevationVariableNameInFile(String elevationVariableNameInFile) {
		this.elevationVariableNameInFile = elevationVariableNameInFile;
	}

	public String getRuntimeVariableNameInFile() {
		return runtimeVariableNameInFile;
	}

	public void setRuntimeVariableNameInFile(String runtimeVariableNameInFile) {
		this.runtimeVariableNameInFile = runtimeVariableNameInFile;
	}

	public String getTauVariableNameInFile() {
		return tauVariableNameInFile;
	}

	public void setTauVariableNameInFile(String tauVariableNameInFile) {
		this.tauVariableNameInFile = tauVariableNameInFile;
	}

	public String getTauVariableTimeOriginAttributeNameInFile() {
		return tauVariableTimeOriginAttributeNameInFile;
	}

	public void setTauVariableTimeOriginAttributeNameInFile(
			String tauVariableTimeOriginAttributeNameInFile) {
		this.tauVariableTimeOriginAttributeNameInFile = tauVariableTimeOriginAttributeNameInFile;
	}

	public String getGlobalTimeOriginAttributeNameInFile() {
		return globalTimeOriginAttributeNameInFile;
	}

	public void setGlobalTimeOriginAttributeNameInFile(String globalTimeOriginAttributeNameInFile) {
		this.globalTimeOriginAttributeNameInFile = globalTimeOriginAttributeNameInFile;
	}

	/**
	 * default no arg constructor. protected as only use so far is for tests that do not need file.
	 */
	protected NetCDFFileInspector() {
	}

	/**
	 * constructor.
	 */
	public NetCDFFileInspector(File file) {
		validateNetCdfFile(file.getAbsolutePath());
		this.file = file;
		setProperties();
	}

	/**
	 * Reads the NetCDF and gets information needed to populate bounds and originalDim.
	 */
	private void setProperties() {
		LOG.log(Level.INFO, "Getting Bounds For {0}", file.getAbsolutePath());
		/*
		 * Sane defaults in case something goes wrong
		 */
		float[] minAndMaxLon = new float[] { NetCdfUtil.MIN_LON, NetCdfUtil.MAX_LON };
		float[] minAndMaxLat = new float[] { NetCdfUtil.MIN_LAT, NetCdfUtil.MAX_LAT };

		NetcdfFile ncFile = null;
		try {
			// Open NetCDF
			ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);

			// Get lon and lat Variables from NetCDF
			Variable lon = NetCdfUtil.getFileVariableByName(ncFile, NetCdfUtil.LON_VARIABLE_NAMES);
			Variable lat = NetCdfUtil.getFileVariableByName(ncFile, NetCdfUtil.LAT_VARIABLE_NAMES);

			// Get the mins and maxes for lon and lat
			Array lonArray = lon.read();
			Array latArray = lat.read();
			minAndMaxLon = getMinAndMaxFromArray(lonArray);
			minAndMaxLat = getMinAndMaxFromArray(latArray);

			// compute the average offsets for lat/lon divided by 2
			float halfLonOffset = (minAndMaxLon[MAX_INDEX] - minAndMaxLon[MIN_INDEX])
					/ (lonArray.getSize() - 1) / 2;
			float halfLatOffset = (minAndMaxLat[MAX_INDEX] - minAndMaxLat[MIN_INDEX])
					/ (latArray.getSize() - 1) / 2;

			// Check to see if this file uses a (0 to 360) world grid
			if (null != minAndMaxLon) {
				this.isLongitude0to360 = minAndMaxLon[MAX_INDEX] > MAX_LON_180;
			}

			// Convert the lons if necessary
			minAndMaxLon = convertBoundsToNeg180to180(minAndMaxLon[MIN_INDEX],
					minAndMaxLon[MAX_INDEX]);

			// Create the bounds array
			// offsets added to bounds by Sam Foster
			// to handle GetFeatureInfo request offset problem
			this.bounds = new float[] { minAndMaxLon[MIN_INDEX] - halfLonOffset,
					minAndMaxLon[MAX_INDEX] + halfLonOffset,
					minAndMaxLat[MIN_INDEX] - halfLatOffset,
					minAndMaxLat[MAX_INDEX] + halfLatOffset };

			// Get the number of lons and lats
			int width = lon.getShape(0);
			int height = lat.getShape(0);

			// Create the dimension rectangle for this NetCDF
			// basically the number of points in the original data
			this.originalDim = new Rectangle(0, 0, width, height);

		} catch (Exception e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		} finally {
			closeNetCdfFile(ncFile);
		}
	}

	/**
	 * This is just a custom float array to hold the bounds. Uses a (-180 to 180) coordinate system.
	 * 
	 * TODO: Either find an existing class or create one to use instead. Perhaps just use the GeneralEnvelope?
	 * 
	 * @return float array: [minimum longitude, maximum longitude, minimum latitude, maximum latitude]
	 */
	public float[] getBounds() {
		return this.bounds.clone();
	}

	/**
	 * Get the dimension rectangle for this NetCDF. Basically the number of points in the original data.
	 * 
	 * @return Rectangle
	 */
	public Rectangle getOriginalDim() {
		return this.originalDim;
	}

	/**
	 * Creates a GeneralEnvelope from the bounds and a given CRS. Needed by the AbstractGridCoverage2DReader.
	 * 
	 * @param crs
	 * @return GeneralEnvelope
	 */
	public GeneralEnvelope getOriginalEnvelope(CoordinateReferenceSystem crs) {
		if (null == bounds) {
			return null;
		}

		GeneralEnvelope env = new GeneralEnvelope(new double[] {
				bounds[NetCdfUtil.BOUNDS_INDEX_MIN_LONGITUDE],
				bounds[NetCdfUtil.BOUNDS_INDEX_MIN_LATITUDE] }, new double[] {
				bounds[NetCdfUtil.BOUNDS_INDEX_MAX_LONGITUDE],
				bounds[NetCdfUtil.BOUNDS_INDEX_MAX_LATITUDE] });
		env.setCoordinateReferenceSystem(crs);

		return env;
	}

	/**
	 * Creates a GridEnvelope2D from the original dimensions. Needed by the AbstractGridCoverage2DReader.
	 * 
	 * @return GridEnvelope2D
	 */
	public GridEnvelope2D getOriginalGridRange() {
		if (null == originalDim) {
			return null;
		}

		return new GridEnvelope2D(originalDim);
	}

	public String getFileName() {
		return file.getName();
	}

	public boolean isLongitudeIn0to360() {
		return this.isLongitude0to360;
	}

	/**
	 * We have some files whose longitude's range is greater than 180, i.e. 281.5 to 288.0. There is nothing in the headers to indicate that the file
	 * is using a different grid system. So, I'm assuming they are using (0 to 360) as the world range. If min or max is greater than 180, assume the
	 * file's world longitude range is (0 to 360) and convert it to (-180 to 180)
	 */
	public float[] convertBoundsToNeg180to180(float min, float max) {
		float minimum = min;
		float maximum = max;
		// If the max is greater than 180 we assume the file is using a
		// (0 to 360) world range. If the min is less than 0, we have a mix of
		// (-180 to 180) and (0 to 360), and this is really a problem.
		if (maximum > MAX_LON_180 && minimum < 0) {
			LOG.log(Level.SEVERE,
					"Invalid file bounds: minimum longitude < 0 and maximum longitude > 180: ({0} to {1})",
					new Object[] { minimum, maximum });
			return null;

			// If the maximum is greater than 180 we assume the file is using a
			// (0 to 360) world range. If the minimum is less than 180, we end
			// up
			// with a wrap around scenario, in which case the bounding box
			// becomes the world.
		} else if (maximum > MAX_LON_180 && minimum < MAX_LON_180) {
			minimum = MIN_LON_MINUS_180;
			maximum = MAX_LON_180;

			// If the maximum is greater than 180 we assume the file is using a
			// (0 to 360) world range. If the minimum is also greater than 180,
			// the
			// bounds are completely in the western hemisphere, just convert
			// both by subtracting 360.
		} else if (maximum > MAX_LON_180 && minimum >= MAX_LON_180) {
			minimum -= MAX_LON_360;
			maximum -= MAX_LON_360;
		}
		// else both the bounds are less than 180 and dont need to be changed.
		// There is an assumption here that minimum is actually less than
		// maximum.

		return new float[] { minimum, maximum };
	}

	private Float[][] calcmagnitude(Float[][] u, Float[][] v){
		Float[][] retVal = new Float[u.length][u[0].length];

		System.out.println("Calculating new matrix " + u.length + "," + u[0].length);
		
		for(int countX=0; countX<u.length; countX++)
			for(int countY=0; countY<u[0].length; countY++)
			{
				try{
					retVal[countX][countY] = (float) Math.sqrt((u[countX][countY]*u[countX][countY])+(v[countX][countY]*v[countX][countY]));
				}catch(Exception ex){
					retVal[countX][countY] = Float.NaN;
				}
			}
		
		return retVal;
	}
	
	/**
	 * For the parameter and dimensions specified in paramInfo, look for match in the file data. If we have a match, process. If not throw an
	 * Exception.
	 */
	public synchronized GrdDataEncapsulator parseFiles(ParamInformation paramInfo) {
		GrdDataEncapsulator data = new GrdDataEncapsulator(paramInfo);

		// if no parameter name is provided, we will not be able to do the rest of the file work here.
		// do not throw an exception because there are situations where we need the NetCDFReader's read to succeed
		// even if there is no parameter name (for instance, when user creates a store using GeoServer admin ui).
		// no parameter name has been specified at that time. it only happens when user creates a layer.
		String parameterName = paramInfo.getParameter();
		if (parameterName == null || parameterName == "") {
			return data;
		}

		if (parameterName.compareTo("adcircMagnitude")==0){
			
			System.out.println("Entering adcircMagnitude trap");
			
			GrdDataEncapsulator udata = new GrdDataEncapsulator(paramInfo);
			GrdDataEncapsulator vdata = new GrdDataEncapsulator(paramInfo);
			NetcdfFile ncFile = null;
			try {
				ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);

				// check for the requested parameter in the file.
				// if parameter not found, go ahead and throw Exception.
				// will not find out about any dimension problems. can't see if param has runtime dim if no param.
				Variable uparameterVariable = ncFile.findVariable("u-vel");
				if (uparameterVariable == null) {
					String msg = "Requested parameter " + "u-vel" + " not found in NetCDF file "
							+ ncFile.getTitle();
					throw new InvalidParameterValueException(msg, "parameter", paramInfo.getParameter());
				}

				// check for requested (dimension) variables in the file. we are prepared for time, elevation, reference time.
				// do not throw Exception until we have reviewed each.
				// TODO do we need to confirm that time and elev are actually dims of the parameter here?
				// existing code assumes if they are vars in file, they are dims of the param.
				// then later it finds their dim posn on the parameter variable.
				// update: yes, we need to watch out for this!
				// for example, i just fixed an existing problem by adding check in getElevationIndexInNCFile to see if parameter has elevation dimension.

				// check elevation.
				DimensionInfo udimensionInfo = new DimensionInfo();
				udimensionInfo.setElevationIndex(getElevationIndexInNCFile(ncFile, uparameterVariable,
						paramInfo.getElevation()));

				// check time and reference time together, as they can be intertwined.
				// assume they can be null, so set the default here, as it depends on the nc/ncml file.

				// could move this inside the next step, keeping it out here means we repeat a few things.
				// Jared working on some new default logic, so keeping default logic separate where possible.
				// also, the next step is confusing enough without adding time default to the mix.
				setDefaultTimeIfNoneRequested(paramInfo, uparameterVariable, ncFile);

				// look for the correct indexes for both.
				// if value of either is not found(nf), we will not process the request further and will throw Exception.
				// ok for the runtime index to be null, as long as the time index is found.
				// once we have identified one of the two to be nf, no need to process any further.
				getRuntimeAndTimeIndexes(udimensionInfo, ncFile, paramInfo);

				// review the dimension results and throw Exception if needed.
				reviewDimensionsAndThrowExceptionIfNeeded(udimensionInfo, paramInfo);

				// if we made it this far, go dog go!
				parseFile(ncFile, "u-vel", udimensionInfo, udata);
				
				// check for the requested parameter in the file.
				// if parameter not found, go ahead and throw Exception.
				// will not find out about any dimension problems. can't see if param has runtime dim if no param.
				Variable vparameterVariable = ncFile.findVariable("v-vel");
				if (vparameterVariable == null) {
					String msg = "Requested parameter " + "v-vel" + " not found in NetCDF file "
							+ ncFile.getTitle();
					throw new InvalidParameterValueException(msg, "parameter", paramInfo.getParameter());
				}

				// check for requested (dimension) variables in the file. we are prepared for time, elevation, reference time.
				// do not throw Exception until we have reviewed each.
				// TODO do we need to confirm that time and elev are actually dims of the parameter here?
				// existing code assumes if they are vars in file, they are dims of the param.
				// then later it finds their dim posn on the parameter variable.
				// update: yes, we need to watch out for this!
				// for example, i just fixed an existing problem by adding check in getElevationIndexInNCFile to see if parameter has elevation dimension.

				// check elevation.
				DimensionInfo vdimensionInfo = new DimensionInfo();
				vdimensionInfo.setElevationIndex(getElevationIndexInNCFile(ncFile, vparameterVariable,
						paramInfo.getElevation()));

				// check time and reference time together, as they can be intertwined.
				// assume they can be null, so set the default here, as it depends on the nc/ncml file.

				// could move this inside the next step, keeping it out here means we repeat a few things.
				// Jared working on some new default logic, so keeping default logic separate where possible.
				// also, the next step is confusing enough without adding time default to the mix.
				setDefaultTimeIfNoneRequested(paramInfo, vparameterVariable, ncFile);

				// look for the correct indexes for both.
				// if value of either is not found(nf), we will not process the request further and will throw Exception.
				// ok for the runtime index to be null, as long as the time index is found.
				// once we have identified one of the two to be nf, no need to process any further.
				getRuntimeAndTimeIndexes(vdimensionInfo, ncFile, paramInfo);

				// review the dimension results and throw Exception if needed.
				reviewDimensionsAndThrowExceptionIfNeeded(vdimensionInfo, paramInfo);

				// if we made it this far, go dog go!
				parseFile(ncFile, "v-vel", vdimensionInfo, vdata);		
				
				Float[][] umatrix = udata.getImageArray();
				Float[][] vmatrix = vdata.getImageArray();

				Float[][] retMatrix = calcmagnitude(umatrix, vmatrix);
				
				data.setImageArray(retMatrix);
				
			} catch (InvalidParameterValueException e) {
				throw e;
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Error occurred during parseFiles.", e);
			} finally {
				closeNetCdfFile(ncFile);
			}			 
		} else {
			NetcdfFile ncFile = null;
			try {
				ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);

				// check for the requested parameter in the file.
				// if parameter not found, go ahead and throw Exception.
				// will not find out about any dimension problems. can't see if param has runtime dim if no param.
				Variable parameterVariable = ncFile.findVariable(parameterName);
				if (parameterVariable == null) {
					String msg = "Requested parameter " + parameterName + " not found in NetCDF file "
							+ ncFile.getTitle();
					throw new InvalidParameterValueException(msg, "parameter", paramInfo.getParameter());
				}

				// check for requested (dimension) variables in the file. we are prepared for time, elevation, reference time.
				// do not throw Exception until we have reviewed each.
				// TODO do we need to confirm that time and elev are actually dims of the parameter here?
				// existing code assumes if they are vars in file, they are dims of the param.
				// then later it finds their dim posn on the parameter variable.
				// update: yes, we need to watch out for this!
				// for example, i just fixed an existing problem by adding check in getElevationIndexInNCFile to see if parameter has elevation dimension.

				// check elevation.
				DimensionInfo dimensionInfo = new DimensionInfo();
				dimensionInfo.setElevationIndex(getElevationIndexInNCFile(ncFile, parameterVariable,
						paramInfo.getElevation()));

				// check time and reference time together, as they can be intertwined.
				// assume they can be null, so set the default here, as it depends on the nc/ncml file.

				// could move this inside the next step, keeping it out here means we repeat a few things.
				// Jared working on some new default logic, so keeping default logic separate where possible.
				// also, the next step is confusing enough without adding time default to the mix.
				setDefaultTimeIfNoneRequested(paramInfo, parameterVariable, ncFile);

				// look for the correct indexes for both.
				// if value of either is not found(nf), we will not process the request further and will throw Exception.
				// ok for the runtime index to be null, as long as the time index is found.
				// once we have identified one of the two to be nf, no need to process any further.
				getRuntimeAndTimeIndexes(dimensionInfo, ncFile, paramInfo);

				// review the dimension results and throw Exception if needed.
				reviewDimensionsAndThrowExceptionIfNeeded(dimensionInfo, paramInfo);

				// if we made it this far, go dog go!
				parseFile(ncFile, parameterName, dimensionInfo, data);

			} catch (InvalidParameterValueException e) {
				throw e;
			} catch (Exception e) {
				LOG.log(Level.SEVERE, "Error occurred during parseFiles.", e);
			} finally {
				closeNetCdfFile(ncFile);
			}}
		return data;
	}

	/**
	 * If any of the variable's expected dimensions have been looked for and not found, we can not read file correctly so throw Exception. This method
	 * does not do the looking, it just reviews the results and throws the Exception.
	 * 
	 * @param dimensionInfo use to decide if need to throw Exception
	 * @param paramInfo use for Exception details
	 * 
	 *        protected instead of private only for unit test.
	 */
	protected void reviewDimensionsAndThrowExceptionIfNeeded(DimensionInfo dimensionInfo,
			ParamInformation paramInfo) {
		boolean throwException = false;
		String exceptionMsgPreamble = ServiceException.INVALID_DIMENSION_VALUE + ":";
		StringBuffer exceptionMsgBody = new StringBuffer();
		StringBuffer exceptionParameterName = new StringBuffer();
		StringBuffer exceptionParameterValue = new StringBuffer();

		if (dimensionInfo.getElevationIndex() != null
				&& dimensionInfo.getElevationIndex() == NetCdfUtil.NOT_FOUND) {
			throwException = true;
			String parameterName = "elevation";
			String parameterValue = paramInfo.getElevation().toString();
			addToException(exceptionMsgBody, exceptionParameterName, exceptionParameterValue,
					parameterName, parameterValue);
		}

		if (dimensionInfo.getRuntimeIndex() != null
				&& dimensionInfo.getRuntimeIndex() == NetCdfUtil.NOT_FOUND) {
			throwException = true;
			String parameterName = "reference time";
			String parameterValue = NetCdfDateFormatUtil.getDateFormat3().format(
					paramInfo.getReferenceTime());
			addToException(exceptionMsgBody, exceptionParameterName, exceptionParameterValue,
					parameterName, parameterValue);
		}

		if (dimensionInfo.getTimeIndex() != null
				&& dimensionInfo.getTimeIndex() == NetCdfUtil.NOT_FOUND) {
			throwException = true;
			String parameterName = "time";
			String parameterValue = NetCdfDateFormatUtil.getDateFormat3().format(
					paramInfo.getTime());
			addToException(exceptionMsgBody, exceptionParameterName, exceptionParameterValue,
					parameterName, parameterValue);
		}

		if (throwException) {
			throw new InvalidParameterValueException(exceptionMsgPreamble
					+ exceptionMsgBody.toString(), exceptionParameterName.toString(),
					exceptionParameterValue.toString());
		}
	}

	private void addToException(StringBuffer exceptionMsgBody, StringBuffer exceptionParameterName,
			StringBuffer exceptionParameterValue, String variableName, String variableValue) {

		exceptionMsgBody.append(" Requested " + variableName + " (" + variableValue
				+ ") not available.");
		if (exceptionParameterName.length() != 0) {
			exceptionParameterName.append(", ");
		}
		exceptionParameterName.append(variableName);
		if (exceptionParameterValue.length() != 0) {
			exceptionParameterValue.append(", ");
		}
		exceptionParameterValue.append(variableValue);
	}

	/**
	 * if no time specified in the parameter, set the default to the latest time.
	 * <ul>
	 * <li>if no reference time requested, we will simply look for the latest time.</li>
	 * <li>if reference time was requested and the file parameter has a runtime dimension, we need to find the latest time only in the that runtime
	 * dimension.</li>
	 * </ul>
	 */
	private void setDefaultTimeIfNoneRequested(ParamInformation paramInfo,
			Variable parameterVariable, NetcdfFile ncFile) {
		if (paramInfo.getTime() == null) {
			boolean parameterHasRuntimeDimension = false;
			Variable runtimeVariable = NetCdfUtil.getFileVariableByName(ncFile,
					this.runtimeVariableNameInFile, NetCdfUtil.RUNTIME_VARIABLE_NAMES);

			if (runtimeVariable != null) {
				int runtimeIndex = parameterVariable.findDimensionIndex(runtimeVariable.getName());
				if (runtimeIndex > -1) {
					parameterHasRuntimeDimension = true;
				}
			}

			if (paramInfo.getReferenceTime() != null && parameterHasRuntimeDimension) {
				// special case - limit to the requested runtime dimension.
				int runtimeIndex = getRuntimeDimensionIndexValue(paramInfo, ncFile);
				if (runtimeIndex == NetCdfUtil.NOT_FOUND) {
					LOG.warning("Could not get default time. Runtime dimension for reference time "
							+ paramInfo.getReferenceTime() + " not found in NetCDF file.");
					return;
				}

				Variable timeVariable = getTimeVariable(ncFile);
				if (timeVariable == null) {
					LOG.warning("Could not get default time. Time variable not found in NetCDF file.");
				}

				Attribute timeUnitsAttribute = NetCdfUtil.getVariableAttributeByName(timeVariable,
						NetCdfUtil.TIME_UNIT_ATTRIBUTE_NAMES);
				if (timeUnitsAttribute == null) {
					LOG.info("Could not get default time. Time variable unit attribute not found in NetCDF file.");
					return;
				}

				try {
					// TODO improve with read targetted to the index.
					Double timeAsNumberOfHours = findHighestValueSecondDim(timeVariable.read(),
							runtimeIndex);
					Date defaultTime = getNumberOfHoursAsDate(timeAsNumberOfHours,
							timeUnitsAttribute);
					paramInfo.setTime(defaultTime);
				} catch (Exception e) {
					LOG.warning("Could not get default time.");
					LOG.log(Level.SEVERE, e.getMessage(), e);
					return;
				}
			} else {
				// simple case
				String timeAsString = getTimeMaximum();
				Date defaultTime = NetCdfUtil.getDateFromOutputStyleString(timeAsString);
				paramInfo.setTime(defaultTime);
			}
		}
	}

	private void getRuntimeAndTimeIndexes(DimensionInfo dimensionInfo, NetcdfFile ncFile,
			ParamInformation paramInfo) {
		if (paramInfo.getReferenceTime() == null) {
			getRuntimeAndTimeInfo_ReferenceTimeNotRequested(dimensionInfo, ncFile, paramInfo);
		} else if (hasRuntimeVariable(ncFile)) {
			getRuntimeAndTimeInfo_ReferenceTimeRequested_HaveRuntimeDim(dimensionInfo, ncFile,
					paramInfo);
		} else {
			getRuntimeAndTimeInfo_ReferenceTimeRequested_NoRuntimeDim(dimensionInfo, ncFile,
					paramInfo);
		}

		return;
	}

	// if no reference time requested, get the time (and possibly the runtime dim).
	// we will look at all times for the match.
	// 1. if we do not have a time match, return time dim not found. (rt:null, t:nf)
	// 2. if we have a time match, return the time match dim.
	// - 2a. if we have a runtime dim, also return the runtime dim for the time match. (rt:#, t:#)
	// - 2b. if we do not have a runtime dim, return the runtime dim null. (rt:null, t:#)
	//
	/**
	 * get the runtime index and time index.
	 */
	private void getRuntimeAndTimeInfo_ReferenceTimeNotRequested(DimensionInfo dimensionInfo,
			NetcdfFile ncFile, ParamInformation paramInfo) {
		// get time.
		Date time = paramInfo.getTime();

		if (time == null) {
			LOG.info("Time parameter not provided.");
			dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
			return;
		}

		Variable timeVariable = getTimeVariable(ncFile);
		if (timeVariable == null) {
			LOG.info("Time variable not found in NetCDF file.");
			dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
			return;
		}

		Attribute timeUnitsAttribute = NetCdfUtil.getVariableAttributeByName(timeVariable,
				NetCdfUtil.TIME_UNIT_ATTRIBUTE_NAMES);
		if (timeUnitsAttribute == null) {
			LOG.info("Time variable unit attribute not found in NetCDF file.");
			dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
			return;
		}

		double numHours = getTimeAsNumberOfHours(time, timeUnitsAttribute);

		try {
			if (hasRuntimeVariable(ncFile)) {
				Integer[] indexes = findIndexSecondDim(timeVariable.read(), numHours, null);
				dimensionInfo.setRuntimeIndex(indexes[0]);
				dimensionInfo.setTimeIndex(indexes[1]);
			} else {
				dimensionInfo.setRuntimeIndex(null);
				dimensionInfo.setTimeIndex(findIndex(timeVariable.read(), numHours));
			}

			if (dimensionInfo.getTimeIndex() == NetCdfUtil.NOT_FOUND) {
				LOG.log(Level.INFO, "Time match not found in NetCDF file {0} {1}", new Object[] {
						time.toString(), ncFile.getTitle() });
			}
			return;

		} catch (Exception e) {
			LOG.warning("COULD NOT GET DATE ");
			LOG.log(Level.SEVERE, e.getMessage(), e);
			dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
			return;
		}
	}

	// if reference time requested, and we have a runtime dim, look for runtime match.
	// 1. if we have a runtime match, look for time dim only within the runtime dim.
	// - 1a. if we have a time match, return time dim of match. (rt:#, t:#)
	// - 1b. if we do not have a time match, return time dim of not found. (rt:#, t:nf)
	// 2. if we do not have a runtime match, return runtime dim not found. (rt:nf, t:null)
	/**
	 * get the runtime index and the time index.
	 */
	private void getRuntimeAndTimeInfo_ReferenceTimeRequested_HaveRuntimeDim(
			DimensionInfo dimensionInfo, NetcdfFile ncFile, ParamInformation paramInfo) {
		Date referenceTime = paramInfo.getReferenceTime();

		if (referenceTime != null && hasRuntimeVariable(ncFile)) {
			int runtimeIndex = getRuntimeDimensionIndexValue(paramInfo, ncFile);

			if (runtimeIndex == NetCdfUtil.NOT_FOUND) {
				dimensionInfo.setRuntimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			// ok, now look for time match.
			Date time = paramInfo.getTime();

			if (time == null) {
				LOG.info("Time parameter not provided.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			Variable timeVariable = getTimeVariable(ncFile);
			if (timeVariable == null) {
				LOG.info("Time variable not found in NetCDF file.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			Attribute timeUnitsAttribute = NetCdfUtil.getVariableAttributeByName(timeVariable,
					NetCdfUtil.TIME_UNIT_ATTRIBUTE_NAMES);
			if (timeUnitsAttribute == null) {
				LOG.info("Time variable unit attribute not found in NetCDF file.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			double numHours = getTimeAsNumberOfHours(time, timeUnitsAttribute);

			// only look in the found runtime dimension.
			try {
				Integer[] var = findIndexSecondDim(timeVariable.read(), numHours, runtimeIndex);
				dimensionInfo.setRuntimeIndex(runtimeIndex);
				dimensionInfo.setTimeIndex(var[1]);

				if (dimensionInfo.getTimeIndex() == NetCdfUtil.NOT_FOUND) {
					LOG.log(Level.INFO, "Time match not found in NetCDF file {0} {1}",
							new Object[] { time.toString(), ncFile.getTitle() });
				}
				return;

			} catch (Exception e) {
				LOG.warning("COULD NOT GET DATE ");
				LOG.log(Level.SEVERE, e.getMessage(), e);
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}
		}

		return;
	}

	// if reference time requested, and we do not have a runtime dim,
	// look at other file attributes for runtime match.
	// we assume if there is no runtime dim, that all data in file is for a single runtime.
	// 1. if we have an reference time attribute match, look for time dim match.
	// - 1a. if we have a time match, return time dim of match. (rt:null, t:#)
	// - 1b. if we do not have a time match, return time dim of not found. (rt:null, t:nf)
	// 2. if we do not have a runtime match, return not found. (rt:nf, t:null)
	// Note: in this case the runtime index is not really an index, it refers to whether found in file attributes.
	// TODO Clarify the difference by add another property to the DimensionInfo object?
	/**
	 * get the runtime index and time index.
	 */
	private void getRuntimeAndTimeInfo_ReferenceTimeRequested_NoRuntimeDim(
			DimensionInfo dimensionInfo, NetcdfFile ncFile, ParamInformation paramInfo) {
		Date referenceTime = paramInfo.getReferenceTime();

		Date referenceTimeInFile = getReferenceTimeFromTauVariable(ncFile);

		if (referenceTimeInFile == null) {
			referenceTimeInFile = getReferenceTimeFromGlobalAttribute(ncFile);
		}

		// if we have reference time match in file, look for time match.
		if (referenceTime != null && referenceTimeInFile != null
				&& referenceTime.compareTo(referenceTimeInFile) == 0) {
			Date time = paramInfo.getTime();

			if (time == null) {
				LOG.info("Time parameter not provided.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			Variable timeVariable = getTimeVariable(ncFile);
			if (timeVariable == null) {
				LOG.info("Time variable not found in NetCDF file.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			Attribute timeUnitsAttribute = NetCdfUtil.getVariableAttributeByName(timeVariable,
					NetCdfUtil.TIME_UNIT_ATTRIBUTE_NAMES);
			if (timeUnitsAttribute == null) {
				LOG.info("Time variable unit attribute not found in NetCDF file.");
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}

			double numHours = getTimeAsNumberOfHours(time, timeUnitsAttribute);

			try {
				dimensionInfo.setRuntimeIndex(null);
				dimensionInfo.setTimeIndex(findIndex(timeVariable.read(), numHours));

				if (dimensionInfo.getTimeIndex() == NetCdfUtil.NOT_FOUND) {
					LOG.log(Level.INFO, "Time match not found in NetCDF file {0} {1}",
							new Object[] { time.toString(), ncFile.getTitle() });
				}
				return;

			} catch (Exception e) {
				LOG.warning("COULD NOT GET DATE ");
				LOG.log(Level.SEVERE, e.getMessage(), e);
				dimensionInfo.setTimeIndex(NetCdfUtil.NOT_FOUND);
				return;
			}
		} else {
			dimensionInfo.setRuntimeIndex(NetCdfUtil.NOT_FOUND);
		}

		return;
	}

	private double getTimeAsNumberOfHours(Date time, Attribute timeUnitsAttribute) {
		Date startTime = getDateFromString(timeUnitsAttribute.getStringValue());
		DateTime requestedDateTime = new DateTime(time.getTime());
		DateTime timeOrigin = new DateTime(startTime.getTime());
		Hours hoursBetween = Hours.hoursBetween(timeOrigin, requestedDateTime);
		return hoursBetween.getHours();
	}

	/**
	 * the inverse of getTimeAsNumberOfHours above
	 * 
	 * @param numberOfHours currently no null protection
	 * @param timeUnitsAttribute currently no null protection
	 * @return
	 */
	private Date getNumberOfHoursAsDate(Double numberOfHours, Attribute timeUnitsAttribute) {
		Date startTime = getDateFromString(timeUnitsAttribute.getStringValue());
		DateTime timeOrigin = new DateTime(startTime.getTime());
		DateTime timeNew = timeOrigin.plusHours(numberOfHours.intValue());
		return new Date(timeNew.getMillis());
	}

	private Variable getElevationVariable(NetcdfFile ncfile) {
		return NetCdfUtil.getFileVariableByName(ncfile, this.elevationVariableNameInFile,
				NetCdfUtil.ELEVATION_VARIABLE_NAMES);
	}

	private Variable getTimeVariable(NetcdfFile ncFile) {
		return NetCdfUtil.getFileVariableByName(ncFile, this.timeVariableNameInFile,
				NetCdfUtil.TIME_VARIABLE_NAMES);
	}

	private int getRuntimeDimensionIndexValue(ParamInformation paramInfo, NetcdfFile ncFile) {
		return getVariableTargetValueIndex(ncFile, this.runtimeVariableNameInFile,
				NetCdfUtil.RUNTIME_VARIABLE_NAMES,
				NetCdfDateFormatUtil.getDateFormat3().format(paramInfo.getReferenceTime()));
	}

	private boolean hasRuntimeVariable(NetcdfFile ncFile) {
		return NetCdfUtil.getFileVariableByName(ncFile, this.runtimeVariableNameInFile,
				NetCdfUtil.RUNTIME_VARIABLE_NAMES) == null ? false : true;
	}

	/**
	 * Get the index for the elevation dimension.
	 * 
	 * assumes the elevations in the NetCDF file can be read as doubles.
	 * 
	 * @param ncfile
	 * @param parameterVariable expect non-null
	 * @param targetElevation expect Double or String
	 * @return Integer index of elevation dimension.
	 * 
	 *         protected (instead of private) only to put under test.
	 */
	protected Integer getElevationIndexInNCFile(NetcdfFile ncfile, Variable parameterVariable,
			Object targetElevation) {
		Variable elevationVariable = getElevationVariable(ncfile);

		// if no elevation dimension in file, return null. elevation dimension is not required.
		if (elevationVariable == null) {
			LOG.log(Level.INFO, "File {0} does not have a recognized elevation variable.",
					ncfile.getTitle());
			return null;
		}

		// if the parameter variable does not use the file's elevation dimension, return null.
		// elevation dimension not required to be used by each parameter in the file.
		if (parameterVariable.findDimensionIndex(elevationVariable.getName()) == NetCdfUtil.NOT_FOUND) {
			return null;
		}

		// if the elevation dimension in file has only one element, return it.
		// wait, not so fast, if there is a target elevation, make sure it is a match.
		// accept the file elevation value or the special case elevation value.
		if (elevationVariable.getSize() == 1) {
			if (targetElevation == null) {
				return 0;
			}

			Set<Object> elevationsInFile = getElevationsInFile(elevationVariable);
			Set<Object> elevationsSpecialCase = getElevationsForSpecialCases(elevationVariable,
					elevationsInFile);

			// impl note: separate branches since these are TreeSets. if use unordered Collection in future, can simplify this.
			if (targetElevation instanceof Double) {
				if (elevationsInFile != null && elevationsInFile.contains(targetElevation)) {
					return 0;
				} else {
					return NetCdfUtil.NOT_FOUND;
				}
			} else if (elevationsSpecialCase != null
					&& elevationsSpecialCase.contains(targetElevation)) {
				return 0;
			} else {
				return NetCdfUtil.NOT_FOUND;
			}
		}

		// if the elevation dimension in file has more than one element, use
		// the target elevation to determine which index to return.
		// we know what to do if the target elevation is a Double.
		// we assume it is a mistake to specify an target elevation of non-Double for a
		// parameter with elevation dimension of more than one element.
		if (elevationVariable.getSize() > 1) {
			if (targetElevation instanceof Double) {
				try {
					int i = findIndex(elevationVariable.read(), (Double) targetElevation);
					if (i == NetCdfUtil.NOT_FOUND) {
						LOG.log(Level.WARNING,
								"Could not find index for elevation {0} in file ({1}).",
								new Object[] { targetElevation.toString(), ncfile.getTitle() });
					}
					return i;
				} catch (IOException e) {
					LOG.log(Level.WARNING,
							"Could not find index for elevation {0} in file ({1}). Exception:{2}",
							new Object[] { targetElevation.toString(), ncfile.getTitle(),
							e.getMessage() });
					return NetCdfUtil.NOT_FOUND;
				}
			} else if (targetElevation == null) {
				// we have already looked in the parameter for the elevation value and for the default elevation value.
				// if it is still null, we can return one of the elevations or return NOT FOUND. for now, return the first elevation.
				LOG.log(Level.WARNING,
						"Null elevation requested of a file ({1}) that has more than one elevation.  Using first elevation.",
						new Object[] {
						(targetElevation != null) ? targetElevation.toString()
								: targetElevation, ncfile.getTitle() });
				return 0;
			} else {
				LOG.log(Level.WARNING,
						"Non Double elevation ({0}) requested of a file ({1}) that has more than one elevation.",
						new Object[] {
						(targetElevation != null) ? targetElevation.toString()
								: targetElevation, ncfile.getTitle() });
				return NetCdfUtil.NOT_FOUND;
			}
		}

		// unexpected case. log and return null.
		LOG.log(Level.WARNING, "Unexpected case for elevation ({0}) requested of file ({1}).",
				new Object[] {
				(targetElevation != null) ? targetElevation.toString() : targetElevation,
						ncfile.getTitle() });
		return null;
	}

	/**
	 * assumes elevations in the NetCDF files can be read as doubles.
	 */
	public SortedSet<Object> getElevations() {
		/*
		 * TODO: a lot of code duplication here between getTimeString and getElevation string but I'm in a hurry, try to consolidate some common logic
		 * in the future. Also a lot of duplication in TimeLayerInNCFile and ElevationLayerInNCFile
		 */
		SortedSet<Object> result = new TreeSet<Object>();
		NetcdfFile ncFile = null;
		try {
			ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);
			Variable elevationVariable = getElevationVariable(ncFile);
			SortedSet<Object> elevationsInFile = getElevationsInFile(elevationVariable);

			// in some special cases, we may return elevations in a different form than the
			// actual elevations in the NetCDF file.
			SortedSet<Object> specialCaseElevations = getElevationsForSpecialCases(
					elevationVariable, elevationsInFile);

			if (specialCaseElevations != null && specialCaseElevations.size() > 0) {
				result = specialCaseElevations;
			} else {
				result = elevationsInFile;
			}

		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred during getElevations.", e);
			return null;
		} finally {
			closeNetCdfFile(ncFile);
		}

		return result;
	}

	private SortedSet<Object> getElevationsInFile(Variable elevationVariable) {
		SortedSet<Object> result = new TreeSet<Object>();

		if (elevationVariable == null) {
			return null;
		}

		try {
			IndexIterator ii = elevationVariable.read().getIndexIterator();
			while (ii.hasNext()) {
				double elevation = ii.getDoubleNext();
				result.add(elevation);
			}
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred during getElevationsInFile.", e);
			return null;
		}

		return result;
	}

	/**
	 * protected instead of private only for unit test.
	 */
	protected SortedSet<Object> getElevationsForSpecialCases(Variable elevationVariable,
			Collection<Object> elevations) {
		SortedSet<Object> result = new TreeSet<Object>();

		if (elevationVariable == null) {
			return null;
		}

		// special case - computed value surface
		// if the elevation dimension is either depth or height and
		// there is only one elevation and it is 0.0, add surface.
		String shortName = elevationVariable.getShortName();
		if (NetCdfUtil.ELEVATION_VARIABLE_NAMES_MAY_BE_SURFACE.contains(shortName)
				&& elevations != null && elevations.size() == 1) {
			Object elevation = elevations.iterator().next();
			if (elevation instanceof Number
					&& Math.abs((Double) elevation - 0.0) < NetCdfUtil.FLOATING_POINT_EPSILON) {
				result.add(NetCdfUtil.ELEVATION_SURFACE);
			}
		}

		// special case 2 - computed value tropopause
		// have not seen this in a NetCDF file yet.

		return result;
	}

	/**
	 * Gets the first elevation dimension value.
	 * 
	 * assumes elevations in the NetCDF files can be read as doubles.
	 * 
	 * @return Object return null if file has no elevation dimension or if elevation dimension has no values.
	 */
	public Object getFirstElevation() {
		SortedSet<Object> elevations = getElevations();
		if (elevations != null && elevations.size() > 0) {
			return elevations.first();
		} else {
			return null;
		}
	}

	public String getElevationString() {
		return NetCdfUtil.getDomainListAsString(getElevations());
	}

	public String getReferenceTimeString() {
		String result = null;
		NetcdfFile ncFile = null;
		try {
			ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);
			List<Date> referenceTimes = getReferenceTimesInNcFile(ncFile);
			if (referenceTimes != null) {
				List<String> referenceTimesAsStrings = new ArrayList<String>();
				for (Date referenceTime : referenceTimes) {
					referenceTimesAsStrings.add(NetCdfDateFormatUtil.getDateFormat3().format(
							referenceTime));
				}
				result = NetCdfUtil.getDomainListAsString(referenceTimesAsStrings);
			}
		} catch (IOException e) {
			LOG.log(Level.WARNING,
					"Problem getting reference time(s) for " + file.getAbsolutePath(), e);
			result = null;
		} finally {
			closeNetCdfFile(ncFile);
		}

		return result;
	}

	/**
	 * <ul>
	 * <li>a. first check for usable runtime variable</li>
	 * <li>b. if not found, then check for usable tau variable time_origin attribute</li>
	 * <li>c. if not found, then check for global time_origin attribute</li>
	 * </ul>
	 */
	private List<Date> getReferenceTimesInNcFile(NetcdfFile ncFile) {
		List<Date> result = null;

		result = getReferenceTimesFromRuntimeVariable(ncFile);

		if (result == null) {
			Date fromTau = getReferenceTimeFromTauVariable(ncFile);
			if (fromTau != null) {
				result = new ArrayList<Date>();
				result.add(fromTau);
			}
		}

		if (result == null) {
			Date fromGlobal = getReferenceTimeFromGlobalAttribute(ncFile);
			if (fromGlobal != null) {
				result = new ArrayList<Date>();
				result.add(fromGlobal);
			}
		}

		return result;
	}

	private List<Date> getReferenceTimesFromRuntimeVariable(NetcdfFile ncFile) {
		List<Date> result = null;

		Variable runtimeVariable = NetCdfUtil.getFileVariableByName(ncFile,
				this.runtimeVariableNameInFile, NetCdfUtil.RUNTIME_VARIABLE_NAMES);
		if (runtimeVariable != null) {
			List<Object> runtimes = NetCdfUtil.getVariableCachedData(runtimeVariable);
			if (runtimes.size() > 0) {
				result = new ArrayList<Date>();
				for (Object runtime : runtimes) {
					result.add(NetCdfUtil.getDateFromOutputStyleString((String) runtime));

				}
			}

		}

		return result;
	}

	/**
	 * protected instead of private only for unit test.
	 */
	protected Date getReferenceTimeFromTauVariable(NetcdfFile ncFile) {
		Date result = null;

		Variable tauVariable = NetCdfUtil.getFileVariableByName(ncFile, this.tauVariableNameInFile,
				NetCdfUtil.TAU_VARIABLE_NAMES);
		if (tauVariable == null) {
			LOG.log(Level.INFO, "Tau variable not found for {0}", ncFile.getLocation());
		} else {
			Attribute timeOriginAttribute = NetCdfUtil.getVariableAttributeByName(tauVariable,
					NetCdfUtil.TIME_ORIGIN_ATTRIBUTE_NAMES);
			if (timeOriginAttribute == null) {
				LOG.log(Level.INFO, "Tau variable''s time_origin attribute not found for {0}",
						ncFile.getLocation());
			} else {
				result = NetCdfUtil.getDateFromFileAttributeString(timeOriginAttribute
						.getStringValue());
			}
		}

		return result;
	}

	private Date getReferenceTimeFromGlobalAttribute(NetcdfFile ncFile) {
		Date result = null;

		Attribute globalTimeOriginAttribute = ncFile.findGlobalAttribute("time_origin");
		if (globalTimeOriginAttribute == null) {
			LOG.log(Level.INFO, "Global time_origin attribute not found for {0}",
					ncFile.getLocation());
		} else {
			result = NetCdfUtil.getDateFromFileAttributeString(globalTimeOriginAttribute
					.getStringValue());
		}

		return result;
	}

	/**
	 * get the desired index of variable based on the String target value.
	 * 
	 * @return index (return NetCdfUtil.NOT_FOUND if not found)
	 */
	private int getVariableTargetValueIndex(NetcdfFile ncFile, String preferredVariableName,
			Collection<String> defaultVariableNames, String targetValue) {

		String variableNameForLog = (preferredVariableName == null) ? defaultVariableNames
				.toString() : preferredVariableName;
				String fileDescriptionForLog = ncFile.getTitle();

				if (targetValue == null) {
					LOG.log(Level.INFO, "{0} target value was not provided.", variableNameForLog);
					return NetCdfUtil.NOT_FOUND;
				}

				Variable variable = NetCdfUtil.getFileVariableByName(ncFile, preferredVariableName,
						defaultVariableNames);

				if (variable == null) {
					LOG.log(Level.INFO, "File does not have recognized {0} variable. {1}", new Object[] {
							variableNameForLog, fileDescriptionForLog });
					return NetCdfUtil.NOT_FOUND;
				}

				int index = NetCdfUtil.getIndexOfMatchInVariableCachedData(variable, targetValue);

				if (index == NetCdfUtil.NOT_FOUND) {
					LOG.log(Level.INFO, "No match for {0} targetValue {1} in file. {2}", new Object[] {
							variableNameForLog, targetValue, fileDescriptionForLog });
				}

				return index;
	}

	/**
	 * Get a list of the time values from the file.
	 * 
	 * @return SortedSet<String>
	 */
	public SortedSet<String> getTimes() {
		SortedSet<String> timeStrings = new TreeSet<String>();
		NetcdfFile ncFile = null;
		try {
			ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);

			Variable timeVariable = getTimeVariable(ncFile);
			if (timeVariable == null) {
				LOG.info("Time variable not found in NetCDF file.");
				return null;
			}

			Attribute timeUnitsAttribute = NetCdfUtil.getVariableAttributeByName(timeVariable,
					NetCdfUtil.TIME_UNIT_ATTRIBUTE_NAMES);
			if (timeUnitsAttribute == null) {
				LOG.info("Time variable unit attribute not found in NetCDF file.");
			} else {
				Date startTime = getDateFromString(timeUnitsAttribute.getStringValue());

				Array timeArray = timeVariable.read();
				IndexIterator ii = timeArray.getIndexIterator();
				while (ii.hasNext()) {
					double time = ii.getDoubleNext();
					/*
					 * The value here is hours since 2000-01-01 00:00:00, which is stored in startTime, we will use some joda magic to add this number
					 * of hours and see what date we get
					 */
					org.joda.time.DateTime startDateTime = new org.joda.time.DateTime(
							startTime.getTime());
					org.joda.time.DateTime actualTime = startDateTime.plusHours((int) Math
							.round(time));
					timeStrings.add(NetCdfDateFormatUtil.getDateFormat3().format(
							actualTime.toDate()));
				}
			}
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred during getTimes.", e);
			return null;
		} finally {
			closeNetCdfFile(ncFile);
		}

		return timeStrings;
	}

	/**
	 * This function is designed to get a geoserver formatted time string (some times separated by commas, for now, this will probably change in the
	 * future) for the getCapabilities information
	 */
	public String getTimeString() {
		return NetCdfUtil.getDomainListAsString(getTimes());
	}

	/**
	 * Get the first time in the file.
	 * 
	 * @return String
	 */
	public String getTimeMinimum() {
		SortedSet<String> times = getTimes();

		if (null != times && !times.isEmpty()) {
			return times.first();
		}

		return null;
	}

	/**
	 * Get the last time in the file.
	 * 
	 * @return String
	 */
	public String getTimeMaximum() {
		SortedSet<String> times = getTimes();

		if (null != times && !times.isEmpty()) {
			return times.last();
		}

		return null;
	}

	/**
	 * we have a NetCdfUtil method that is the same as this method except for return if not able to parse the date. This method returns epoch date,
	 * NetCdfUtil method returns null. TODO: evaluate combining these two methods?
	 */
	private Date getDateFromString(String input) {
		/*
		 * TODO: In my experience NAVO netcdf time values are always "hour since" *some time*, but we need to confirm this with a subject matter
		 * expert on navo data
		 */
		String dateString = NetCdfUtil.getDateStringFromExpectedFileAttributeString(input);

		// Note: order is important!
		List<SimpleDateFormat> netCdfFormats = Arrays.asList(NetCdfDateFormatUtil.getDateFormat1(),
				NetCdfDateFormatUtil.getDateFormat2());

		for (SimpleDateFormat netCdfFormat : netCdfFormats) {
			try {
				return netCdfFormat.parse(dateString);
			} catch (ParseException e) {
				if (netCdfFormat.equals(NetCdfDateFormatUtil.getDateFormat1())) {
					LOG.fine("Date not in format 1. Will try format 2.");
				} else {
					LOG.fine("Date not in format 2.  Set date to default.");
				}
			}
		}
		return new Date(0);
	}

	public SortedSet<String> getVariables() {
		SortedSet<String> variableStrings = new TreeSet<String>();
		NetcdfFile ncFile = null;

		try {
			ncFile = NetcdfDataset.openFile(file.getAbsolutePath(), null);

			for (Variable variable : ncFile.getVariables()) {
				variableStrings.add(variable.getShortName());
			}

		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Error occurred during getVariables.", e);
			return null;
		} finally {
			closeNetCdfFile(ncFile);
		}

		return variableStrings;
	}

	public String getVariablesString() {
		return NetCdfUtil.getDomainListAsString(getVariables());
	}

	/**
	 * limitation: works only for 1 dim array.
	 */
	private int findIndex(Array a, Double val) {
		IndexIterator ii = a.getIndexIterator();
		int index = -1;
		while (ii.hasNext()) {
			Double currentVal = ii.getDoubleNext();
			int currentPos = ii.getCurrentCounter()[0];
			if (currentVal.equals(val)) {
				index = currentPos;
				break;
			}
		}

		return index;
	}

	// cases to consider:
	// a. two dim array, first dim value specified, look only at that dim.
	// b. two dim array, first dim not specified, look through all values in
	// order. if more than one value match, choose latest one.
	// for a., we already know first array dim value.
	// for b. we are also interested in the first array dim value.
	/**
	 * limitation: works only for 2 dim arrays.
	 */
	private Integer[] findIndexSecondDim(Array a, Double val, Integer firstDim) {
		Integer[] result = new Integer[2];
		if (firstDim == null) {
			result[0] = NetCdfUtil.NOT_FOUND;
		} else {
			result[0] = firstDim;
		}
		result[1] = NetCdfUtil.NOT_FOUND;

		IndexIterator ii = a.getIndexIterator();
		while (ii.hasNext()) {
			Double currentVal = ii.getDoubleNext();
			int[] currentPos = ii.getCurrentCounter();
			if ((firstDim == null || currentPos[0] == firstDim) && (currentVal.equals(val))) {
				if (firstDim == null) {
					result[0] = currentPos[0];
				}
				result[1] = currentPos[1];
				// no break first time through, so can let later match overwrite earlier.
			}
		}

		return result;
	}

	// cases to consider:
	// a. two dim array, first dim value specified, look only at that dim for highest value in second dim.
	/**
	 * limitation: works only for 2 dim arrays.
	 */
	private Double findHighestValueSecondDim(Array a, Integer firstDim) {
		Double result = null;

		IndexIterator ii = a.getIndexIterator();
		while (ii.hasNext()) {
			Double currentVal = ii.getDoubleNext();
			int[] currentPos = ii.getCurrentCounter();
			if ((currentPos[0] == firstDim) && (result == null || currentVal > result)) {
				result = currentVal;
			}
		}

		return result;
	}

	public boolean validateNetCdfFile(String absoluteFilePath) {
		boolean valid = false;
		try {
			valid = NetcdfFile.canOpen(absoluteFilePath);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, e.getMessage(), e);
		}
		return valid;
	}

	private void parseFile(NetcdfFile ncFile, String parameterName, DimensionInfo dimensionInfo,
			GrdDataEncapsulator data) throws IOException, InvalidRangeException {

		// Find the requested Variable.
		Variable variable = ncFile.findVariable(parameterName);
		if (variable == null) {
			if (LOG.isLoggable(Level.SEVERE)) {
				LOG.log(Level.SEVERE, "Unable to parse NetCDF file [{0}] for parameter: {1}",
						new Object[] { ncFile.getTitle(), parameterName });
			}
			return;
		}

		// Get the list of longitude values indices to read from the file.
		List<Double> lonList = data.getDesiredLons();
		Array lonValuesFromNcFile = NetCdfUtil.getFileVariableByName(ncFile,
				NetCdfUtil.LON_VARIABLE_NAMES).read();
		Map<Integer, Integer> lonImagePositionAndIndexInFile = getTargetIndexes(lonList,
				lonValuesFromNcFile, true);

		// Get the list of latitude values indices to read from the file.
		List<Double> latList = data.getDesiredLats();
		Array latValuesFromNcFile = NetCdfUtil.getFileVariableByName(ncFile,
				NetCdfUtil.LAT_VARIABLE_NAMES).read();
		Map<Integer, Integer> latImagePositionAndIndexInFile = getTargetIndexes(latList,
				latValuesFromNcFile, false);

		// Ask the ReadStrategyFactory for the appropriate ReadStrategy.
		ReadStrategy readStrategy = ReadStrategyFactory.getReadStrategy(
				lonImagePositionAndIndexInFile, latImagePositionAndIndexInFile,
				lonValuesFromNcFile, latValuesFromNcFile);

		LOG.log(Level.INFO, "Using Read Strategy: {0}", readStrategy);

		// Set the appropriate dimension indices to read.
		readStrategy.setVariable(variable);
		if (null != dimensionInfo.getElevationIndex()) {
			readStrategy.setElevationIndex(dimensionInfo.getElevationIndex());
			readStrategy.setElevationVariableNameInFile(this.elevationVariableNameInFile);
		}
		if (null != dimensionInfo.getTimeIndex()) {
			readStrategy.setTimeIndex(dimensionInfo.getTimeIndex());
			readStrategy.setTimeVariableNameInFile(this.timeVariableNameInFile);

		}
		if (null != dimensionInfo.getRuntimeIndex()) {
			readStrategy.setRuntimeIndex(dimensionInfo.getRuntimeIndex());
			readStrategy.setRuntimeVariableNameInFile(this.runtimeVariableNameInFile);
		}

		// Actually read the data from the NetCDF file.
		readStrategy.read(lonImagePositionAndIndexInFile, latImagePositionAndIndexInFile, data);
	}

	private Map<Integer, Integer> getTargetIndexes(List<Double> targetValues, Array lookupValues,
			boolean isLongitude) {
		Map<Integer, Integer> desiredIndexes = new HashMap<Integer, Integer>();

		// Default indexing strategy to nearest neighbor on an ascending array.
		IndexingStrategy indexingStrategy = new NearestNeighborAscending();

		if (lookupValues.getDouble(0) > lookupValues.getDouble((int) lookupValues.getSize() - 1)) {
			// First value is greater than the last, assume the array is
			// sorted in descending order
			indexingStrategy = new NearestNeighborDescending();
		}

		for (int targetValueIndex = 0; targetValueIndex < targetValues.size(); targetValueIndex++) {
			Double targetValue = targetValues.get(targetValueIndex);
			// If the file uses a (0 to 360) world grid and the requested point
			// is in the western hemisphere, convert the longitude
			if (isLongitude && isLongitudeIn0to360() && targetValues.get(targetValueIndex) < 0) {
				targetValue += MAX_LON_360;
			}

			int lookupValueIndex = indexingStrategy.getCoordinateIndex(lookupValues, targetValue);

			if (lookupValueIndex != NetCdfUtil.NOT_FOUND) {
				desiredIndexes.put(targetValueIndex, lookupValueIndex);
			} else if (targetValue > (MAX_LON_360 - .5) && targetValue <= MAX_LON_360) {
				// Special case city right here. WAM files have 0 to 359 longitudes. When we try
				// to get the pixels between -.5 and 0 in a normal system, they return NOT_FOUND
				// since the closest value for the range (359.5, 360] is 360 and there is no 360
				// in the file. The closest value really is 0 since the world is sphericalish
				// and wraps around.

				// Some extra logic to make sure the NetCDF is wrapping around the
				// entire globe and starts and ends at the prime meridian
				// before adding the extra index.
				// This was causing a streak on the prime meridian for files
				// that crossed the IDL but did not cover the entire globe
				long size = lookupValues.getSize();
				double startValue = lookupValues.getDouble(0);
				double endValue = lookupValues.getDouble((int) (size - 1));
				double resolution = (endValue - startValue) / (size - 1);

				if (startValue == 0.0 && endValue + resolution - 360.0 == 0.0) {
					desiredIndexes.put(targetValueIndex, 0);
				}
			}
		}

		return desiredIndexes;
	}

	/**
	 * protected instead of private only so can unit test.
	 */
	protected String buildReadParameter(int dimensionSize, String[] range) {
		StringBuffer sb = new StringBuffer();
		for (int sbPos = 0; sbPos < dimensionSize; sbPos++) {
			sb.append(range[sbPos]);
			sb.append(", ");
		}

		return sb.substring(0, sb.length() - 2);
	}

	private float[] getMinAndMaxFromArray(Array a) {
		Float max = null;
		Float min = null;
		IndexIterator ii = a.getIndexIterator();
		while (ii.hasNext()) {
			float f = ii.getFloatNext();
			if (min == null || f < min) {
				min = f;
			}
			if (max == null || f > max) {
				max = f;
			}
		}

		return new float[] { min, max };
	}

	private void closeNetCdfFile(NetcdfFile file) {
		if (file != null) {
			try {
				file.close();
			} catch (IOException e) {
				LOG.log(Level.SEVERE, "Close NetCDF file: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Inner class to hold variable dimension information.
	 * 
	 * protected instead of private only for unit test.
	 */
	protected class DimensionInfo {
		/**
		 * this is the index within the elevation variable values of the elevation we want. this is NOT the dimension index of elevation within the
		 * parameter. the file may not contain an elevation variable.
		 */
		private Integer elevationIndex = null;

		/**
		 * this is the index within the runtime variable values of the elevation we want. this is NOT the dimension index of runtime within the
		 * parameter. the file may not contain a runtime variable. sometimes a single runtime is described in a single attribute of another variable
		 * or in a global attribute.
		 */
		private Integer runtimeIndex = null;

		/**
		 * this is the index within the time values of the time we want. this is NOT the dimension index of time within the parameter. the file is
		 * expected to contain time.
		 */
		private Integer timeIndex = null;

		public Integer getElevationIndex() {
			return elevationIndex;
		}

		public Integer getRuntimeIndex() {
			return runtimeIndex;
		}

		public Integer getTimeIndex() {
			return timeIndex;
		}

		public void setElevationIndex(Integer elevationIndex) {
			this.elevationIndex = elevationIndex;
		}

		public void setRuntimeIndex(Integer runtimeIndex) {
			this.runtimeIndex = runtimeIndex;
		}

		public void setTimeIndex(Integer timeIndex) {
			this.timeIndex = timeIndex;
		}
	}
}
