<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:el="http://www.saxonica.com/ns/doc/elements"
    xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
    exclude-result-prefixes="xs"
    expand-text="yes"
    extension-element-prefixes="ixsl"
    version="3.0">

    <xsl:template match="section|article" mode="primary">
        <xsl:variable name="title" select="(h1|h2|h3)[1]"/>
        <xsl:if test="exists($title)">
            <ixsl:set-property name="document.title" select="concat('Saxon-JS ', $title)"/>
        </xsl:if>
        <xsl:apply-templates mode="secondary"/>
    </xsl:template>

    <xsl:template match="node()|@*" mode="secondary">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" mode="secondary"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="section|article" mode="secondary"/>
    
    <xsl:template match="section[@id='functions']" mode="primary" priority="10">
        <xsl:apply-templates select="." mode="function-list"/>
        <!-- See functions-body.xsl -->
    </xsl:template>

    <xsl:template match="dfn" mode="secondary">
        <xsl:apply-templates mode="secondary"/>
    </xsl:template>

    <xsl:template match="nav[ul]" mode="secondary">
        <xsl:variable name="except" select="tokenize(@except, ' ')"/>
        <ul>
            <xsl:for-each select="../(article|section)[not($except = @id)]">
                <li>
                    <p>
                        <span class="link" data-href="{@id}">
                            <xsl:value-of select="@title"/>
                        </span>
                        <xsl:if test="@summary">
                            <xsl:value-of select="':', @summary" separator=" "/>
                        </xsl:if>
                    </p>
                </li>
            </xsl:for-each>
        </ul>
    </xsl:template>

    <xsl:template match="img[@src]" mode="secondary">
        <xsl:variable name="src" select="if (starts-with(@src, '/')) then substring(@src, 2) else ."/>
        <img src="{$src}">
            <xsl:apply-templates select="(@* except @src)" mode="secondary"/>
            <xsl:apply-templates/>
        </img>
    </xsl:template>
    
    <!--<xsl:template match="a[@class = 'anchor']" mode="secondary">
        <a class="link {@class}" id="{@id}">
            <xsl:apply-templates select="node()" mode="secondary"/>
        </a>
    </xsl:template>-->

    <xsl:template match="a|el:a|fnd:a" mode="secondary">
        <xsl:choose>
            <xsl:when
                test="substring(@href, 1, 5) = ('file:','http:') or starts-with(@href, 'https:')">
                <a>
                    <xsl:apply-templates select="node()|@*" mode="secondary"/>
                </a>
            </xsl:when>
            <xsl:when test="substring-after(@href, '.') = ('zip','pdf','txt','xsl','xsd','xml')">
                <!-- Unused in Saxon-JS documentation.
                    - I think only used in main documentation in example of link to internal
                    resource /docimg/web-changes.pdf in help-system -->
                <xsl:variable name="href"
                    select="if (starts-with(@href, '/')) then substring(@href, 2) else @href"/>
                <a href="{$href}">
                    <xsl:apply-templates select="(@* except @href)|node()" mode="secondary"/>
                </a>
            </xsl:when>
            <xsl:when test="ends-with(@href ,'..')">
                <xsl:variable name="levels" select="count(tokenize(@href,'/'))"/>
                <span class="link {@class}" data-href="{string-join(for $l in 1 to ($levels+1) return '../','')}{ancestor::section[$levels+1]/@id}">
                    <xsl:apply-templates select="node()" mode="secondary"/>
                </span>
            </xsl:when>
            <xsl:otherwise>
                <span class="link {@class}" data-href="{@href}">
                    <xsl:apply-templates select="node()" mode="secondary"/>
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:transform>
