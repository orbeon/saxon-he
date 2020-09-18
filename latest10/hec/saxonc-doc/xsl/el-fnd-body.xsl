<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:xs="http://www.w3.org/2001/XMLSchema"
   xmlns:el="http://www.saxonica.com/ns/doc/elements"
   xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
   exclude-result-prefixes="xs el fnd"
   version="3.0">
   
   <!-- Share templates for elements common to fnd and el namespaces
         e.g. description, status, details, examples, see-also -->
   
   <!-- Note that: 
            - functions main body processing uses mode f
            - elements main body processing uses unnamed mode
            - since we never use the unnamed mode for elements in the fnd namespace, and use mode f
            only for functions, there won't be a clash by using e.g. match="el:*|fnd:*" mode="#unnamed f" -->
   
   <!-- Also mode secondary used for el:a and fnd:a elements
         - see processing in body.xsl -->
   
   <!-- Note that each el:saxon-element is in a separate section;
        but all fnd:function elements are grouped together in the same section. -->
   
   <xsl:template match="node()|@*" mode="#unnamed f">
      <xsl:copy>
         <xsl:apply-templates select="node()|@*" mode="#current"/>
      </xsl:copy>
   </xsl:template>
   
   <xsl:template match="el:*|fnd:*" mode="#unnamed f">
      <xsl:element name="{local-name()}">
         <xsl:apply-templates select="node()|@*" mode="#current"/>
      </xsl:element>
   </xsl:template>
   
   <xsl:template match="el:a|fnd:a" mode="#unnamed f">
      <xsl:apply-templates select="." mode="secondary"/>
   </xsl:template>
   
   <xsl:template match="el:description|fnd:description" mode="#unnamed f">
      <xsl:apply-templates mode="#current"/>
   </xsl:template>
   
   <xsl:template match="el:status|fnd:status" mode="#unnamed f">
      <h3 class="subtitle">Notes on the Saxon implementation</h3>
      <xsl:apply-templates mode="#current"/>
   </xsl:template>
   
   <xsl:template match="el:details|fnd:details" mode="#unnamed f">
      <h3 class="subtitle">Details</h3>
      <xsl:apply-templates mode="#current"/>
   </xsl:template>
   
   <xsl:template match="el:examples|fnd:examples" mode="#unnamed f">
      <h3 class="subtitle">Examples</h3>
      <!-- List of examples (enumerated if more than one)-->
      <xsl:choose>
         <xsl:when test="*:example">
            <xsl:choose>
               <xsl:when test="count(*:example) = 1">
                  <xsl:apply-templates select="*:example" mode="#current"/>
               </xsl:when>
               <xsl:otherwise>
                  <xsl:iterate select="*:example">
                     <xsl:param name="i" select="1" as="xs:integer"/>
                     <xsl:variable name="new-i" select="$i + 1"/>
                     <h4>Example <xsl:value-of select="$i"/></h4>
                     <xsl:apply-templates mode="#current"/>
                     <xsl:next-iteration>
                        <xsl:with-param name="i" select="$new-i"/>
                     </xsl:next-iteration>
                  </xsl:iterate>
               </xsl:otherwise>
            </xsl:choose>
         </xsl:when>
         <xsl:otherwise>
            <xsl:apply-templates mode="#current"/>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>
   
   <xsl:template match="el:see-also|fnd:see-also" mode="#unnamed f">
      <!-- Links to related ixsl-extensions -->
      <p>
         <span class="link" data-href="/{@type}/{@subtype}/{@name}">
            <xsl:value-of select="concat('ixsl:', @name)"/>
            <xsl:if test="@subtype eq 'functions'">()</xsl:if>
         </span>
      </p>
   </xsl:template>
   
</xsl:stylesheet>