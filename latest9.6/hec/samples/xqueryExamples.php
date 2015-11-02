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
                $proc->importXQueryFile($queryFile);
  	              
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
            function exampleSimple2($proc, $xmlFile, $xslFile){
                $proc->setSourceFile($xmlFile);
                $proc->importStylesheetFile($xslFile);
                $filename = "out/output1.xml";
                $proc->transformToFile($filename);
		echo '<b>exampleSimple2:</b><br/>';		
		if (file_exists($filename)) {
		    echo "The file $filename exists";
		} else {
		    echo "The file $filename does not exist";
		}
       		$proc->clearParameters();
		$proc->clearProperties();
            }
            /* simple example to show importing a document as string and stylesheet as a string */
            function exampleSimple3($proc){
		$proc->clearParameters();
		$proc->clearProperties();
                $xdmNode = $proc->parseString("<doc><b>text value of out</b></doc>");
		if($xdmNode == null) {
			echo 'xdmNode is null';
			return;	
		}            
		$proc->setSourceValue($xdmNode);
                $proc->importStylesheetString("<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>
					    	<xsl:template match='/'>
					    	    <xsl:copy-of select='.'/>
					    	</xsl:template>
					    </xsl:stylesheet>");

                $result = $proc->transformToString();
		echo '<b>exampleSimple3</b>:<br/>';
		if($result != null) {             		
		echo 'Output:'.$result;
		} else {
			echo "Result is null";
		}       
		$proc->clearParameters();
		$proc->clearProperties();            
            }


           
           
            function exampleParam($proc, $xmlFile, $xslFile){
                echo '<b>ExampleParam:</b><br/>';
                $proc->setSourceFile($xmlFile);
                $proc->importStylesheetFile($xslFile);
		$xdmvalue = $proc->createXdmValue(strval("Hello to you"));
		if($xdmvalue !=null){
			echo "Name of Class " , get_class($xdmvalue) , "\n"; 			
			/*$str = $xdmvalue->getStringValue();
			if($str!=null) {
				echo "XdmValue:".$str;
			} */
			$proc->setParameter("", 'a-param', $xdmvalue);
		} else {
			echo "Xdmvalue is null";
		}
                $result = $proc->transformToString();
		if($result != null) {                
			echo 'Output:'.$result."<br/>";
		} else {
			echo "Result is NULL<br/>";
		}
                //$par1 = $proc->getParameter('a-param'); //TODO there is a bug in this method
                if($par1 != NULL) {
                    echo 'Parameter exists';
                }
                //unset($par1);
                $proc->clearParameters();                
                //unset($result);
                echo 'again with a no parameter value<br/>';
		$proc->setProperty('!indent', 'yes'); //Serialization property indicated with a '!' symbol
                $result = $proc->transformToString();
                /*$prop1 = $proc->getProperty('!indent'); //TODO there is a bug in this method
                if($prop1 != NULL) {
                    echo 'getProperty = '.$prop1;
                }*/
                $proc->clearProperties();
                echo $result;
                echo '<br/>';
              //  unset($result);
                echo 'again with no parameter and no properties value set<br/>';
                $xdmvalue = $proc->createXdmValue(strval("goodbye to you"));
		$proc->setParameter('a-param', $xdmvalue);
                $result = $proc->transformToString();                
                echo $result;
		$proc->clearParameters();
		$proc->clearProperties(); 
                        
            }


            function exampleXMLFilterChain($proc, $xmlFile, $xsl1File, $xsl2File, $xsl3File){
                echo '<b>XML Filter Chain using setSource</b><br/>';                
                $proc->setSourceFile($xmlFile);
                $proc->importStylesheetFile($xsl1File);
                $xdmValue1 = $proc->transformToValue();
                
                $proc->importStylesheetFile($xsl2File);
                $proc->setSourceValue($xdmValue1);
                unset($xdmValue1);
                $xdmValue1 = $proc->transformToValue();
                
                $proc->importStylesheetFile($xsl3File);                
                $proc->setSourceValue($xdmValue1);
                $result = $proc->transformToString();
		if($result != null) {
                	echo 'Output:'.$result;        
		} else {
			echo 'Result is null';
				    $errCount = $proc->getExceptionCount();
				    if($errCount > 0 ){ 
				        for($i = 0; $i < $errCount; $i++) {
					       $errCode = $proc->getErrorCode(intval($i));
					       $errMessage = $proc->getErrorMessage(intval($i));
					       echo 'Expected error: Code='.$errCode.' Message='.$errMessage;
					   }
						$proc->exceptionClear();	
					}
		}                      
		$proc->clearParameters();
		$proc->clearProperties();
            }
            
            function exampleXMLFilterChain2($proc, $xmlFile, $xsl1File, $xsl2File, $xsl3File){
                echo '<b>XML Filter Chain using Parameters</b><br/>';                
                $xdmNode = $proc->parseFile($xmlFile);
                $proc->setParameter('s', $xdmNode);
                $proc->importStylesheetFile($xsl1File);
                $xdmValue1 = $proc->transformToValue();
                $proc->clearParameters();
                
                $proc->importStylesheetFile($xsl2File);
                $proc->setParameter('s',$xdmValue1);
                $xdmValue1 = $proc->transformToValue();
                $proc->clearParameters();
                
                $proc->importStylesheetFile($xsl3File);                
                $proc->setParameter('s', $xdmValue1);
                $result = $proc->transformToString();
		if($result != null) {
                	echo 'Output:'.$result;        
		} else {
			echo 'Result is null';
				    $errCount = $proc->getExceptionCount();
				    if($errCount > 0 ){ 
				        for($i = 0; $i < $errCount; $i++) {
					       $errCode = $proc->getErrorCode(intval($i));
					       $errMessage = $proc->getErrorMessage(intval($i));
					       echo 'Expected error: Code='.$errCode.' Message='.$errMessage;
					   }
						$proc->exceptionClear();	
					}
		}        
		$proc->clearParameters();
		$proc->clearProperties();
            }            

            /* simple example to detect and handle errors from a transformation */
            function exampleError1($proc, $xmlfile, $xslFile){
		echo '<b>exampleError1:</b><br/>';
                $proc->setSourceFile($xmlFile);
                $proc->importStylesheetFile($xslFile);
                
                $result = $proc->transformToString();
                
                if($result == NULL) {
                    $errCount = $proc->getExceptionCount();
				    if($errCount > 0 ){ 
				        for($i = 0; $i < $errCount; $i++) {
					       $errCode = $proc->getErrorCode(intval($i));
					       $errMessage = $proc->getErrorMessage(intval($i));
					       echo 'Expected error: Code='.$errCode.' Message='.$errMessage;
					   }
						$proc->exceptionClear();	
					}
                
                
                }                
                echo $result;
            	$proc->clearParameters();
		$proc->clearProperties();
            
            }   


            /* simple example to test transforming without an stylesheet */
            function exampleError2($proc, $xmlfile, $xslFile){
		echo '<b>exampleError2:</b><br/>';
                $proc->setSourceFile($xmlFile);
                $proc->importStylesheetFile($xslFile);
                
                $result = $proc->transformToString();
                
                if($result == NULL) {
                    $errCount = $proc->getExceptionCount();
				    if($errCount > 0 ){ 
				        for($i = 0; $i < $errCount; $i++) {
					       $errCode = $proc->getErrorCode(intval($i));
					       $errMessage = $proc->getErrorMessage(intval($i));
					       echo 'Expected error: Code='.$errCode.' Message='.$errMessage;
					   }
						$proc->exceptionClear();	
					}
                
                
                }                
                echo $result;            
           	$proc->clearParameters();
		$proc->clearProperties();
            
            }   
            
            
            $books_xml = "xml/books.xml";
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
/*            echo '<br/>';
            exampleSimple2($proc, "xml/foo.xml", $foo_xsl);
            echo '<br/>';            
            exampleSimple3($proc);
            echo '<br/>';
            exampleParam($proc, $foo_xml, $foo_xsl);
            exampleError1($proc, $foo_xml, $err_xsl);
            echo '<br/>'; 
	    exampleError2($proc, $foo_xml, $err1_xsl);
            echo '<br/>';
            exampleXMLFilterChain($proc, $foo_xml, $foo_xsl, $foo2_xsl, $foo3_xsl);
            echo '<br/>';                    
            exampleXMLFilterChain2($proc, $foo_xml, $foo_xsl, $foo2_xsl, $foo3_xsl);          
            echo '<br/>'; */ 

//	    $proc->close();            
            unset($proc);
	
        
        ?>
    </body>
</html>
