package org.geoserver.wps.gs;

import java.awt.image.RenderedImage;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import mil.navy.fnmoc.gis.wps.util.meteogram.AreaGetCoverageRequest;
import net.opengis.wcs10.DescribeCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wcs.DefaultWebCoverageService100;
import org.geoserver.wcs.DefaultWebCoverageService111;
import org.geoserver.wcs.responses.CoverageResponseDelegateFinder;
import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.GeoTools;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.wcs.WCSConfiguration;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
//import org.opengis.geometry.primitive.Point;

@DescribeProcess(title="Magnatude Direction Process", description="Unknown at this time.")
public class MagnatudeDirectionProcess implements GSProcess {

	private static final GeoServer geoServer = GeoServerExtensions.bean(GeoServer.class);
	private static final String    namespace = "https://oceanography.navy.mil";

	/**
	 *
	 * @author pcoleman, altered by jcraft
	 */

	private static SimpleFeatureType createFeatureType() {

		SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
		builder.setName("magnatude_direction");
		builder.setNamespaceURI(namespace);
		builder.setCRS(DefaultGeographicCRS.WGS84); // Coordinate reference system

		// add attributes in order
		builder.add("geometry", Point.class);        
		builder.add("magnatude", Double.class); // 30 chars width for name field
		builder.add("direction", Double.class); // 30 chars width for name field
		builder.add("gsize", Integer.class);

		// build the type
		final SimpleFeatureType FEATURE_TYPE = builder.buildFeatureType();

		return FEATURE_TYPE;
	}

	private RandomIter getIter(RenderedImage renderedImage) {

		RandomIter iter = RandomIterFactory.create(renderedImage, null); 

		return iter;
	}

	public DescribeCoverageType getDescribeType(String source) throws Exception {

		WCSConfiguration configuration = new WCSConfiguration();
		WcsXmlReader reader = new WcsXmlReader("DescribeCoverage", "1.0.0", configuration);

		String request = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + // 
				"<wcs:DescribeCoverage service=\"WCS\" " + //
				"xmlns:ows=\"http://www.opengis.net/ows/1.1\"\r\n" + // 
				"  xmlns:wcs=\"http://www.opengis.net/wcs/1.0.0\"\r\n" + // 
				"  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \r\n" + // 
				"  xsi:schemaLocation=\"http://www.opengis.net/wcs/1.0.0 " + //
				"schemas/wcs/1.0.0/wcsAll.xsd\"\r\n" + //
				"  version=\"1.0.0\" >\r\n" + //
				"  <wcs:Identifier>" + source + "</wcs:Identifier>\r\n" + // 
				"</wcs:DescribeCoverage>";

		// smoke test, we only try out a very basic request
		DescribeCoverageType cap = (DescribeCoverageType) reader.read(null, new StringReader(request), null);

		return cap;
	}

	private double calcDirection(double u, double v){

		double retVal = 0.0;

		try{
			retVal = Math.toDegrees(Math.atan2(u, v)) + 180.0;    		
		}catch(Exception ex){
			System.out.println("calcDirection : " + ex.getMessage());
		}

		// Alternate calculation   
		// retVal = (270-Math.atan2(v,u)*180/Math.PI)%360;

		return retVal;
	}

	private double calcMagnatude(double u, double v){
		double retVal = 0.0;

		try{
			retVal = Math.sqrt((u*u)+(v*v));
		}catch(Exception ex){
			System.out.println("calcMagnatude : " + ex.getMessage());
		}
		return retVal;
	}

	private String convertDate(Date date){
		java.text.DateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
		// explicitly set timezone of input if needed
		df.setTimeZone(java.util.TimeZone.getTimeZone("Zulu"));
		String retVal = df.format(date);

		return retVal;
	}

	private double calcLonSpan(double lLon, double rLon){

		double leftLon, rightLon;

		if (lLon < 0) {
			leftLon = lLon + 360.0;
		} else {
			leftLon = lLon;
		}

		if (rLon < 0) {
			rightLon = rLon + 360.0;
		} else {
			rightLon = rLon;
		}        

		return Math.abs((leftLon - rightLon));
	}

