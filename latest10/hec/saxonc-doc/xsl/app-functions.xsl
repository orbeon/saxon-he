<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:map="http://www.w3.org/2005/xpath-functions/map"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
    xmlns:f="urn:viewerapp.function"
    exclude-result-prefixes="xs" 
    version="3.0">

    <!-- Produce sorted list of functions. -->
    <xsl:function name="f:fn-list" as="element()*">
        <xsl:param name="fns" as="element()*"/>
        <xsl:perform-sort select="$fns">
            <xsl:sort select="lower-case(fnd:name)" lang="en"/>
        </xsl:perform-sort>
    </xsl:function>

    <!-- Get function namespace 'usual' (conventional) prefix (used for definition page titles and function references). -->
    <xsl:function name="f:usual-prefix" as="xs:string">
        <xsl:param name="ns" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$ns = 'http://saxonica.com/ns/interactiveXSLT'">ixsl</xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    <!-- Get lexical QName (i.e. prefix:name) of function from fnd:function element -->
    <xsl:function name="f:fn-name" as="xs:string">
        <xsl:param name="fn-element" as="element(fnd:function)"/>
        <xsl:variable name="prefix" select="f:usual-prefix($fn-element/fnd:name/@namespace)"/>
        <xsl:sequence select="if ($prefix ne '') then concat($prefix, ':', $fn-element/fnd:name) else
            string($fn-element/fnd:name)"/>
    </xsl:function>

    <!-- Determine text within 'Specs versions & Saxon editions' note: Saxon editons & versions. -->
    <xsl:function name="f:edition" as="xs:string*">
        <xsl:param name="available" as="xs:string"/>
        <xsl:param name="implement" as="document-node()"/>
        <xsl:variable name="available"
            select="
                for $t in tokenize($available, '\s+')
                return
                    upper-case($t)"
            as="xs:string*"/>
        <xsl:for-each select="$available">
            <xsl:value-of select="$implement/implement/saxon[@version = current()]"/>
        </xsl:for-each>
    </xsl:function>
</xsl:stylesheet>
