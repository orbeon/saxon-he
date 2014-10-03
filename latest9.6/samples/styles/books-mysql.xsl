<xsl:stylesheet
    xmlns:sql="http://saxon.sf.net/sql"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:saxon="http://saxon.sf.net/"
    extension-element-prefixes="saxon sql">
    
    <!-- insert your database details here, or supply them in parameters -->
    <xsl:param name="driver" select="'com.mysql.jdbc.Driver'"/>
    <xsl:param name="database" select="'jdbc:mysql://localhost/Book'"/>  
    <xsl:param name="user" select="'ond1'"/>
    <xsl:param name="password"/>
    
    <!-- This stylesheet writes the book list to a SQL database -->
    
    <xsl:variable name="count" select="0" saxon:assignable="yes"/>
    
    <xsl:output method="xml" indent="yes"/>
    
    <xsl:template match="BOOKLIST">
        <xsl:if test="not(element-available('sql:connect'))">
            <xsl:message>sql:connect is not available</xsl:message>
        </xsl:if>
        
        <xsl:message>Connecting to <xsl:value-of select="$database"/>...</xsl:message>
        
        <xsl:variable name="connection" 
            as="java:java.sql.Connection" xmlns:java="http://saxon.sf.net/java-type">   
            <sql:connect driver="{$driver}" database="{$database}" 
                user="{$user}" password="{$password}">
                <xsl:fallback>
                    <xsl:message terminate="yes">SQL extensions are not installed</xsl:message>
                </xsl:fallback>
            </sql:connect>
        </xsl:variable>
        
        <xsl:message>Connected...</xsl:message>
        
        <xsl:apply-templates select="BOOKS">
            <xsl:with-param name="connection" select="$connection"/>
        </xsl:apply-templates>
        
        <xsl:message>Inserted <xsl:value-of select="$count"/> records.</xsl:message>
        
        <xsl:variable name="book-table">
            <sql:query connection="$connection" table="Book" column="*" row-tag="book" column-tag="col"/> 
        </xsl:variable>
        
        <xsl:message>There are now <xsl:value-of select="count($book-table//Book)"/> books.</xsl:message>
        <new-book-table>
            <xsl:copy-of select="$book-table"/>
        </new-book-table>
        
        <sql:close connection="$connection"/>
        
    </xsl:template>
    
    <xsl:template match="BOOKS">
        <xsl:param name="connection"/>
        <xsl:for-each select="ITEM">
            <sql:insert connection="$connection" table="Book">
                <sql:column name="title" select="TITLE"/>
                <sql:column name="author" select="AUTHOR"/>
                <sql:column name="category" select="@CAT"/>
            </sql:insert>
            <saxon:assign name="count" select="$count+1"/>
        </xsl:for-each>
    </xsl:template>
    
</xsl:stylesheet>	