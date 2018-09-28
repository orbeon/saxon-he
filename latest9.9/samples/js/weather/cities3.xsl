<xsl:transform xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:prop="http://saxonica.com/ns/html-property"
    xmlns:style="http://saxonica.com/ns/html-style-property"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" version="3.0" xmlns:w="http://openweathermap.org/w"
    exclude-result-prefixes="w xs" expand-text="yes" extension-element-prefixes="ixsl prop style">



    <xsl:template match="/" name="main">
        <xsl:call-template name="show-all"/>
    </xsl:template>

    <xsl:function name="w:fetch">
        <xsl:param name="uri"/>
        <xsl:sequence select="parse-json(unparsed-text($uri))"/>
    </xsl:function>

    <xsl:template name="show-all">
        <xsl:result-document href="#title" method="ixsl:replace-content">
            A List of Cities Beginning With {(ixsl:query-params()?select, 'A')[1]}
           <!-- A List of Cities Beginning With <xsl:value-of select="ixsl:query-params()('select')"/>-->
        </xsl:result-document>
        <xsl:result-document href="#target" method="ixsl:replace-content">
            <table>
                <thead>
                    <tr>
                        <td>City</td>
                        <td>Country</td>
                        <td>Longitude</td>
                        <td>Latitude</td>
                    </tr>
                </thead>
                <tbody>
                    <xsl:variable name="X" select="(ixsl:query-params()?select, 'A')[1]"/>
                    <xsl:for-each select="w:fetch('http://127.0.0.1:8000/SaxonJS/weather/cities/'||$X||'.json')?*">
                        <xsl:sort select="?name"/>
                        <tr>
                            <td>{?name}</td>
                            <td>{?country}</td>
                            <td>{?coord?lon}</td>
                            <td>{?coord?lat}</td>
                        </tr>
                    </xsl:for-each>
                </tbody>
            </table>
        </xsl:result-document>
    </xsl:template>





</xsl:transform>
