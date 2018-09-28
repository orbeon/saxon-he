<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema" version="3.0" exclude-result-prefixes="xs"
    expand-text="yes">

   <xsl:param name="in" required="yes"/>
    
   <xsl:output method="html" version="5"/> 

   <xsl:template name="xsl:initial-template">
     <html>
       <head>
          <title>Index of Cities</title>
       </head>
       <body>
         <h1>Index of Cities</h1>
         <p>
           <xsl:for-each-group select="unparsed-text-lines($in)!parse-json(.)"
                        group-by="?name => substring(1,1)">
             <xsl:sort select="current-grouping-key()"/>                       
             { " | "[position() != 1]} 
             <a href="cities.html?select={current-grouping-key()}">{current-grouping-key()}</a>                      
             <xsl:result-document href="cities/{current-grouping-key()}.json" method="text">
                [ {string-join(current-group() ! serialize(., map{"method":"json"}), ",&#xa;")} ]
             </xsl:result-document>
           </xsl:for-each-group>
         </p>
       </body>
     </html>
   </xsl:template>

</xsl:transform>
