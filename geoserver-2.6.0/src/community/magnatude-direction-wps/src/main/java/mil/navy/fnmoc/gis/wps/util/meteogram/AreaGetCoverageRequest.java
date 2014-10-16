package mil.navy.fnmoc.gis.wps.util.meteogram;

/**
 *
 * @author pcoleman
 */
public class AreaGetCoverageRequest extends GetCoverageRequestWrapper {

    private String[] coords;

    public AreaGetCoverageRequest() {
        coords = new String[]{"-180.0", "-90.0", "180.0", "90.0"};
    }

    public AreaGetCoverageRequest(String leftLon, String lowLat, String rightLon, String upLat) {
        coords = new String[]{leftLon, lowLat, rightLon, upLat};
    }

    @Override
    protected String[] getBBoxCoordinates() {
        return coords;
    }
}
