package org.geoserver.web.data.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.web.util.MapModel;
import org.geotools.gce.netcdf.NetCDFFormat;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;

/**
 *
 * @author Yancy
 */
public class NetCDFCoverageResourceConfigurationPanel extends ResourceConfigurationPanel {

    static final Logger LOGGER = Logging.getLogger(NetCDFCoverageResourceConfigurationPanel.class);
    
    DropDownChoice netcdfParameter;

    public NetCDFCoverageResourceConfigurationPanel(String id, final IModel model) {
        super(id, model);
        try {
            final CoverageInfo coverage = (CoverageInfo) getResourceInfo();
            
            GridCoverageReader gridCoverageReader = coverage.getGridCoverageReader(null, null);
            String variablesString = gridCoverageReader.getMetadataValue(NetCDFFormat.NETCDF_PARAMETER_NAME);

            final IModel paramsModel = new PropertyModel(model, "parameters");

            netcdfParameter = new DropDownChoice("netcdfParameter",
                    new MapModel(paramsModel, NetCDFFormat.NETCDF_PARAMETER_NAME),
                    Arrays.asList(variablesString.split(",")),
                    new NetCDFParameterListChoiceRenderer());
            
            add(netcdfParameter);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    class NetCDFParameterListChoiceRenderer implements IChoiceRenderer<String> {

        public Object getDisplayValue(String netcdfParameter) {
            return netcdfParameter;
        }

        public String getIdValue(String netcdfParameter, int arg1) {
            return netcdfParameter;
        }
    }
}
