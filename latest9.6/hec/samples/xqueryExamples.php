<!DOCTYPE html SYSTEM "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <title>Saxon/C API design use cases</title>
    </head>
    <body>
        <?php 
            
            /* simple example to show transforming to string */
            function exampleSimple1($proc, $xmlfile, $queryFile){
                $proc->setSourceFile($xmlfile);
                $proc->setQueryFile($queryFile);
  	        //$proc->setProperty('base', '/');      
                $result = $proc->queryToString();               
		if($result != null) {               
		echo '<b>exampleSimple1:</b><br/>';		
		echo 'Output:'.$result;
		} else {
			echo "Result is null";
		}
		$proc->clearParameters();
		$proc->clearProperties();            
            }
            
            /* simple example to show transforming to file */
            function exampleSimple2($proc){
		$sourceNode = $proc->parseString("<foo xmlns='http://one.uri/'><bar><b>text node in example</b></bar></foo>");
		if($sourceNode !=null){
			/*echo "Name of Class " , get_class($sourceNode) , "\n"; 			
			$str = $xdmvalue->getStringValue();
			if($str!=null) {
				echo "XdmValue:".$str;
			} */
			$proc->setSourceValue($sourceNode);
		} else {
			echo "Xdmvalue is null";
		}
                $proc->setQueryContent("declare default element namespace 'http://one.uri/'; /foo");
                $result = $proc->queryToString();
		echo '<b>exampleSimple2:</b><br/>';		
		if($result != null) {               
		  echo 'Output:'.$result;
		} else {
			echo "Result is null";
		}
       		$proc->clearParameters();
		$proc->clearProperties();
            }

            
            
            $books_xml = "query/books.xml";
            $books_to_html_xq = "query/books-to-html.xq";
            $baz_xml = "xml/baz.xml";
            $baz_xsl = "xsl/baz.xsl";
            $foo2_xsl = "xsl/foo2.xsl";
            $foo3_xsl = "xsl/foo3.xsl";
            $err_xsl = "xsl/err.xsl";            
            $err1_xsl = "xsl/err1.xsl";
            $text_xsl = "xsl/text.xsl";
            $cities_xml = "xml/cities.xml";
            $embedded_xml = "xml/embedded.xml";
            $multidoc_xsl = "xsl/multidoc.xsl";
            $identity_xsl = "xsl/identity.xsl"; 
            
            $proc = new SaxonProcessor();
		
		
            $version = $proc->version();
   	    echo '<b>PHP XQuery examples</b><br/>';
            echo 'Saxon Processor version: '.$version;
            echo '<br/>';        
            exampleSimple1($proc, $books_xml, $books_to_html_xq);
            echo '<br/>';
            exampleSimple2($proc);
            echo '<br/>';            
           
            unset($proc);
	
        
        ?>
    </body>
</html>
