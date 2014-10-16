<?xml version="1.0" encoding="UTF-8"?><sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
  <sld:NamedLayer>
    <sld:Name>currentSpeed</sld:Name>
    <sld:UserStyle>
      <sld:Name>currentSpeed</sld:Name>
      <sld:Title>Contour DEM</sld:Title>
      <sld:Abstract>Extracts contours from DEM</sld:Abstract>
      <sld:FeatureTypeStyle>
        <sld:Name>name</sld:Name>
        <sld:Rule>
                    <sld:RasterSymbolizer>
                        <sld:Opacity>1.0</sld:Opacity>
  <ColorMap>
  <ColorMapEntry color="#0135D4" quantity="0.00000" label="0 m/s"/>
  <ColorMapEntry color="#0084D1" quantity="0.01" label="5 m/s"/>
  <ColorMapEntry color="#00CFCD" quantity="0.02" label="10 m/s"/>
  <ColorMapEntry color="#00CD7C" quantity="0.05" label="15 m/s"/>
  <ColorMapEntry color="#00CB2D" quantity="0.1" label="20 m/s"/>
  <ColorMapEntry color="#20C900" quantity="0.15" label="25 m/s"/>
  <ColorMapEntry color="#6CC700" quantity="0.20" label="30 m/s"/>
  <ColorMapEntry color="#B6C500" quantity="0.3" label="35 m/s"/>
  <ColorMapEntry color="#C38700" quantity="0.7" label="40 m/s"/>
  <ColorMapEntry color="#C13B00" quantity="0.9" label="45 m/s"/>
  <ColorMapEntry color="#BF000E" quantity="1.2" label="50 m/s"/>
  <ColorMapEntry color="#FFFFFF" quantity="2.0" label="50 m/s"/>
</ColorMap>

                      
                      
                    </sld:RasterSymbolizer>
        </sld:Rule>
      </sld:FeatureTypeStyle>
    </sld:UserStyle>
  </sld:NamedLayer>
</sld:StyledLayerDescriptor>