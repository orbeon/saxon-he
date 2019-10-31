<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:map="http://www.w3.org/2005/xpath-functions/map"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:el="http://www.saxonica.com/ns/doc/elements"
    xmlns:f="urn:viewerapp.function"
    exclude-result-prefixes="xs el"
    version="3.0"> 
    
    <!-- For Saxon-JS documentation: Used to produce ixsl-extension instructions pages (in browser),
            from doc/saxonc-extension.xml (contains el:saxon-element elements) -->

    <!-- variable "implement-doc" is defined in jstree.xsl 
        It is safe to use doc($implement-doc) because this doc will have already been loaded from the
        initial 'main' template-->
    
    <!-- Modes used: 
            secondary - for initial match on el:saxon-element
                - initially called from body.xsl
            #unnamed - for main element body content
            model - for (getting started for) element-syntax model section
            -->
    
    <xsl:template match="el:saxon-element" mode="secondary">
        <xsl:apply-templates select="." mode="#unnamed"/>
    </xsl:template>
    

    <xsl:template match="el:saxon-element">
        <xsl:variable name="MyElement" select="."/>
        <xsl:variable name="elemName" select="el:name"/>
        <xsl:variable name="section-ids" select="(ancestor::article/@id, ancestor::section/@id)"/>
        <xsl:variable name="implement" select="doc($implement-doc)" as="document-node()"/>
        <xsl:variable name="defaults"
            select="$implement/implement/default[@id = $section-ids]/@saxon"/>
        <xsl:variable name="default.saxon"
            select="$defaults[position() = last()]"/>
        
        <h1>
            <xsl:value-of select="concat('ixsl:', $elemName)"/>
        </h1>

        <xsl:apply-templates select="el:description" mode="#current"/>

        <aside>
            <xsl:value-of select="f:edition((el:saxon-edition, $default.saxon, 'default')[1], $implement)"/>
        </aside>


        <xsl:if test="el:element-syntax/*[not(self::el:attribute)]">
            <xsl:apply-templates select="el:element-syntax" mode="model"/>
        </xsl:if>

        <xsl:if test="el:element-syntax/el:attribute">
            <h3 class="subtitle">Attributes</h3>
            <xsl:apply-templates select="el:element-syntax" mode="#current"/>
        </xsl:if>
        
        <xsl:apply-templates select="el:status" mode="#current"/>
        <!-- <status-ok> elements are ignored -->
        
        <xsl:apply-templates select="el:details" mode="#current"/>

        <xsl:apply-templates select="el:examples" mode="#current"/>

        <xsl:if test="el:see-also">
            <h3 class="subtitle">See also</h3>
            <xsl:apply-templates select="el:see-also" mode="#current"/>
        </xsl:if>

    </xsl:template>
    

    <xsl:template match="el:element-syntax">
        <!-- Build table of attributes with descriptions -->
        <table class="element-att">
            <xsl:for-each select="el:attribute">
                <tr>
                    <td>
                        <p>
                            <code>
                                <xsl:apply-templates select="."/>
                            </code>
                        </p>
                    </td>
                    <td>
                        <p>
                            <code>
                                <xsl:apply-templates select="* except el:desc"/>
                            </code>
                        </p>
                    </td>
                    <td>
                        <p>
                            <xsl:apply-templates select="el:desc"/>
                        </p>
                    </td>
                </tr>
            </xsl:for-each>
        </table>
    </xsl:template>

    <xsl:template match="el:attribute">
        <xsl:choose>
            <xsl:when test="@deprecated='yes'">
                <xsl:text>[</xsl:text>
                <xsl:value-of select="@name"/>
                <xsl:text>]?</xsl:text>
            </xsl:when>
            <xsl:when test="@required='yes'">
                <strong>
                    <xsl:value-of select="@name"/>
                </strong>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="@name"/>
                <xsl:text>?</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="el:data-type">
        <xsl:if test="position()>3"> | </xsl:if>
        <i>
            <xsl:value-of select="@name"/>
        </i>
    </xsl:template>

    <xsl:template match="el:constant">
        <xsl:if
            test="(position()>1 and not(parent::el:attribute-value-template)) or (position()>2 and parent::el:attribute-value-template)"
            > | </xsl:if>
        <xsl:text>"</xsl:text>
        <xsl:value-of select="@value"/>
        <xsl:text>"</xsl:text>
    </xsl:template>

    <xsl:template match="el:attribute-value-template">
        <xsl:text>{ </xsl:text>
        <xsl:apply-templates/>
        <xsl:text> }</xsl:text>
    </xsl:template>

    <xsl:template match="el:desc">
        <xsl:apply-templates mode="#current"/>
    </xsl:template>
    
    

    <!-- Model section-->

    <xsl:template match="el:element-syntax" mode="model">
        <a name="element-{@name}"/>
        <p class="element-syntax">
            <xsl:apply-templates select="el:in-category"/>
            <xsl:apply-templates select="el:sequence|el:choice|el:model|el:content|el:text|el:empty"
                mode="#current"/>
            <xsl:apply-templates select="el:allowed-parents"/>
            <xsl:if test="not(el:attribute)">
                <xsl:call-template name="no-attributes"/>
            </xsl:if>
        </p>
    </xsl:template>

    <xsl:template match="el:in-category">
        <strong>Category: </strong>
        <xsl:value-of select="@name"/>
        <br/>
    </xsl:template>

    <xsl:template name="no-attributes">
        <br/>
        <strong>
            <i>Element has no attributes</i>
        </strong>
    </xsl:template>

    <xsl:template match="el:sequence|el:choice|el:model|el:content|el:text|el:empty" mode="model">
        <strong>Content: </strong>
        <xsl:apply-templates select="."/>
    </xsl:template>

    <xsl:template match="el:sequence|el:choice">
        <xsl:call-template name="group"/>
        <xsl:text>(</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>)</xsl:text>
        <xsl:call-template name="repeat"/>
    </xsl:template>

    <xsl:template match="el:model">
        <xsl:call-template name="group"/>
        <i>
            <xsl:value-of select="@name"/>
        </i>
        <xsl:call-template name="repeat"/>
    </xsl:template>

    <xsl:template match="el:text">#PCDATA</xsl:template>

    <xsl:template match="el:empty">none</xsl:template>

    <xsl:template match="el:content">
        <xsl:call-template name="group"/>
        <xsl:choose>
            <xsl:when test="contains(@name, ':')">
                <xsl:value-of select="@name"/>
            </xsl:when>
            <xsl:otherwise>
                <a class="bodylink code" href="/xsl-elements/{@name}">
                    <xsl:value-of select="concat('xsl:', @name)"/>
                </a>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:call-template name="repeat"/>
    </xsl:template>

    <xsl:template name="group">
        <xsl:if test="position()>2">
            <!-- Changed "position()>1" to "position()>2". DL-->
            <xsl:choose>
                <xsl:when test="parent::el:sequence">, </xsl:when>
                <xsl:when test="parent::el:choice"> | </xsl:when>
                <xsl:when test="parent::el:allowed-parents">; </xsl:when>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template name="repeat">
        <xsl:choose>
            <xsl:when test="@repeat='one-or-more'">
                <xsl:text>+</xsl:text>
            </xsl:when>
            <xsl:when test="@repeat='zero-or-more'">
                <xsl:text>*</xsl:text>
            </xsl:when>
            <xsl:when test="@repeat='zero-or-one'">
                <xsl:text>?</xsl:text>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="el:allowed-parents">
        <br/>
        <strong>Permitted parent elements: </strong>
        <xsl:apply-templates/>
        <xsl:if test="not(*)">None</xsl:if>
    </xsl:template>

    <xsl:template match="el:parent">
        <xsl:call-template name="group"/>
        <a href="/xsl-elements/{@name}" class="bodylink code">
            <xsl:value-of select="concat('xsl:', @name)"/>
        </a>
    </xsl:template>

    <xsl:template match="el:parent-category[@name='sequence-constructor']">
        <xsl:if test="position()>2">; </xsl:if> any XSLT element whose content model is 
        <i>sequence-constructor</i>; any literal result element
    </xsl:template>

</xsl:stylesheet>
