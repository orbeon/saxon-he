<?xml version="1.0" encoding="UTF-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:svg="http://www.w3.org/2000/svg"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:js="http://saxonica.com/ns/globalJS"
    xmlns:cat="http://www.saxonica.com/ns/doc/catalog"
    xmlns:ch="http://www.saxonica.com/ns/doc/changes"
    xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
    xmlns:f="urn:viewerapp.function"
    exclude-result-prefixes="xs fnd ch"
    extension-element-prefixes="ixsl"
    version="3.0">
    
    <!-- Modified 2017-07-07 
        - sort out use of ixsl:schedule wait, etc, so that found results count is right
        - This no longer works with Saxon-JS 1.0.0, since we rely on lazy evaluation fix to bug
        #3335, for mechanism to check if all documents have been searched.
    -->

    <xsl:template match="p[@class eq 'search']" mode="ixsl:onclick">
        <xsl:call-template name="run-search"/>
    </xsl:template>

    <xsl:template name="run-search">
        <xsl:variable name="search" 
            select="lower-case(normalize-space(ixsl:get($navlist/div/input,'value')))"/>
        <!--<xsl:message>Search input : <xsl:value-of select="$search"/></xsl:message>-->
        
        <xsl:if test="string-length($search) gt 0">
            <xsl:for-each select="ixsl:page()/html/body/div[@id = 'wrap']/div[@class eq 'found']">
                <ixsl:set-style name="display" select="'block'"/>
            </xsl:for-each>
            <xsl:result-document href="#foundPaths" method="ixsl:replace-content">
                <p><i>Search result links:</i></p>
                <xsl:for-each select="doc(concat($location, '/catalog.xml'))/cat:catalog/cat:section">
                    <div id="{@ref}" class="foundP" _searching="true"></div>
                </xsl:for-each>
            </xsl:result-document>
            <xsl:result-document href="#findstatus" method="ixsl:replace-content"> searching... </xsl:result-document>
            
            <xsl:call-template name="searching">
                <xsl:with-param name="search" select="$search"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>

    <xsl:template match="p[@class = ('foundNext','foundPrev','foundClosed')]" mode="ixsl:onclick">
        <xsl:apply-templates mode="found-action" select="."/>
    </xsl:template>

    <xsl:template match="p" mode="found-action">
        <xsl:variable name="foundDiv" select="ixsl:page()/html/body/div[@id = 'wrap']/div[@class eq 'found']"/>
        <xsl:variable name="foundPathsDiv" select="ixsl:page()/html/body/div[@id = 'wrap']/div[@id eq 'foundPaths']"/>
        <xsl:variable name="paths" select="$foundPathsDiv/descendant::p[@class = ('link','hot')]"/>
        
        <xsl:choose>
            <xsl:when test="@class eq 'foundClosed'">
                <xsl:for-each select="$foundDiv|$foundPathsDiv">
                    <ixsl:set-style name="display" select="'none'"/>
                </xsl:for-each>
            </xsl:when>
            
            <xsl:otherwise>
                <xsl:variable name="status-element" select="$foundDiv/p[@id = 'findstatus']"/>
                <xsl:variable name="index" as="xs:integer"
                    select="xs:integer(substring-before($status-element,' of'))"/>
                
                <xsl:variable name="newindex" as="xs:integer"
                    select=" if (@class eq 'link') then count(preceding::p[@class=('link','hot')]) + 1
                    else if (@class eq 'foundNext' and $index lt count($paths)) then $index + 1
                    else if (@class eq 'foundPrev' and $index gt 0) then $index - 1
                    else 0"/>
                <xsl:if test="$newindex gt 0">
                    <xsl:for-each select="$status-element">
                        <xsl:result-document href="?." method="ixsl:replace-content">
                            <xsl:value-of select="$newindex, ' of ', substring-after(., 'of ')"/>
                        </xsl:result-document>
                    </xsl:for-each>
                    <xsl:sequence select="f:set-hash($paths[$newindex]/@path)"/>
                    <xsl:for-each select="$paths[$index]">
                        <ixsl:set-attribute name="class" select="'link'"/>
                    </xsl:for-each>
                    <xsl:for-each select="$paths[$newindex]">
                        <ixsl:set-attribute name="class" select="'hot'"/>
                    </xsl:for-each>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>

    <xsl:template name="searching">
        <xsl:param name="search" as="xs:string"/>
        <!--<xsl:message>searching for <xsl:value-of select="$search"/></xsl:message>-->
        <!--<xsl:message>searching $location: <xsl:value-of select="$location"/> </xsl:message>-->
        <xsl:for-each select="$navlist/ul/li">
            <xsl:variable name="docName" select="concat($location, '/', @id,'.xml')"/>
            <ixsl:schedule-action document="{$docName}">
                <xsl:call-template name="search-doc">
                    <xsl:with-param name="search" select="$search"/>
                    <xsl:with-param name="docName" select="$docName"/>
                    <xsl:with-param name="docID" select="string(@id)"/> <!-- trace(string(@id),  concat('id-for-document ', $docName)) -->
                </xsl:call-template>
            </ixsl:schedule-action>
        </xsl:for-each>
        
    </xsl:template>
    
    <xsl:template name="search-doc">
        <!-- Called from within ixsl:schedule-action document="{$docName}" -->
        <xsl:param name="search"/>
        <xsl:param name="docName"/>
        <xsl:param name="docID"/>
        <!--<xsl:message>doc id: <xsl:value-of select="$docID"/></xsl:message>-->
        <xsl:apply-templates select="doc($docName)/*" mode="check-text">
            <xsl:with-param name="search" select="$search"/>
        </xsl:apply-templates>
        
        <!-- After every doc search, mark that this one is done, and then check if they are all done.
            If all done, then do display-results-->
        <xsl:for-each select="ixsl:page()/html/body/div[@id = 'wrap']/div[@id eq
            'foundPaths']/div[@id=$docID]">
            <!--<xsl:message>setting @searching to false</xsl:message>-->
            <ixsl:set-attribute name="_searching" select="'false'"/>
        </xsl:for-each>
        
        <xsl:if test="f:allSectionsSearched()">
            <xsl:call-template name="display-results"/>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="section|article" mode="check-text">
        <xsl:param name="search"/>
        <xsl:param name="path" as="xs:string" select="''"/>
        <xsl:variable name="newpath" select="concat($path, '/', @id)"/>
        <xsl:variable name="text"
            select="lower-case(string-join(*[not(local-name() = ('section','article','function'))],'!'))"/>
        
        <xsl:if test="contains($text, $search)">
            <xsl:call-template name="foundPaths">
                <xsl:with-param name="newPath" select="substring($newpath,2)"/>
                <xsl:with-param name="section" as="node()" select="."/>
            </xsl:call-template>
        </xsl:if>
        <xsl:apply-templates select="section|article|fnd:function" mode="check-text">
            <xsl:with-param name="search" select="$search"/>
            <xsl:with-param name="path" select="$newpath"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="fnd:function" mode="check-text">
        <xsl:param name="search"/>
        <xsl:param name="path"/>
        <xsl:variable name="newpath" select="concat($path, '/', fnd:name)"/>
        <xsl:variable name="text" select="lower-case(.)"/>
        <xsl:if test="contains($text, $search)">
            <xsl:call-template name="foundPaths">
                <xsl:with-param name="newPath" select="substring($newpath,2)"/>
                <xsl:with-param name="section" as="node()" select="."/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="*" mode="check-text"/>
    
    <!--<xsl:template match="ch:changes" mode="check-text">
        <xsl:param name="search"/>
        <xsl:apply-templates select="ch:release/ch:category" mode="check-text">
            <xsl:with-param name="search" select="$search"/>
        </xsl:apply-templates>
    </xsl:template>
    
    <xsl:template match="ch:category" mode="check-text">
        <xsl:param name="search"/>
        <xsl:variable name="newpath" select="concat('changes/', @name, '/', ../following-sibling::*[1]/@number, '-', ../@number)"/>
        <xsl:variable name="text" select="lower-case(.)"/>
        <xsl:sequence select="if (contains($text, $search)) then $newpath else ()"/>
    </xsl:template>-->
    
    <xsl:template name="foundPaths">
        <xsl:param name="newPath"/>
        <xsl:param name="section"/>
        <xsl:variable name="path-parts" select="tokenize($newPath,'/')"/>
        <xsl:variable name="start" select="$path-parts[1]"/>
        <!--<xsl:message>Found in page: <xsl:value-of select="$newPath"/></xsl:message>-->
        <xsl:for-each select="ixsl:page()/html/body/div[@id = 'wrap']/div[@id eq 'foundPaths']/div[@id=$start]">
            <xsl:result-document href="?." method="ixsl:append-content">
                <p path="{$newPath}" class="link">
                    <xsl:value-of select="if ($section/fnd:name) then f:fn-name($section)
                        else if ($section/@title) then $section/@title else $section/@id"/>
                </p>
            </xsl:result-document>
        </xsl:for-each>
    </xsl:template>
    
    <!-- onclick for found result links -->
    <xsl:template match="div[@id eq 'foundPaths']/div/p[@class eq 'link']" mode="ixsl:onclick">
        <xsl:apply-templates mode="found-action" select="."/>
    </xsl:template>
    
    <xsl:template name="display-results">
        <xsl:variable name="foundPathsDiv" select="ixsl:page()/html/body/div[@id = 'wrap']/div[@id eq 'foundPaths']"/>
        <xsl:variable name="count" select="count($foundPathsDiv/descendant::p[@class = 'link'])"/>
        <!--<xsl:message>display-results </xsl:message>-->
        
        <xsl:if test="$count eq 0">
            <xsl:for-each select="$foundPathsDiv">
                <ixsl:set-style name="display" select="'none'"/>
            </xsl:for-each>
            <xsl:result-document href="#findstatus" method="ixsl:replace-content">
                <xsl:value-of select="'0 of 0'"/>
            </xsl:result-document>
        </xsl:if>
        
        <xsl:if test="$count gt 0">
            <xsl:variable name="path1" select="$foundPathsDiv/descendant::p[@class = 'link'][1]/@path"/>
            <xsl:if test="f:get-hash() eq $path1">
                <!-- In this case set-hash() does not change the hash, so the page content is not reloaded -->
                <xsl:call-template name="highlighting"/>
            </xsl:if>
            <xsl:sequence select="f:set-hash($path1)"/>
            <xsl:for-each select="$foundPathsDiv">
                <ixsl:set-style name="display" select="'block'"/>
            </xsl:for-each>
            <xsl:result-document href="#findstatus" method="ixsl:replace-content">
                <xsl:value-of select="concat('1 of ',$count)"/>
            </xsl:result-document>
            <xsl:for-each select="$foundPathsDiv/descendant::p[@class = 'link'][1]">
                <ixsl:set-attribute name="class" select="'hot'"/>
            </xsl:for-each>
        </xsl:if>
        
    </xsl:template>
    
    <!--<xsl:function name="f:highlight-finds">
        <xsl:variable name="findtext" select="ixsl:get($navlist/div/input, 'value')"/>
        <!-\-<xsl:message>findtext <xsl:value-of select="$findtext"/></xsl:message>-\->
        <xsl:sequence select="js:findit($findtext)"/>
    </xsl:function>-->
    
    <!-- Replace use of js:findit with XSLT solution -->
    
    <xsl:template name="highlighting">
        <xsl:variable name="search" 
            select="lower-case(normalize-space(ixsl:get($navlist/div/input,'value')))"/>
        <xsl:for-each select="ixsl:page()/html/body/div/div[@id eq 'main']">
            <xsl:result-document href="?." method="ixsl:replace-content">
                <xsl:apply-templates mode="highlight-text">
                    <xsl:with-param name="search" select="$search"/>
                </xsl:apply-templates>
            </xsl:result-document>
        </xsl:for-each>
        
        <!-- 2017-09-08 wait should not be completely removed, 
            not sure what value is reasonable. -->
        <ixsl:schedule-action wait="10">
            <xsl:call-template name="scrollToHighlight"/>
        </ixsl:schedule-action>
    </xsl:template>
    
    <xsl:mode name="highlight-text" on-no-match="shallow-copy"/>
    
    <xsl:template match="text()" mode="highlight-text">
        <xsl:param name="search"/>
        <xsl:if test="normalize-space() != ''">
            <xsl:analyze-string select="." regex="{$search}" flags="qi">
                <xsl:matching-substring><span class="highlight"><xsl:value-of select="."/></span></xsl:matching-substring>
                <xsl:non-matching-substring><xsl:value-of select="."/></xsl:non-matching-substring>
            </xsl:analyze-string>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="scrollToHighlight">
        <xsl:variable name="el" select="ixsl:page()/html/body/div/div[@id eq 'main']/descendant::span[@class='highlight'][1]"/>
        <xsl:choose>
            <!-- TODO will this break for browsers which don't support scrollIntoView? 
                Maybe add condition xsl:if test="ixsl:get($el, 'scrollIntoView')"-->
            <xsl:when test="exists($el)">
                <xsl:sequence select="ixsl:call($el, 'scrollIntoView', [true()])"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message>Warning: scrollToHighlight found no highlight to scroll to</xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    

    <!-- Check if all sections have been searched -->
    <xsl:function name="f:allSectionsSearched" as="xs:boolean">
        <!--<xsl:message>allSectionsSearched function</xsl:message>-->
        <!-- if there is a foundpaths div with _searching='true' then return false() -->
        <xsl:variable name="foundPathDivs" select="ixsl:page()/html/body/div[@id = 'wrap']/div[@id
            eq 'foundPaths']/div"/>
        <xsl:sequence select="if (exists($foundPathDivs[@_searching eq 'true'])) then false() else true()"/>
    </xsl:function>
</xsl:transform>
