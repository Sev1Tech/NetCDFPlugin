package mil.navy.fnmoc.gis.wps.util.meteogram;

/**
 * $HeadURL: https://svn.forge.mil/svn/repos/metocgis/wps/trunk/src/main/java/mil/navy/fnmoc/gis/wps/util/meteogram/GetCoverageRequestWrapper.java $
 * $Id: GetCoverageRequestWrapper.java 10677 2013-02-27 20:44:41Z colemanclayton $
 * Classification: Unclassified
 */

import java.io.StringReader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.geoserver.wcs.xml.v1_0_0.WcsXmlReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.wcs.WCSConfiguration;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import net.opengis.wcs10.GetCoverageType;

/**
 * This class is a container for the GetCoverage request and provides convenient
 * access to its request parameters which is useful when building the
 * FeatureCollection
 *
 * @author Marc Cenac
 */
public abstract class GetCoverageRequestWrapper {

    private static final Logger LOGGER = Logger.getLogger(GetCoverageRequestWrapper.class);

    private static final String WCS_VERSION = "1.0.0";

    private String sourceCoverage = null;
    private String analysisTime = null;
    private String time = null;
    private String elevation = null;
    private String ensembleRun = null;


    private String interpolation = null;
    private String unitOfMeasure = null;
    private int xSize = 3;
    private int ySize = 3;

	public GetCoverageRequestWrapper() {}

