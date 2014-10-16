package org.geoserver.web.data.resource;

import org.geoserver.catalog.CoverageInfo;
import org.geotools.gce.netcdf.NetCDFFormat;

/**
 *
 * @author Yancy
 */
public class NetCDFCoverageResourceConfigurationPanelInfo extends ResourceConfigurationPanelInfo {
    
    @Override
    public boolean canHandle(Object obj) {
        if (obj instanceof CoverageInfo) {
            CoverageInfo coverageInfo = (CoverageInfo) obj;
            
            return NetCDFFormat.NATIVE_FORMAT_NAME.equals(coverageInfo.getNativeFormat());
        }
        
        return false;
    }
    
}
