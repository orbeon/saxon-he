<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
version="2.0">

  <!-- Bug 1930319 -->


	<xsl:output method="xml" indent="yes"/>

	<xsl:strip-space elements="*"/>
	<xsl:param name="grp-size" select="number(20)"/>
	
	<xsl:template match="/">
	  <out><xsl:apply-templates select="/tbody/row" mode="stateitem"/></out>
	</xsl:template>


	
	<xsl:template match="row" mode="stateitem">

			<entry colname="col1" align="left" valign="top">
				<xsl:value-of select="entry[@colname='col1']"/>
			</entry>

	</xsl:template>
	
	
</xsl:stylesheet>
