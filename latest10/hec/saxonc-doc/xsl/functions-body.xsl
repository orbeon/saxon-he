<?xml version="1.0" encoding="utf-8"?>
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:ixsl="http://saxonica.com/ns/interactiveXSLT"
    xmlns:fnd="http://www.saxonica.com/ns/doc/functions"
    xmlns:f="urn:viewerapp.function"
    exclude-result-prefixes="xs fnd"
    extension-element-prefixes="ixsl"
    version="3.0">
    
    <!-- For Saxon-JS documentation: 
        - used to produce ixsl-extension functions pages (in browser),
            from doc/saxonc-extension.xml (contains fnd:functions elements) -->
    
    <!-- Modes used: 
            f - for main functions body content
            function-list - for namespace main page (ixsl-extension/functions page)
                - initially called from body.xsl (on section element)
            index - for namespace function list (on ixsl-extension/functions page)
            fn-description - description in hover box (on ixsl-extension/functions page)
                - initially called from jstree.xsl (on span[@data-href][@class eq 'flink'])
            secondary - for mixed content of span links
            
            (note unnamed mode not used)
            -->
    
    
    <!--  Function pages for each function. -->
    <xsl:template match="fnd:function" mode="f">
        
        <xsl:variable name="namespace" select="ancestor::section/@id"/>
        <xsl:variable name="implement" select="doc($implement-doc)" as="document-node()"/>
        <xsl:variable name="default.saxon"
            select="$implement/implement/default[@id = $namespace]/@saxon"/>
        
        <ixsl:set-property name="document.title" select="concat('Saxon-JS ', f:fn-name(.))"/>
        
        <h1>
            <xsl:value-of select="f:fn-name(.)"/>
        </h1>
        
        <xsl:apply-templates select="fnd:description" mode="f"/>
        
        <aside>
            <xsl:value-of select="f:edition((fnd:saxon-edition, $default.saxon, 'default')[1], $implement)"/>
        </aside>
        
        <xsl:apply-templates select="fnd:signatures" mode="f"/>
        
        <h3 class="subtitle">Namespace</h3>
        <p>
            <xsl:value-of select="fnd:name/@namespace"/>
        </p>
        
        <xsl:apply-templates select="fnd:status" mode="f"/>
        <xsl:apply-templates select="fnd:details" mode="f"/>
        <xsl:apply-templates select="fnd:examples" mode="f"/>
        
        <xsl:if test="fnd:see-also">
            <h3 class="subtitle">See also:</h3>
            <xsl:apply-templates select="fnd:see-also" mode="f"/>
        </xsl:if>
        
    </xsl:template>
    
    
    <!-- Signature formats. -->
    <xsl:template match="signatures" mode="f"
        xpath-default-namespace="http://www.saxonica.com/ns/doc/functions">
        
        <xsl:for-each select="proto">
            <xsl:variable name="MyProto" select="."/>
            
            <p class="fn-sig">
                <xsl:value-of
                    select="concat(../../name,'(',string-join(for $arg in arg return concat('$',$arg/@name, ' as ', $arg/@type), ', ') ,')', ' &#x2794;  ',@return-type)"
                />
            </p>
            <!-- Note that don't currently have any signatures/proto/description in
                ixsl-extensions -->
            <xsl:apply-templates select="description" mode="f"/>
            <table class="fn-prototype" style="margin-bottom:10px;">
                <tr>
                    <td width="470" align="left" colspan="4" style="border-top:solid 1px;">
                        <p>
                            <i>
                                <xsl:value-of select="if (exists(arg)) then 'Arguments' else 'There are no arguments'"/>
                            </i>
                        </p>
                    </td>
                </tr>
                <xsl:for-each select="arg">
                    <tr>
                        <td width="40">
                            <p>&#xa0;</p>
                        </td>
                        <td width="80" valign="top">
                            <p>$<xsl:value-of select="@name"/></p>
                        </td>
                        <td valign="top" width="150">
                            <p>
                                <xsl:value-of select="@type"/>
                            </p>
                        </td>
                        <td valign="top" width="200">
                            <p>
                                <xsl:value-of select="@desc"/>
                            </p>
                        </td>
                    </tr>
                </xsl:for-each>
                <tr>
                    <td colspan="2" style="border-top:solid 1px; border-bottom:solid 1px;">
                        <p>
                            <i>Result</i>
                        </p>
                    </td>
                    
                    <td style="border-top:solid 1px #3D5B96; border-bottom:solid 1px;" colspan="2">
                        <p>
                            <xsl:value-of select="$MyProto/@return-type"/>
                        </p>
                    </td>
                    
                </tr>
            </table>
            
        </xsl:for-each>
    </xsl:template>
    
    
    <!-- Description in the hover box. -->
    <xsl:template match="fnd:description" mode="fn-description">
        <div>
            <xsl:apply-templates mode="f"/>
        </div>
    </xsl:template>
    
    <!-- Description in the hover box, when there are multiple signatures. -->
    <xsl:template match="fnd:signatures" mode="fn-description">
        <xsl:if test="fnd:proto/fnd:description">
            <ol>
                <xsl:for-each select="fnd:proto/fnd:description">
                    <li>
                        <xsl:apply-templates mode="f"/>
                    </li>
                </xsl:for-each>
            </ol>
        </xsl:if>
    </xsl:template>
    
    <!-- Namespace main page. -->
    <xsl:template match="section" mode="function-list">
        <xsl:variable name="namespace" select="fnd:function[1]/fnd:name/@namespace"/>
        <ixsl:set-property name="document.title" select="concat('Saxon-JS ', @title)"/>
        
        <!-- Default content of hover box. -->
        <div id="fn-desc">
            <h4>Functions in namespace <xsl:value-of select="$namespace"/></h4>
            <p>
                <i>Hover over a listed function to view a brief description. For full details, select the
                    function entry.</i>
            </p>
        </div>
        
        <!-- div for namespace functions list, on main page (scrollable section). -->
        <!-- Warning: apparently browser support for HTMLElement.offsetHeight varies -->
        <xsl:variable xpath-default-namespace="" name="height"
            select="ixsl:get(ixsl:page()/html/body/div[@id = 'wrap']/div[@id = 'main'], 'offsetHeight') - 140"/>
        
        <div style="overflow:auto; height:{$height}px; margin-top:5px;margin-bottom:5px">
            
            <!-- Namespace functions list, on main page. -->
            <xsl:variable name="sfns" as="element()*" select="f:fn-list(fnd:function)"/>
            
            <p>Number of functions: <xsl:value-of select="count($sfns)"/></p>
            
            <xsl:variable name="colsize" select="xs:integer(ceiling(count($sfns) div 3))"/>
            
            <table class="fn-list-by-ns">
                <tr>
                    <td valign="top">
                        <p>
                            <xsl:apply-templates select="$sfns[(position() - 1) idiv $colsize eq 0]" mode="index"/>
                        </p>
                    </td>
                    <td valign="top">
                        <p>
                            <xsl:apply-templates select="$sfns[(position() - 1) idiv $colsize eq 1]" mode="index"/>
                        </p>
                    </td>
                    <td valign="top">
                        <p>
                            <xsl:apply-templates select="$sfns[(position() - 1) idiv $colsize eq 2]" mode="index"/>
                        </p>
                    </td>
                </tr>
            </table>
            
        </div>
        
        <!-- Implementation note at the bottom of namespace page. -->
        <p class="small-note">The information in this section indicates which functions are implemented
            in this Saxon release, and any restrictions in the current implementation.
        </p>
        
    </xsl:template>
    
    <!-- Contents of namespace function list: links to each function in namespace. -->
    <xsl:template match="fnd:function" mode="index">
        <span class="flink" data-href="{fnd:name}" data-ns="'ixsl'">
            <xsl:value-of select="fnd:name"/>
        </span>
        <br/>
    </xsl:template>
    

</xsl:transform>
