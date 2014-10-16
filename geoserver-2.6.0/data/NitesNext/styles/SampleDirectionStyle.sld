<?xml version="1.0" encoding="iso-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
xsi:schemaLocation="http://www.opengis.net/sld StyledLayerDescriptor.xsd"
xmlns="http://www.opengis.net/sld"
xmlns:ogc="http://www.opengis.net/ogc"
xmlns:xlink="http://www.w3.org/1999/xlink"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns:gs="http://geoserver.org">
  <NamedLayer>
    <Name>direction</Name>
    <UserStyle>
      <Title>direction</Title>
      <Abstract>Extracts contours from DEM</Abstract>
      <FeatureTypeStyle>
        <Transformation>
          <ogc:Function name="gs:MagnatudeDirection">
            <ogc:Function name="parameter">
              <ogc:Literal>data</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>currentU</ogc:Literal>
              <ogc:Literal>sandy.ADCIRC.U-VEL</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>currentV</ogc:Literal>
              <ogc:Literal>sandy.ADCIRC.V-VEL</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_time</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_time</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_elevation</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_elevation</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_width</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_width</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_height</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_height</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_bbox</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_bbox</ogc:Literal>
              </ogc:Function>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>wms_crs</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_crs</ogc:Literal>
              </ogc:Function>
            </ogc:Function>   
            <ogc:Function name="parameter">
              <ogc:Literal>wms_scale_denominator</ogc:Literal>
              <ogc:Function name="env">
                <ogc:Literal>wms_scale_denominator</ogc:Literal>
              </ogc:Function>
            </ogc:Function>     
            <ogc:Function name="parameter">
              <ogc:Literal>scalelevel</ogc:Literal>
              <ogc:Literal>2000000</ogc:Literal>
              <ogc:Literal>1000000</ogc:Literal>
              <ogc:Literal>500000</ogc:Literal>
              <ogc:Literal>250000</ogc:Literal>
              <ogc:Literal>125000</ogc:Literal>
              <ogc:Literal>50000</ogc:Literal>
              <ogc:Literal>20000</ogc:Literal>
              <ogc:Literal>0</ogc:Literal>
            </ogc:Function>   
            <ogc:Function name="parameter">
              <ogc:Literal>shiftlevel</ogc:Literal>
              <ogc:Literal>20</ogc:Literal>
              <ogc:Literal>13</ogc:Literal>
              <ogc:Literal>11</ogc:Literal>
              <ogc:Literal>9</ogc:Literal>
              <ogc:Literal>7</ogc:Literal>
              <ogc:Literal>5</ogc:Literal>
              <ogc:Literal>3</ogc:Literal>
              <ogc:Literal>2</ogc:Literal>
            </ogc:Function>             
            <ogc:Function name="parameter">
              <ogc:Literal>gsizelevel</ogc:Literal>
              <ogc:Literal>12</ogc:Literal>
              <ogc:Literal>14</ogc:Literal>
              <ogc:Literal>13</ogc:Literal>
              <ogc:Literal>11</ogc:Literal>
              <ogc:Literal>12</ogc:Literal>
              <ogc:Literal>13</ogc:Literal>
              <ogc:Literal>14</ogc:Literal>
              <ogc:Literal>15</ogc:Literal>
            </ogc:Function>   
            <ogc:Function name="parameter">
              <ogc:Literal>viewportwidth</ogc:Literal>
              <ogc:Literal>512</ogc:Literal>
              <ogc:Literal>318</ogc:Literal>
              <ogc:Literal>159</ogc:Literal>
              <ogc:Literal>81</ogc:Literal>
              <ogc:Literal>41</ogc:Literal>
              <ogc:Literal>21</ogc:Literal>
              <ogc:Literal>21</ogc:Literal>
              <ogc:Literal>21</ogc:Literal>
            </ogc:Function> 
            <ogc:Function name="parameter">
              <ogc:Literal>viewportheight</ogc:Literal>
              <ogc:Literal>364</ogc:Literal>
              <ogc:Literal>227</ogc:Literal>
              <ogc:Literal>114</ogc:Literal>
              <ogc:Literal>57</ogc:Literal>
              <ogc:Literal>29</ogc:Literal>
              <ogc:Literal>15</ogc:Literal>
              <ogc:Literal>15</ogc:Literal>
              <ogc:Literal>15</ogc:Literal>
            </ogc:Function>              
          </ogc:Function>
        </Transformation>
        <Rule>
          <Name>0 - 0.038214</Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.0</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.3</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label><![CDATA[.]]></Label>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:href="arrow-1.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              
              <Rotation>
                <ogc:Add>
                  <ogc:PropertyName>direction</ogc:PropertyName>
                  <ogc:Literal>180.0</ogc:Literal>
                </ogc:Add>
              </Rotation>
            </Graphic>
            <VendorOption name="conflictResolution">false</VendorOption>
          </TextSymbolizer>
        </Rule>
        <Rule>
          <Name>0.038215 - 0.086976</Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.31</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.5</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label><![CDATA[.]]></Label>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:href="arrow-2.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>
                <ogc:PropertyName>gsize</ogc:PropertyName>
              </Size>                
              <Rotation>
                <ogc:Add>
                  <ogc:PropertyName>direction</ogc:PropertyName>
                  <ogc:Literal>180.0</ogc:Literal>
                </ogc:Add>
              </Rotation>
            </Graphic>
            <VendorOption name="conflictResolution">false</VendorOption>
          </TextSymbolizer>
        </Rule>
        <Rule>
          <Name>0.086977 - 0.154270</Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.51</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.7</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label><![CDATA[.]]></Label>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:href="arrow-3.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>
                <ogc:PropertyName>gsize</ogc:PropertyName>
              </Size>  
              <Rotation>
                <ogc:Add>
                  <ogc:PropertyName>direction</ogc:PropertyName>
                  <ogc:Literal>180.0</ogc:Literal>
                </ogc:Add>
              </Rotation>
            </Graphic>
            <VendorOption name="conflictResolution">false</VendorOption>
          </TextSymbolizer>
        </Rule>
        <Rule>
          <Name>0.154271 - 0.278296</Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.71</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.9</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label><![CDATA[.]]></Label>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:href="arrow-4.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>
                <ogc:PropertyName>gsize</ogc:PropertyName>
              </Size> 
              <Rotation>
                <ogc:Add>
                  <ogc:PropertyName>direction</ogc:PropertyName>
                  <ogc:Literal>180.0</ogc:Literal>
                </ogc:Add>
              </Rotation>
            </Graphic>
            <VendorOption name="conflictResolution">false</VendorOption>
          </TextSymbolizer>
        </Rule>  
        <Rule>
          <Name>0.278297 - 0.564170</Name>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsGreaterThanOrEqualTo>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>0.91</ogc:Literal>
              </ogc:PropertyIsGreaterThanOrEqualTo>
              <ogc:PropertyIsLessThan>
                <ogc:PropertyName>magnatude</ogc:PropertyName>
                <ogc:Literal>10.0</ogc:Literal>
              </ogc:PropertyIsLessThan>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label><![CDATA[.]]></Label>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:href="arrow-5.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>
                <ogc:PropertyName>gsize</ogc:PropertyName>
              </Size> 
              <Rotation>
                <ogc:Add>
                  <ogc:PropertyName>direction</ogc:PropertyName>
                  <ogc:Literal>180.0</ogc:Literal>
                </ogc:Add>
              </Rotation>
            </Graphic>
            <VendorOption name="conflictResolution">false</VendorOption>
          </TextSymbolizer>
        </Rule> 
        
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>