	@SuppressWarnings("deprecation")
	@DescribeResult(
			name="result",
			description="Feature Collection containing the bounding boxes of the layers from the specified workspace. " +
					"The Feature Collection has three properties: name (String), title (String), and geometry (Geometry)."
			)
	public SimpleFeatureCollection execute(
			@DescribeParameter(name="data", description="Layer Data") GridCoverage2D g2c,
			@DescribeParameter(name="currentU", description="Current U Layer") String uLayerName,
			@DescribeParameter(name="currentV", description="Current V Layer") String vLayerName,
			@DescribeParameter(name="wms_time", description="Requested Time") String wmsTime,
			@DescribeParameter(name="wms_elevation", description="Requested Elevation") String wmsElevation,
			@DescribeParameter(name="wms_width", description="Width") int wmswidth,
			@DescribeParameter(name="wms_height", description="Height") int wmsheight, 
			@DescribeParameter(name="wms_crs", description="Coordinate Reference System") CoordinateReferenceSystem wmscrs,
			@DescribeParameter(name="wms_scale_denominator", description="Scale") int scale,
			@DescribeParameter(name="wms_bbox", description="Envelope") ReferencedEnvelope env,
			@DescribeParameter(name="scalelevel", description="Denominator levels") int[] scaleLevel,
			@DescribeParameter(name="shiftlevel", description="Pixel Cube Skip Levels") int[] shiftLevel,
			@DescribeParameter(name="gsizelevel", description="Graphic Size Levels") int[] gsizeLevel,
			@DescribeParameter(name="viewportwidth", description="Viewport Width Levels") int[] viewportwidth,
			@DescribeParameter(name="viewportheight", description="Viewport Height Levels") int[] viewportheight
			) {

		boolean wmsOnePointOne = (wmscrs.getCoordinateSystem().getAxis(0).getAbbreviation().compareTo("Long")==0);
		DefaultFeatureCollection collection = new DefaultFeatureCollection();   	
		SimpleFeatureType featureType =  createFeatureType();
		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( GeoTools.getDefaultHints() );
		CoverageResponseDelegateFinder crdf = null;
		DefaultWebCoverageService100 wcs100 = new DefaultWebCoverageService100(geoServer, crdf);

		int width = 0;
		int height = 0;

		try {
			DirectPosition pos1 = env.getLowerCorner();
			Double leftLon, lowerLat, rightLon, upperLat;

			/*
			height = g2c.getGridGeometry().getGridRange2D().height;
			width = g2c.getGridGeometry().getGridRange2D().width;

			if(width < 21){
				height = 15;
				width = 21;
			}
*/
			if(wmsOnePointOne){
				leftLon = pos1.getOrdinate(0);
				lowerLat = pos1.getOrdinate(1);
				pos1 = env.getUpperCorner();
				rightLon = pos1.getOrdinate(0);
				upperLat = pos1.getOrdinate(1);
			}else{
				leftLon = pos1.getOrdinate(1);
				lowerLat = pos1.getOrdinate(0);
				pos1 = env.getUpperCorner();
				rightLon = pos1.getOrdinate(1);
				upperLat = pos1.getOrdinate(0);		
			}
			
			int shift = 0;
			int gsize = 0;
	
			int count = 0;
			for(int level : scaleLevel){
				
				if(scale > level){
					shift = shiftLevel[count];
					gsize = gsizeLevel[count];
					height = viewportheight[count];
					width = viewportwidth[count];
					break;
				}
				count++;
			}

			// Lat/lon sepearation check
			// Any distance less than .06 seems to cause the netcdf plugin problems.
			// If the span is too small, the WCS returns too many points.
			double loSpan = Math.abs(leftLon - rightLon);
			double laSpan = Math.abs(upperLat - lowerLat);

			if(loSpan < .06){
				leftLon = leftLon - 0.03;
				rightLon = rightLon + 0.03;
			}

			if(laSpan < 06){

				upperLat = upperLat + 0.03;
				lowerLat = lowerLat - 0.03;
			}

			AreaGetCoverageRequest uacr = new AreaGetCoverageRequest(leftLon.toString(), lowerLat.toString(), rightLon.toString(), upperLat.toString());
			uacr.setInterpolation("none");

			if(wmsElevation.length() > 0){
				uacr.setElevation(wmsElevation);
			}
			
			/*
			if(wmsElevation != null && wmsElevation.size() == 1)
				if(wmsElevation.get(0) != null)
					uacr.setElevation(((Double)(wmsElevation.get(0))).toString());
			*/
			
			if(wmsTime.length() > 0){
				uacr.setTime(wmsTime);				
			}
			
			/*
			if(wmsTime != null && wmsTime.size() == 1)
				if(wmsTime.get(0) != null)
					uacr.setTime(convertDate((Date)wmsTime.get(0)));
			*/
			
			uacr.setSourceCoverage(uLayerName);
			uacr.setXSize(width);
			uacr.setYSize(height);
			GridCoverage2D[] u = (GridCoverage2D[]) wcs100.getCoverage(uacr.getGetCoverageType());

			AreaGetCoverageRequest vacr = new AreaGetCoverageRequest(leftLon.toString(), lowerLat.toString(), rightLon.toString(), upperLat.toString());
			vacr.setInterpolation("none");

			if(wmsElevation.length() > 0){
				vacr.setElevation(wmsElevation);
			}

			/*
			if(wmsElevation != null && wmsElevation.size() == 1)
				if(wmsElevation.get(0) != null)
					vacr.setElevation(((Double)(wmsElevation.get(0))).toString());
			*/
			
			if(wmsTime.length() > 0){
				vacr.setTime(wmsTime);				
			}

			/*
			if(wmsTime != null && wmsTime.size() == 1)
				if(wmsTime.get(0) != null)
					vacr.setTime(convertDate((Date)wmsTime.get(0)));
			*/
			
			vacr.setSourceCoverage(vLayerName);
			vacr.setXSize(width);
			vacr.setYSize(height);  

			GridCoverage2D[] v = (GridCoverage2D[]) wcs100.getCoverage(vacr.getGetCoverageType());

			int widthPixels = u[0].getGridGeometry().getGridRange2D().width;
			int heightPixels = u[0].getGridGeometry().getGridRange2D().height;


			int baseX, baseY;

			baseX = u[0].getGridGeometry().getGridRange2D().x;
			baseY = u[0].getGridGeometry().getGridRange2D().y;

			RandomIter uIter = getIter(u[0].getRenderedImage());
			RandomIter vIter = getIter(v[0].getRenderedImage());  

			double xIncrement = Math.abs(calcLonSpan(leftLon, rightLon)/widthPixels);
			double yIncrement = Math.abs((upperLat-lowerLat)/heightPixels);

			Double mag, dir;

			// Debug statements
			System.out.println("WMS Width :" + wmswidth + ":: WMS Height : " + wmsheight);
			System.out.println("G2C Width :" + width + ":: G2C Height : "  + height);
			System.out.println("WCS Width :" + widthPixels + ":: WCS Height : " + heightPixels);
    		System.out.println("LL: " + leftLon + " RL: " + rightLon + " LLat: " + lowerLat + "UL: " + upperLat);
			System.out.println("Scale : " + scale);
			System.out.println("Shift Level : " + shift + " / Gsize Level : " + gsize);
			

			for(int xc=baseX, x=0, fC=0; xc<(baseX+widthPixels); xc+=shift){
				for(int yc=baseY, y=0; yc<(baseY+heightPixels); yc+=shift){

					Double uVal = uIter.getSampleDouble(xc, yc, 0);
					Double vVal = vIter.getSampleDouble(xc, yc, 0);

					if((uVal != null && uVal.isNaN() == false) && (vVal != null && vVal.isNaN() == false)){
						Coordinate coord= new Coordinate();

						//Calc and set data point
						coord.x = leftLon+(xIncrement*x);
						coord.y = upperLat-(yIncrement*y);

						featureBuilder.add(geometryFactory.createPoint(coord));

						//Calc and set Magnatude
						mag = calcMagnatude(uVal, vVal);
						featureBuilder.add(mag);
						//Calc and set Direction
						dir = calcDirection(uVal, vVal);
						featureBuilder.add(dir);
						
						featureBuilder.add(gsize);

						//build the feature
						SimpleFeature feature = featureBuilder.buildFeature( String.valueOf(fC++) );

						collection.add(feature);        				
					}
					y+=shift;
				}
				x+=shift;
			}
			
			System.out.println("gs:MagnatudeDirection Returning " + collection.size() + " items in Point collection.");

		} catch (Exception e) {

		}
		
		return collection;
	}
}