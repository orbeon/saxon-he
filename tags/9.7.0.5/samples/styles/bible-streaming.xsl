<xsl:stylesheet
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0">
 
<!-- This is an adaptation of the bible.xsl stylesheet published in the Saxon 
     samples for many years; it is changed to use streaming facilities in 
     XSLT 3.0, and is annotated to show where changes were necessary. -->
     
<!-- The following declaration requests streaming. This will be ignored
     by an XSLT 2.0 processor running in forwards compatibility mode -->
     
<xsl:mode streamable="yes"/>           

<xsl:strip-space elements="*"/>

<!-- parameter "dir" must be set from the command line: it represents the output directory -->
<xsl:param name="dir" select="'bible'"/>

<xsl:output name="index" method="html"/>
<xsl:output name="book" method="html"/>
<xsl:output name="chapter" method="html"/>

<xsl:template match="tstmt">
    <xsl:result-document href="file:///{$dir}/index.html" format="index">
        <html>
            <frameset rows="15%,*">
                <noframes>You need a browser that allows frames</noframes>
                <frame src="titlepage.html" />
                <frame src="index2.html" />
            </frameset>
        </html>
    </xsl:result-document>
    <xsl:result-document href="file:///{$dir}/index2.html" format="index">
        <html>
            <frameset cols="16%,14%,*">
                <noframes>You need a browser that allows frames</noframes>
                <frame src="books.html" />
                <frame src="book1.html" name="chapters" />
                <frame src="chap1.1.html" name="content" />
            </frameset>
        </html>
    </xsl:result-document>
    <xsl:result-document href="file:///{$dir}/titlepage.html" format="index">
        <html>
            <body style="font-family:sans-serif" bgcolor="#008080" text="#ffffff" link="#00ffff" vlink="#00cccc">
            <xsl:iterate select="*">
                <xsl:param name="found-preface" select="false()"/>
                <xsl:apply-templates/>
                <xsl:next-iteration>
                    <xsl:with-param name="found-preface" select="$found-preface or self::preface"/>
                </xsl:next-iteration>
                <xsl:on-completion>
                    <xsl:if test="$found-preface">
                        <a href="preface.html" target="_blank">Preface</a>
                    </xsl:if>
                </xsl:on-completion> 
            </xsl:iterate>            
            <div align="right"><a href="coverpage.html" target="_blank">Source</a></div>
            </body>
        </html>
    </xsl:result-document>
</xsl:template>

<xsl:template match="coverpg">
    <xsl:result-document href="file:///{$dir}/coverpage.html" format="book">
        <html>
            <body bgcolor="#00eeee">
                <center>
                    <xsl:apply-templates/>
                    <hr/>
                    <p>This HTML Rendition produced from John Bosak's XML source by
                        <a href="mailto:mike@saxonica.com">Michael Kay</a>
                    </p>
                    <hr/>
                </center>
            </body>
        </html>
    </xsl:result-document>
</xsl:template>

<xsl:template match="titlepg">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="title">
    <center><font size="5"><b>
        <xsl:apply-templates/>
    </b></font></center>
</xsl:template>

<xsl:template match="title2">
    <center><font size="2">
        <xsl:apply-templates/>
    <br/></font></center>
</xsl:template>

<xsl:template match="subtitle">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="preface">
    <xsl:result-document href="file:///{$dir}/preface.html" format="chapter">
        <html><body bgcolor="#00eeee"><center>
            <xsl:apply-templates/>
        </center><hr/></body></html>
    </xsl:result-document>
</xsl:template>

<xsl:template match="ptitle">
    <h2>
        <xsl:apply-templates/>
    </h2>
</xsl:template>

<xsl:template match="ptitle0">
    <p><font size="7" color="red">
        <xsl:apply-templates/>
    </font></p>
</xsl:template>

<xsl:template match="bookcoll">
    <xsl:result-document href="file:///{$dir}/books.html" format="book">
        <html>
            <body bgcolor="#00c0c0">
            <font face="sans-serif" size="2">
                <script language="JavaScript">
                <xsl:comment>
                    function bk(n) {
                        parent.frames['content'].location="chap" + n + ".1.html";
                    }
                    //</xsl:comment>
                </script>
                <xsl:apply-templates/>
            </font>
            </body>
        </html>
    </xsl:result-document>
</xsl:template>

<xsl:template match="book">
    <xsl:variable name="booknr" select="position()"/>
    
    <xsl:variable name="book">
        <xsl:copy-of select="."/>
    </xsl:variable>
    
    <xsl:result-document href="file:///{$dir}/book{$booknr}.html" format="book">
        <html><body bgcolor="#00FFFF"><font face="sans-serif" size="2">
            <xsl:apply-templates select="$book">
                <xsl:with-param name="booknr" select="$booknr"/>
            </xsl:apply-templates>
        </font></body></html>
    </xsl:result-document>
    
    <a href="book{$booknr}.html" target="chapters" onClick="bk({$booknr})">
        <xsl:value-of select="bktshort"/>
    </a>
    <br/>
</xsl:template>

<xsl:template match="bktlong"/>

<xsl:template match="bktshort">
	<h2><xsl:value-of select="."/></h2>
</xsl:template>

<xsl:template match="chapter">
    <xsl:param name="booknr"/>
    <xsl:variable name="chapnr"><xsl:number count="chapter"/></xsl:variable>
    <xsl:variable name="chapfile" select="concat('chap', $booknr, '.', $chapnr, '.html')"/> 
    <xsl:result-document href="file:///{$dir}/{$chapfile}" format="chapter">
        <html><head><title>
        <xsl:value-of select="ancestor::book/bktshort"/>
        </title></head>

        <body text="#000080"><H2><font face="sans-serif">
        <xsl:value-of select="ancestor::book/bktlong"/>
        </font></H2>
        <table>
            <xsl:apply-templates/>        
        </table><hr/>
        </body></html>
    </xsl:result-document>
    <a href="{$chapfile}" target="content">
        <xsl:value-of select="chtitle"/>
    </a><br/>
</xsl:template>

<xsl:template match="chtitle">
    <h2>
	    <xsl:apply-templates/>
	</h2>
</xsl:template>

<xsl:template match="v">
    <xsl:variable name="pos"><xsl:number/></xsl:variable>
    <tr>
        <td valign="top">
            <xsl:choose>
                <xsl:when test="$pos=1">
                    <b><xsl:apply-templates/></b>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates/>
                </xsl:otherwise>
            </xsl:choose>
        </td>
        <td width="10"></td>
        <td valign="top">
            <font color="#808080">
                <xsl:copy-of select="$pos"/>
            </font>
        </td>
    </tr>
</xsl:template>

<xsl:template match="div">
	<xsl:apply-templates/>
</xsl:template>

<xsl:template match="divtitle">
    <tr>
        <td valign="TOP">
            <font color="green">
	            <xsl:apply-templates/>
            </font>
        </td>
    </tr>
</xsl:template>

</xsl:stylesheet>	