    public GetCoverageRequestWrapper(String sourceCoverage, String analysisTime, 
            String time, String elevation, String ensembleRun, 
            String interpolation, String unitOfMeasure) {
        this.sourceCoverage = sourceCoverage;
        this.analysisTime = analysisTime;
        this.time = time;
        this.elevation = elevation;
        this.ensembleRun = ensembleRun;
        this.interpolation = interpolation;
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getSourceCoverage() {
        return this.sourceCoverage;
    }

    public void setSourceCoverage(String sourceCoverage) {
        this.sourceCoverage = sourceCoverage;
    }

    public String getAnalysisTime() {
        return this.analysisTime;
    }

    public void setAnalysisTime(String analysisTime) {
        this.analysisTime = analysisTime;
    }
    
    public String getEnsembleRun() {
        return ensembleRun;
    }
    public void setEnsembleRun(String ensembleRun){
        this.ensembleRun = ensembleRun;
    }
    
    public String getTime() {
        return this.time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getElevation() {
        return this.elevation;
    }

    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public String getInterpolation() {
        return this.interpolation;
    }

    public void setInterpolation(String interpolation) {
        this.interpolation = interpolation;
    }

    public String getUnitOfMeasure() {
        return this.unitOfMeasure;
    }

    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public int getXSize() {
        return this.xSize;
    }

    public void setXSize(int size) {
        this.xSize = size;
    }
    public int getYSize() {
        return this.ySize;
    }

    public void setYSize(int size) {
        this.ySize = size;
    }

    /**
     * Returns the GetCoverageType object representing the request of this
     * instance of GetCoverageRequestWrapper
     *
     * @return the GetCoverageType object representing the request of this
     * instance of GetCoverageRequestWrapper
     */
    public GetCoverageType getGetCoverageType() {
        GetCoverageType type = null;
        WcsXmlReader reader = new WcsXmlReader("GetCoverage", WCS_VERSION, new WCSConfiguration());
        try {
            type = (GetCoverageType) reader.read(null, new StringReader(getXMLRequest()), null);
        } catch (Exception ex) {
            LOGGER.log(Level.ERROR, null, ex);
        }
        return type;
    }

    public String getParameterName() {
        // assumes format <Model>:<Geometry>.<Parameter>.<Level>
        return this.sourceCoverage.split("\\.")[1];
    }

    /**
     * Creates the GetCoverage request as an XML String
     *
     * @return the String of xml representing the GetCoverage execute request
     */
    public String getXMLRequest() {
        String[] coords = getBBoxCoordinates();

        StringBuilder sb = new StringBuilder(1024);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<GetCoverage service=\"WCS\" version=\"1.0.0\"");
        sb.append("  xmlns=\"http://www.opengis.net/wcs\"");
        sb.append("  xmlns:ogc=\"http://www.opengis.net/ogc\"");
        sb.append("  xmlns:gml=\"http://www.opengis.net/gml\"");
        sb.append("  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        sb.append("  xsi:schemaLocation=\"http://www.opengis.net/wcs schemas/wcs/1.0.0/getCoverage.xsd\">");
        sb.append("  <sourceCoverage>").append(this.sourceCoverage).append("</sourceCoverage>");
        sb.append("  <domainSubset>");
        sb.append("    <spatialSubset>");
        sb.append("      <gml:Envelope srsName=\"EPSG:4326\">");
        sb.append("        <gml:pos>").append(coords[0]).append(' ').append(coords[1]).append("</gml:pos>");
        sb.append("        <gml:pos>").append(coords[2]).append(' ').append(coords[3]).append("</gml:pos>");
        sb.append("      </gml:Envelope>");
        sb.append("      <gml:Grid dimension=\"2\">");
        sb.append("        <gml:limits>");
        sb.append("          <gml:GridEnvelope>");
        sb.append("            <gml:low>0 0</gml:low>");
        sb.append("            <gml:high>").append(this.xSize).append(' ').append(this.ySize).append("</gml:high>");
        sb.append("          </gml:GridEnvelope>");
        sb.append("        </gml:limits>");
        sb.append("        <gml:axisName>x</gml:axisName>");
        sb.append("        <gml:axisName>y</gml:axisName>");
        sb.append("      </gml:Grid>");
        sb.append("    </spatialSubset>");
        if (this.time != null) {
            sb.append("    <temporalSubset>");
            sb.append("      <gml:timePosition>").append(this.time).append("</gml:timePosition>");
            sb.append("    </temporalSubset>");
        }
        sb.append("  </domainSubset>");
        if ((this.analysisTime != null) || (this.elevation != null) || (this.ensembleRun != null)) {
            sb.append("  <rangeSubset>");
            if (this.analysisTime != null) {
                sb.append("    <axisSubset name=\"analysis_time\">");
                sb.append("      <singleValue>").append(this.analysisTime).append("</singleValue>");
                sb.append("    </axisSubset>");
            }
            if (this.elevation != null) {
                sb.append("    <axisSubset name=\"elevation\">");
                sb.append("      <singleValue>").append(this.elevation).append("</singleValue>");
                sb.append("    </axisSubset>");
            }
            if (this.ensembleRun != null) {
                sb.append("    <axisSubset name=\"ensemble_run\">");
                sb.append("      <singleValue>").append(this.ensembleRun).append("</singleValue>");
                sb.append("    </axisSubset>");
            }
            sb.append("  </rangeSubset>");
        }
        //sb.append("  <interpolationMethod>").append(this.interpolation).append("</interpolationMethod>");
        sb.append("  <output>");
        sb.append("    <crs>EPSG:4326</crs>");
        sb.append("    <format>GeoTIFF</format>");
        sb.append("  </output>");
        sb.append("</GetCoverage>");
        return sb.toString();
    }

    protected abstract String[] getBBoxCoordinates();

    /**
     * Creates a Point object from a string of WKT
     *
     * @param pointAsWKT the point as well known text, ex. POINT(1 2)
     * @return a Point object
     */
    public static Point createPointFromWKT(String pointAsWKT) {
        WKTReader wktreader = new WKTReader(JTSFactoryFinder.getGeometryFactory());
        try {
            return (Point) wktreader.read(pointAsWKT);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                "Error converting the WKT to a point: " + pointAsWKT, e);
        }
    }

    /**
     * Creates a DirectPosition2D object from a string of WKT
     *
     * @param pointAsWKT the point as well known text, ex. POINT(1 2)
     * @return the DirectPosition2D object for the point
     */
    public static DirectPosition2D createDirectPosition2D(String pointAsWKT) {
        Point point = createPointFromWKT(pointAsWKT);
        return new DirectPosition2D(point.getX(), point.getY());
    }

    /**
     * Returns the number of numbers after the decimal point
     *
     * @param d - a double number
     * @return an integer representing the number of digits
     */
    protected static int getNumberOfFractionDigits(double d) {
        String s = Double.toString(d);
        String fractionalDigits = s.substring(s.indexOf('.') + 1, s.length());
        return fractionalDigits.length();
    }
}
