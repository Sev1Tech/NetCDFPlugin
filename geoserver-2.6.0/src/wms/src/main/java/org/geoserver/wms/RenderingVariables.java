/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.filter.function.EnvFunction;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.util.logging.Logging;

/**
 * Helper that injects enviroment variables in the {@link EnvFunction} given a map context
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class RenderingVariables {

    static final Logger LOGGER = Logging.getLogger(RenderingVariables.class);

	// Convert the date to a string that WMS can use.
	private static String convertDate(Date date){
		java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
		// explicitly set timezone of input if needed
		df.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
		String retVal = df.format(date);

		return retVal;
	}


    public static void setupEnvironmentVariables(WMSMapContent mapContent) {
        // setup some SLD variable substitution environment used by rendering transformations
        EnvFunction.setLocalValue("wms_bbox", mapContent.getRenderingArea());
        EnvFunction.setLocalValue("wms_crs", mapContent.getRenderingArea()
                .getCoordinateReferenceSystem());
        EnvFunction.setLocalValue("wms_srs", mapContent.getRequest().getSRS());
        EnvFunction.setLocalValue("wms_width", mapContent.getMapWidth());
        EnvFunction.setLocalValue("wms_height", mapContent.getMapHeight());

		// Adding the wms_time and wms_elevation variables to make available 
        // for WPS use.
        String elevationStr = "";
        List<Object> elevationVar = mapContent.getRequest().getElevation();
		if(elevationVar != null && elevationVar.size() == 1)
		{
			if(elevationVar.get(0) != null)
				elevationStr = elevationVar.get(0).toString();
		}
		EnvFunction.setLocalValue("wms_elevation", elevationStr);
		
		String timeStr = "";
        List<Object> timeVar = mapContent.getRequest().getTime();
		if(timeVar != null && timeVar.size() == 1)
		{
			if(timeVar.get(0) != null)
				timeStr = convertDate((Date)timeVar.get(0)).toString();
		}
		EnvFunction.setLocalValue("wms_time", timeStr);

        try {
            double scaleDenominator = RendererUtilities.calculateOGCScale(
                    mapContent.getRenderingArea(), mapContent.getMapWidth(), null);
            EnvFunction.setLocalValue("wms_scale_denominator", scaleDenominator);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Failed to compute the scale denominator, wms_scale_denominator env variable is unset",
                    e);
        }
    }
}
