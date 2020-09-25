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
    exclude-result-prefixes="#all" 
    expand-text="yes"
    extension-element-prefixes="ixsl"
    version="3.0">
    
    <!-- Stylesheet to generate the Saxon/C documentation viewer app using Saxon-JS in the browser. 
        The initial template is named 'main'.
        
        Note that functions-body.xsl and elements-body.xsl are adapted from the versions for the
        main Saxon documentation viewer_app: 
            - The versions here are as used for the Saxon-JS documentation.
            - e.g. Various simplifications are made to assume all instructions and functions referenced are
            those in the ixsl namespace
            - But these are not actually currently used in Saxon/C documentation
            
            - use shared templates in el-fnd-body.xsl for corresponding elements in fnd and el namespaces
    -->

    <xsl:import href="body.xsl"/>
    <xsl:import href="findtext.xsl"/>
    <xsl:import href="app-functions.xsl"/>
    <xsl:import href="el-fnd-body.xsl"/><!-- Must be imported before functions-body.xsl and
        elements-body.xsl to ensure correct import precedence -->
    <xsl:import href="functions-body.xsl"/>
    <xsl:import href="elements-body.xsl"/>
    <!--<xsl:import href="changes.xsl"/>-->
   
    <xsl:param name="product" select="'Saxon/C'" as="xs:string"/>
    <xsl:param name="SEFbuildDate" select="'2020-08-24'" as="xs:string" static="yes"/>
    <xsl:param name="showStatusMessage" select="false()" as="xs:boolean"/>

    <xsl:variable name="location" select="resolve-uri('doc', ixsl:location())"/>
    <xsl:variable name="implement-doc" select="concat($location, '/implement.xml')"/>
    
    <!--<xsl:variable name="docChanges" select="doc(concat($location, '/changes.xml'))"
        as="document-node()"/>-->

    <xsl:variable name="navlist" as="node()" select="ixsl:page()/html/body/div/div[@id = 'nav']"/>

    <xsl:template name="main">
        <xsl:variable name="catDocName" select="concat($location, '/catalog.xml')"/>
        
        <xsl:if test="$showStatusMessage">
            <xsl:message expand-text="yes">{$SEFbuildDate} {$product} documentation app running with Saxon-JS {ixsl:eval('SaxonJS.getProcessorInfo()["productVersion"]')}</xsl:message>
        </xsl:if>
        <ixsl:schedule-action document="{$catDocName}">
            <xsl:call-template name="list">
                <xsl:with-param name="docName" select="$catDocName"/>
            </xsl:call-template>
        </ixsl:schedule-action>
        
        <ixsl:schedule-action document="{$implement-doc}">
            <!-- This ensures that implement.xml has been loaded, and so future calls to
                doc($implement-doc) are fine -->
            <xsl:call-template name="get-implement-doc">
                <xsl:with-param name="implement-doc" select="$implement-doc"/>
            </xsl:call-template>
        </ixsl:schedule-action>
    </xsl:template>
    
    <xsl:template name="list"> 
        <!-- Called from within ixsl:schedule-action document={$docName} -->
        <xsl:param name="docName"/>
        <xsl:result-document href="#list" method="ixsl:replace-content">
            <xsl:apply-templates
                select="doc($docName)/cat:catalog/cat:section"/>
        </xsl:result-document>
        
        <ixsl:schedule-action wait="1">
            <xsl:call-template name="init"/>
        </ixsl:schedule-action>
    </xsl:template>
    
    <xsl:template name="get-implement-doc">
        <xsl:param name="implement-doc"/>
        <xsl:sequence select="doc($implement-doc)[current-date() lt xs:date('2000-01-01')]"/>
    </xsl:template>

    <xsl:template name="init">
        <xsl:call-template name="process-hashchange"/>
    </xsl:template>

    <xsl:template match="cat:section">
        <li class="closed" id="{@ref}">
            <span class="item">
                <xsl:value-of select="."/>
            </span>
        </li>
    </xsl:template>


    <xsl:template match="." mode="ixsl:onhashchange">
        <xsl:call-template name="process-hashchange"/>
    </xsl:template>
    
    <!-- Arrow keys for navigation, and return key for search -->
    <xsl:template match="." mode="ixsl:onkeydown">
        <xsl:variable name="event" select="ixsl:event()"/>
        <xsl:variable name="key" select="ixsl:get($event, 'key')" as="xs:string"/>
        <xsl:variable name="class"
            select="if ($key = ('ArrowLeft', 'Left', 'PageUp')) then 'arrowLeft'
            else if ($key = ('ArrowRight', 'Right', 'PageDown')) then 'arrowRight'
            else if ($key eq 'Enter') then 'enter' else ()"/>
        <xsl:if test="exists($class)">
            <!--<xsl:message>keydown <xsl:value-of select="$key"/></xsl:message>-->
            <xsl:choose>
                <xsl:when test="$class eq 'enter'">
                    <xsl:call-template name="run-search"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:sequence select="f:navpage($class)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template match="p[@class eq 'arrowNone']" mode="ixsl:onclick">
        <xsl:for-each select="$navlist/ul/li">
            <ixsl:set-attribute name="class" select="'closed'"/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="span[@data-href]" mode="ixsl:onclick">
        <xsl:sequence select="f:anchor-navigation(.)"/>
    </xsl:template>

    <xsl:template match="span[@data-href][@class eq 'flink']" mode="ixsl:onmouseover">
        <xsl:variable name="docName" select="concat($location, '/ixsl-extension.xml')"/>
        <ixsl:schedule-action document="{$docName}">
            <xsl:call-template name="show-fn">
                <xsl:with-param name="span" select="."/>
                <xsl:with-param name="docName" select="$docName"/>
            </xsl:call-template>
        </ixsl:schedule-action>
    </xsl:template>

    <xsl:template name="show-fn">
        <!-- Called from within ixsl:schedule-action document={$docName} -->
        <xsl:param name="span"/>
        <xsl:param name="docName"/>
        <xsl:variable name="href" select="$span/@data-href"/>
        <xsl:variable name="fn"
            select="doc($docName)/article/section[@id='functions']/fnd:function[fnd:name = $href]"/>
        <xsl:result-document href="#fn-desc" method="ixsl:replace-content">
            <h4>
                <xsl:value-of select="concat('ixsl:', $href)"/>
            </h4>
            <xsl:apply-templates select="$fn/fnd:description|$fn/fnd:signatures"
                mode="fn-description"/>
        </xsl:result-document>
    </xsl:template>

    
    <xsl:template name="process-hashchange">
        <xsl:variable name="hash-parts" select="tokenize(f:get-hash(),'/')"/>
        <xsl:variable name="start" select="$hash-parts[1]"/>
        <xsl:variable name="docName"
            select="concat($location, '/', $start,'.xml')"/>


        <ixsl:schedule-action document="{$docName}">
            <xsl:call-template name="render-page">
                <xsl:with-param name="hash-parts" select="$hash-parts"/>
                <xsl:with-param name="docName" select="$docName"/>
            </xsl:call-template>
        </ixsl:schedule-action>
        
    </xsl:template>

    <xsl:template name="render-page">
        <!-- Called from within ixsl:schedule-action document={$docName} -->
        <xsl:param name="docName"/>
        <xsl:param name="hash-parts"/>
        <xsl:variable name="start" select="$hash-parts[1]"/>
        <xsl:variable name="first-item" select="f:get-first-item($start)" as="node()?"/> 
        <!-- $first-item is the li node of the navlist with the @id $start -->
        <xsl:variable name="doc" select="if (doc-available($docName)) then doc($docName) else ()"/>
        <xsl:choose>
            <xsl:when test="exists($doc) and exists($first-item)">
                <xsl:call-template name="show-listitems">
                    <xsl:with-param name="doc" select="$doc"/>
                    <xsl:with-param name="ids" select="$hash-parts"/>
                    <xsl:with-param name="index" select="1"/>
                    <xsl:with-param name="item" select="$first-item"/>
                </xsl:call-template>
                <xsl:variable name="count" select="count($hash-parts)"/>
                <xsl:variable name="isfunction"
                    select="$start eq 'ixsl-extension' and $hash-parts[2] eq 'functions'"
                    as="xs:boolean"/>
                <xsl:result-document href="#main" method="ixsl:replace-content">
                    <!--<xsl:message>replace-content #main: {$hash-parts}, count {$count}</xsl:message>-->
                    <xsl:choose>
                        <xsl:when test="$isfunction and $count eq 3">
                            <xsl:apply-templates mode="f"
                                select="$doc//fnd:function[string(fnd:name) eq $hash-parts[3]]"/>
                        </xsl:when>
                        <!--<xsl:when test="$start eq 'changes' and $count eq 1">
                            <xsl:apply-templates select="$doc" mode="changes"/>
                        </xsl:when>
                        <xsl:when test="$start eq 'changes' and $count eq 2">
                            <!-\-<xsl:message>changes <xsl:value-of select="$hash-parts[2]"/></xsl:message>-\->
                            <xsl:apply-templates select="$doc" mode="changes">
                                <xsl:with-param name="selectedCategory" select="$hash-parts[2]"/>
                            </xsl:apply-templates>
                        </xsl:when>
                        <xsl:when test="$start eq 'changes' and $count eq 3">
                            <!-\-<xsl:message>changes <xsl:value-of select="$hash-parts[2]"/> <xsl:value-of select="$hash-parts[3]"/></xsl:message>-\->
                            <xsl:apply-templates select="$doc" mode="changes">
                                <xsl:with-param name="selectedCategory"
                                    select="$hash-parts[2][. != '']"/>
                                <xsl:with-param name="selectedVersionRange"
                                    select="$hash-parts[3][. != '']"/>
                            </xsl:apply-templates>
                        </xsl:when>-->
                        <xsl:when test="$count eq 1">
                            <xsl:apply-templates select="$doc" mode="primary"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="$doc/*" mode="get-section">
                                <xsl:with-param name="ids" select="$hash-parts"/>
                                <xsl:with-param name="parent" select="$doc/*"/>
                                <xsl:with-param name="index" select="2"/>
                            </xsl:apply-templates>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:result-document>
                
                <ixsl:schedule-action wait="1">
                    <xsl:call-template name="scrollpage"/>
                </ixsl:schedule-action>

                <xsl:result-document href="#trail" method="ixsl:replace-content">
                    <xsl:variable name="ul"
                        select="ixsl:page()/html/body/div/div[@id = 'header']/ul[@class='trail']"/>
                    <xsl:copy-of select="$ul/li[@id eq 'trail1']"/>
                    <xsl:copy-of select="$ul/li[@id eq 'trail2']"/>
                    <xsl:call-template name="get-trail">
                        <xsl:with-param name="ids" select="$hash-parts"/>
                        <xsl:with-param name="parent" select="$doc"/>
                        <xsl:with-param name="index" select="1"/>
                    </xsl:call-template>
                </xsl:result-document>

                <ixsl:schedule-action wait="1">
                    <xsl:call-template name="highlight-item">
                        <xsl:with-param name="parent" select="$navlist"/>
                        <xsl:with-param name="ids" select="$hash-parts"/>
                        <xsl:with-param name="index" select="1"/>
                    </xsl:call-template>
                </ixsl:schedule-action>
            </xsl:when>
            <xsl:otherwise>
                <xsl:result-document href="#main" method="ixsl:replace-content">
                    <h1>Page Not Found</h1>
                    <p>Error in URI hash-path:</p>
                    <p><xsl:value-of
                            select="if (exists($doc)) then ('List Item ''', $start) else ('Document ''', $docName)"
                        />' not found</p>
                </xsl:result-document>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="scrollpage">
        <ixsl:set-property
            object="ixsl:page()/html/body/div[@id = 'wrap']/div[@id = 'main']"
            name="scrollTop" select="0"/>
    </xsl:template>

    <xsl:template match="section|article" mode="get-section">
        <xsl:param name="ids" as="xs:string*"/>
        <xsl:param name="parent" as="node()?"/>
        <xsl:param name="index" as="xs:integer"/>
        <xsl:variable name="sectionEl" select="./(section|article)[@id eq $ids[$index]]"/>
        <xsl:choose>
            <xsl:when test="empty($sectionEl)">
                <p>Error in URI hash-path:</p>
                <p>Section '<xsl:value-of select="$ids[$index]"/>' not found in path: <xsl:value-of
                        select="$ids" separator="/"/></p>
            </xsl:when>
            <xsl:when test="$index gt count($ids)"/>
            <xsl:when test="$index eq count($ids)">
                <xsl:apply-templates select="$sectionEl" mode="primary"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:apply-templates select="$sectionEl" mode="get-section">
                    <xsl:with-param name="ids" select="$ids"/>
                    <xsl:with-param name="parent" select="$sectionEl"/>
                    <xsl:with-param name="index" select="$index + 1"/>
                </xsl:apply-templates>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>


    <xsl:template match="span[@class eq 'item']|li[@class = ('closed','open','empty')]"
        mode="ixsl:onclick">
        <xsl:apply-templates select="." mode="handle-itemclick"/>
    </xsl:template>

    <xsl:template match="*" mode="handle-itemclick">
        <xsl:variable name="ids" select="(., ancestor::li)/@id" as="xs:string*"/>
        <xsl:variable name="new-hash" select="string-join($ids, '/')"/>
        <xsl:variable name="isSpan" select="@class eq 'item'" as="xs:boolean"/>
        <!--<xsl:message>new-hash {$new-hash}</xsl:message>-->
        <xsl:for-each select="if ($isSpan) then .. else .">
            <xsl:choose>
                <xsl:when test="@class eq 'open' and not($isSpan)">
                    <ixsl:set-attribute name="class" select="'closed'"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:sequence select="js:disableScroll()"/>
                    <xsl:choose>
                        <xsl:when test="f:get-hash() eq $new-hash">
                            <xsl:variable name="new-class" select="f:get-open-class(@class)"/>
                            <ixsl:set-attribute name="class" select="$new-class"/>
                            <xsl:if test="empty(ul)">
                                <xsl:call-template name="process-hashchange"/>
                            </xsl:if>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:sequence select="f:set-hash($new-hash)"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="li[@class eq 'trail']" mode="ixsl:onclick">
        <xsl:sequence select="f:crumb-navigation(.)"/>
    </xsl:template>

    <xsl:template match="p[@class = ('arrowLeft','arrowRight')]" mode="ixsl:onclick">
        <xsl:sequence select="f:navpage(@class)"/>
    </xsl:template>

    <xsl:function name="f:navpage">
        <xsl:param name="class"/>
        <ixsl:schedule-action wait="16">
            <xsl:call-template name="navpage">
                <xsl:with-param name="class" select="$class"/>
            </xsl:call-template>
        </ixsl:schedule-action>
    </xsl:function>

    <xsl:template name="navpage">
        <xsl:param name="class" as="xs:string"/>
        <!--<xsl:message>navpage</xsl:message>-->
        <xsl:variable name="ids" select="tokenize(f:get-hash(),'/')"/>
        <xsl:variable name="start" select="$ids[1]"/>
        <xsl:variable name="push" as="xs:string">
            <!--<xsl:choose>-->
                <!--<xsl:when test="$start eq 'changes' and count($ids) ge 2">
                    <xsl:variable name="next-cat"
                        select="$docChanges/ch:changes/ch:categories/ch:cat[@name=$ids[2]]/
                        (if ($class eq 'arrowLeft') then preceding-sibling::*[1] else following-sibling::*[1])/@name"/>
                    <xsl:sequence
                        select="concat('changes/', $next-cat, if (count($ids) eq 2) then () else concat('/', $ids[3]))"
                    />
                </xsl:when>-->
                <!--<xsl:otherwise>-->
                    <xsl:variable name="c" as="node()"
                        select="f:get-item($ids, f:get-first-item($start), 1)"/>
                    <xsl:variable name="new-li"
                        select="if ($class eq 'arrowLeft') then
                        ($c/preceding::li[1] union $c/parent::ul/parent::li)[last()]
                        else ($c/ul/li union $c/following::li)[1]"/>

                    <xsl:sequence select="string-join(($new-li/ancestor::li union $new-li)/@id,'/')"
                    />
                <!--</xsl:otherwise>-->
            <!--</xsl:choose>-->
        </xsl:variable>

        <xsl:sequence select="f:set-hash($push)"/>
    </xsl:template>

    <xsl:function name="f:get-item" as="node()">
        <xsl:param name="ids" as="xs:string*"/>
        <xsl:param name="item" as="node()"/>
        <xsl:param name="index" as="xs:integer"/>
        <xsl:choose>
            <xsl:when test="$index eq count($ids)">
                <xsl:sequence select="$item"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="new-item" select="$item/ul/li[@id eq $ids[$index+1]]"/>
                <xsl:sequence select="f:get-item($ids, $new-item, $index + 1)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="f:get-function">
        <xsl:param name="prefix" as="xs:string"/>
        <xsl:param name="local" as="xs:string"/>
        <!-- This function is called from navpage template, when stepping through functions pages, in which case
            the document 'saxonc-extension.xml' has certainly already been loaded -->
        <xsl:sequence
            select="doc(concat($location, '/ixsl-extension.xml'))//fnd:function[fnd:name[. = $local
            and 'ixsl' = $prefix]]"
        />
    </xsl:function>

    <xsl:template name="get-trail">
        <xsl:param name="ids" as="xs:string*"/>
        <xsl:param name="parent" as="node()?"/>
        <xsl:param name="index" as="xs:integer"/>
        <xsl:variable name="section" select="$parent/*[@id eq $ids[$index]]"/>
        <xsl:variable name="title" select="($section/@title, $section/@id, $ids[$index])[1]"/>
        <xsl:choose>
            <xsl:when test="$index gt count($ids)"/>
            <xsl:when test="$index eq count($ids) and empty($section)">
                <!-- $section will be empty when the page is an ixsl-extension function -->
                <xsl:variable name="last" select="$ids[$index]"/>
                <xsl:variable name="function" select="$parent/fnd:function[fnd:name = $last]"/>
                <xsl:variable name="prefix"
                    select="f:usual-prefix($function/fnd:name/@namespace)"/>
                <xsl:variable name="preName" select="if ($prefix ne '') then concat($prefix,
                    ':', $last) else $last"/>
                <li idt="{$last}" class="trail">
                    <xsl:value-of select="$preName"/>
                </li>
            </xsl:when>
            <xsl:when test="$index eq count($ids)">
                <li idt="{$section/@id}" class="trail">
                    <xsl:value-of select="$title"/>
                </li>
            </xsl:when>
            <xsl:otherwise>
                <li idt="{$section/@id}" class="trail">
                    <xsl:value-of select="$title"/> &#x25b7;</li>
                <xsl:call-template name="get-trail">
                    <xsl:with-param name="ids" select="$ids"/>
                    <xsl:with-param name="parent" select="$section"/>
                    <xsl:with-param name="index" select="$index + 1"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="show-listitems">
        <xsl:param name="doc" as="node()"/>
        <xsl:param name="ids"/>
        <xsl:param name="index" as="xs:integer"/>
        <xsl:param name="item" as="node()?"/>
        <xsl:variable name="id" select="$ids[$index]"/>

        <xsl:for-each select="$item">
            <ixsl:set-attribute name="class" select="f:get-open-class(@class)"/>
            <xsl:choose>
                <!--<xsl:when test="$index eq 1 and $item/@id eq 'changes'">
                    <xsl:result-document href="?." method="ixsl:replace-content">
                        <xsl:call-template name="add-list-changes">
                            <xsl:with-param name="top-name" select="span"/>
                            <xsl:with-param name="cat-name" select="$ids[2]"/>
                        </xsl:call-template>
                    </xsl:result-document>
                </xsl:when>-->
                <xsl:when test="exists(ul)">
                    <xsl:if test="$index lt count($ids)">
                        <xsl:call-template name="show-listitems">
                            <xsl:with-param name="doc" select="$doc/*[@id eq $id]"/>
                            <xsl:with-param name="ids" select="$ids"/>
                            <xsl:with-param name="index" select="$index + 1"/>
                            <xsl:with-param name="item" select="ul/li[@id eq $ids[$index + 1]]"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:result-document href="?." method="ixsl:append-content">
                        <xsl:call-template name="add-list">
                            <xsl:with-param name="section" 
                                select="$doc/*[@id eq $id]|$doc/fnd:function[string(fnd:name) eq $id]"/>
                            <xsl:with-param name="ids" select="$ids"/>
                            <xsl:with-param name="index" select="$index"/>
                        </xsl:call-template>
                    </xsl:result-document>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="add-list">
        <!-- This now works for ixsl-extension/functions too: 
            see if test="exists($section/fnd:function)" branch -->
        <xsl:param name="section" as="node()"/>
        <xsl:param name="ids" as="xs:string*"/>
        <xsl:param name="index" as="xs:integer"/>
        <!--<xsl:message>CALL TEMPLATE add-list</xsl:message>
        <xsl:if test="empty($section)">
            <xsl:message>!!!!!!!!!!WARNING section is empty</xsl:message>
        </xsl:if>-->

        <xsl:if test="exists($section/(section|j))">
            <ul>
                <xsl:for-each select="$section/(section|j)">
                    <xsl:variable name="onpath" as="xs:boolean*"
                        select="$index lt count($ids) and @id eq $ids[$index + 1]"/>
                    <xsl:variable name="contains" select="exists(section|j|fnd:function)"/>
                    <li id="{@id}">
                        <xsl:attribute name="class"
                            select="if ($onpath and $contains) then 'open'
                            else if ($contains) then 'closed'
                                    else 'empty'"/>
                        <span class="item">
                            <xsl:value-of select="if (@title) then @title else @id"/>
                        </span>
                        <xsl:if test="$onpath and $contains">
                            <xsl:call-template name="add-list">
                                <xsl:with-param name="section" select="$section/(section|j)[@id = $ids[$index + 1]]"/>
                                <xsl:with-param name="ids" select="$ids"/>
                                <xsl:with-param name="index" select="$index + 1"/>
                            </xsl:call-template>
                        </xsl:if>
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:if>
        <xsl:if test="exists($section/fnd:function)">
            <ul>
                <xsl:for-each select="$section/fnd:function">
                    <!-- Does not have an @id, use value-of name child -->
                    <xsl:variable name="fn-name" select="string(fnd:name)"/>
                    <xsl:variable name="onpath" as="xs:boolean*"
                        select="$index lt count($ids) and $fn-name eq $ids[$index + 1]"/>
                    <xsl:variable name="prefix" select="f:usual-prefix(fnd:name/@namespace)"/>
                    <xsl:variable name="preName" select="if ($prefix ne '') then concat($prefix,
                        ':', $fn-name) else $fn-name"/>
                    <li id="{$fn-name}">
                        <xsl:attribute name="class" select="'empty'"/>
                        <span class="item">
                            <xsl:value-of select="$preName"/>
                        </span>
                    </li>
                    <!-- fnd:function elements do not have section children -->
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>

    <!--<xsl:template name="add-list-changes">
        <xsl:param name="top-name" as="xs:string?"/>
        <xsl:param name="cat-name" as="xs:string?"/>
        <span class="item">
            <xsl:value-of select="$top-name"/>
        </span>
        <xsl:if test="exists($cat-name)">
            <ul>
                <xsl:for-each select="$docChanges/ch:changes/ch:categories/ch:cat">
                    <li id="{@name}" class="empty">
                        <span class="item">
                            <xsl:value-of select="@title"/>
                        </span>
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:if>
    </xsl:template>-->

    <xsl:template name="highlight-item">
        <xsl:param name="parent" as="node()?"/> <!-- navlist from first call -->
        <xsl:param name="ids" as="xs:string*"/>
        <xsl:param name="index" as="xs:integer"/>
        <xsl:variable name="hitem" select="$parent/ul/li[@id eq $ids[$index]]"/>
        <xsl:choose>
            <xsl:when test="$index lt count($ids)">
                <xsl:call-template name="highlight-item">
                    <xsl:with-param name="parent" select="$hitem"/>
                    <xsl:with-param name="ids" select="$ids"/>
                    <xsl:with-param name="index" select="$index + 1"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:for-each select="$hitem/span">
                    <xsl:for-each select="js:swapItem(.)">
                        <ixsl:set-attribute name="class" select="'item'"/>
                    </xsl:for-each>
                    <xsl:sequence select="js:enableScroll()"/>
                    <ixsl:set-attribute name="class" select="'hot'"/>
                </xsl:for-each>
                <!--<xsl:message>style.display of div/@class=found {ixsl:style($navlist/../div[@class eq 'found'])?display}</xsl:message>-->
                <xsl:if test="ixsl:style(ixsl:page()/html/body/div/div[@class eq 'found'])?display ne 'none'">
                    <!--<xsl:sequence select="f:highlight-finds()"/>-->
                    <xsl:call-template name="highlighting"/>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:function name="f:crumb-navigation">
        <xsl:param name="c" as="node()"/>
        <xsl:variable name="seq" select="$c/preceding-sibling::*/@idt|$c/@idt" as="xs:string*"/>
        <xsl:variable name="new-hash" select="string-join($seq,'/')"/>
        <xsl:sequence select="f:set-hash($new-hash)"/>
    </xsl:function>

    <xsl:function name="f:anchor-navigation">
        <xsl:param name="c" as="node()"/>
        <xsl:variable name="href">
            <xsl:variable name="ahref"
                select="resolve-uri($c/@data-href, concat('http://a.com/', f:get-hash(),'/'))"/>
            <xsl:value-of select="substring(string($ahref), 14)"/>
        </xsl:variable>
        <xsl:sequence select="f:set-hash(translate($href, '#','@'))"/>
    </xsl:function>

    <xsl:function name="f:set-hash">
        <xsl:param name="hash"/>
        <!--<xsl:message>f:set-hash {$hash}</xsl:message>-->
        <ixsl:set-property name="location.hash" select="concat('!',$hash)"/>
    </xsl:function>

    <xsl:function name="f:get-open-class" as="xs:string">
        <xsl:param name="class" as="xs:string"/>
        <xsl:sequence select="if ($class eq 'empty') then 'empty' else 'open'"/>
    </xsl:function>

    <xsl:function name="f:get-first-item" as="node()?">
        <xsl:param name="start"/>
        <xsl:sequence select="$navlist/ul/li[@id = $start]"/>
    </xsl:function>

    <!-- hash is prefixed with ! as the 'hashbang' SEO measure: eg. http:/a.com#!about/gwt -->
    <xsl:function name="f:get-hash">
        <xsl:variable name="url" select="ixsl:location()"/>
        <!--<xsl:message>current url {$url}</xsl:message>-->
        <xsl:variable name="hash"
            select="if(contains($url, '%21')) then substring-after($url, '%21') else substring(ixsl:get(ixsl:window() , 'location.hash'),3)"/>
        <xsl:sequence select="if (string-length($hash) gt 0) then $hash else ($navlist/ul/li)[1]/@id"/>
    </xsl:function>

</xsl:transform>
