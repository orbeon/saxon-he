<?php
declare(strict_types=1);

use PHPUnit\Framework\TestCase;

final class XSLT30Test extends TestCase
{

    protected static $saxonProc;
    
    public static function setUpBeforeClass(): void
    {
        self::$saxonProc = new Saxon\SaxonProcessor(false);
    }

     
    public function testCreateXslt30ProcessorObject(): void
    {
	
	    	
        $proc = self::$saxonProc->newXslt30Processor();   
	    

        $this->assertInstanceOf(
            Saxon\Xslt30Processor::class,
            $proc
        );
    }

  /*  public function testCannotBeCreatedFromInvalidEmailAddress(): void
    {
        $this->expectException(InvalidArgumentException::class);

        Email::fromString('invalid');
    }*/

    public function testVersion(): void
    {
        $this->assertStringContainsString(
            'Saxon/C 1.2.0 running',
             self::$saxonProc->version()
        );
	$this->assertStringContainsString('9.9.1.5J from Saxonica', self::$saxonProc->version());
    }




public function testContextNotRoot(): void {
       
            $trans = self::$saxonProc->newXslt30Processor();   
			$node = self::$saxonProc->parseXmlFromString("<doc><e>text</e></doc>");

            $trans->compileFromString("<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:variable name='x' select='.'/><xsl:template match='/'>errorA</xsl:template><xsl:template match='e'>[<xsl:value-of select='name(\$x)'/>]</xsl:template></xsl:stylesheet>");
			$this->assertNotNull($node);
			 $this->assertInstanceOf(
            Saxon\XdmNode::class,
            $node
        );			
	    $this->assertTrue($node->getChildCount()>0);
      	    $this->assertNotNull($node);
	    $eNode = $node->getChildNode(0)->getChildNode(0);
	    $this->assertNotNull($eNode);
	    $trans->setGlobalContextItem($node);
	    $trans->setInitialMatchSelection($eNode);
            $result = $trans->applyTemplatesReturningString();
		
            $this->assertStringContainsString("[", $result);

       
    }


    public function testResolveUri(): void {

            $transformer = self::$saxonProc->newXslt30Processor();

            $transformer->compileFromString("<xsl:stylesheet version='3.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform' xmlns:xs='http://www.w3.org/2001/XMLSchema' xmlns:err='http://www.w3.org/2005/xqt-errors'><xsl:template name='go'><xsl:try><xsl:variable name='uri' as='xs:anyURI' select=\"resolve-uri('notice trailing space /out.xml')\"/> <xsl:message select='\$uri'/><xsl:result-document href='{\$uri}'><out/></xsl:result-document><xsl:catch><xsl:sequence select=\"'\$err:code: ' || \$err:code  || ', \$err:description: ' || \$err:description\"/></xsl:catch></xsl:try></xsl:template></xsl:stylesheet>");


            $value = $transformer->callTemplateReturningValue("go");
	    
	    $item = $value->getHead();
            $this->assertStringContainsString("code", $item->getStringValue());

    }


    public function testEmbeddedStylesheet(): void {

            $transformer = self::$saxonProc->newXslt30Processor();

            // Load the source document
            $input = self::$saxonProc->parseXmlFromFile("../data/books.xml");
            //Console.WriteLine("=============== source document ===============");
            //Console.WriteLine(input.OuterXml);
            //Console.WriteLine("=========== end of source document ============");

            // Navigate to the xml-stylesheet processing instruction having the pseudo-attribute type=text/xsl;
            // then extract the value of the href pseudo-attribute if present

            $path = "/processing-instruction(xml-stylesheet)[matches(.,'type\\s*=\\s*[''\"\"]text/xsl[''\" \"]')]/replace(., '.*?href\\s*=\\s*[''\" \"](.*?)[''\" \"].*', '$1')";

            $xPathProcessor = self::$saxonProc->newXPathProcessor();
            $xPathProcessor->setContextItem($input);
            $hrefval = $xPathProcessor->evaluateSingle($path);
	    $this->assertNotNull($hrefval);
            $href = $hrefval->getStringValue();
	    $this->assertNotEquals($href, "");
                // The stylesheet is embedded in the source document and identified by a URI of the form "#id"

            $transformer->compileFromFile($href);


 	    $this->assertInstanceOf(Saxon\XdmNode::class,$input);		
            // Run it
            $node = $transformer->transformToValue($input);
	    if($transformer->getExceptionCount()>0){
		echo $transformer->getErrorMessage(0);
		}
            $this->assertNotNull($node);

	   if($transformer->getExceptionCount()>0){
		echo $transformer->getErrorMessage(0);
	    }

    }


    public function testContextNotRootNamedTemplate(): void {

            $transformer = self::$saxonProc->newXslt30Processor();
            $node = self::$saxonProc->parseXmlFromString("<doc><e>text</e></doc>");

           
            $transformer->compileFromString("<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'><xsl:variable name='x' select='.'/><xsl:template match='/'>errorA</xsl:template><xsl:template name='main'>[<xsl:value-of select='name(\$x)'/>]</xsl:template></xsl:stylesheet>");          
	    $transformer->setGlobalContextItem($node);
            $result = $transformer->callTemplateReturningValue("main");
	    if($transformer->getExceptionCount()>0){
		echo $transformer->getErrorMessage(0);
	    }
           
	    $this->assertNotNull($result);
		if($result->getHead()!= NULL) {
           		$this->assertStringContainsString("[]", $result->getHead()->getStringValue());
		}



  	    $result2 = $transformer->callTemplateReturningString("main");
	    if($transformer->getExceptionCount()>0){
		echo $transformer->getErrorMessage(0);
		}
           
	    $this->assertNotNull($result2);
    	    $this->assertStringContainsString("[]", $result2);

    }


  
    public function testUseAssociated(): void {

            $transformer = self::$saxonProc->newXslt30Processor();

            
            $foo_xml = "trax/xml/foo.xml";
            $transformer->compileFromAssociatedFile($foo_xml);
	    $transformer->setInitialMatchSelectionAsFile($foo_xml);
	    $result = $transformer->applyTemplatesReturningString();

	    $this->assertNotNull($result);

    }


    public function testNullStylesheet(): void {

         $transformer = self::$saxonProc->newXslt30Processor();
         $result = $transformer->applyTemplatesReturningString();

        $this->assertNull($result);
    }


    public function testXdmDestination(): void {
         $transformer = self::$saxonProc->newXslt30Processor();
         $transformer->compileFromString("<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>".
                    "<xsl:template name='go'><a/></xsl:template>".
                    "</xsl:stylesheet>");

          $root = $transformer->callTemplateReturningValue("go");
	  $this->assertNotNull($root);
	  $this->assertNotNull($root->getHead()); 
	   $node  = $root->getHead()->getNodeValue();
          $this->assertTrue($node->getNodeKind() == 9, "result is document node");
    }

    public function testXdmDestinationWithItemSeparator(): void {
         $transformer = self::$saxonProc->newXslt30Processor();
         $transformer->compileFromString("<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>".
                            "<xsl:template name='go'><xsl:comment>A</xsl:comment><out/><xsl:comment>Z</xsl:comment></xsl:template>".
                            "<xsl:output method='xml' item-separator='§'/>".
                            "</xsl:stylesheet>");

            $root = $transformer->callTemplateReturningValue("go");
	    $node  = $root->getHead()->getNodeValue();
            $this->assertEquals("<!--A-->§<out/>§<!--Z-->", $node);
            $this->assertTrue($node->getNodeKind() == 9, "result is document node");
        

    }


    public function testPipeline(): void {
         $transformer = self::$saxonProc->newXslt30Processor();
        
         $xsl = "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>".
                    "<xsl:template match='/'><a><xsl:copy-of select='.'/></a></xsl:template>".
                    "</xsl:stylesheet>";
         $xml = "<z/>";
            String[] params = new String[]{"!indent"};
            Object[] values = new Object[]{"no"};

          $stage1 = self::$saxonProc->newXslt30Processor();
          $stage1->compileFromString($xsl);
          $in = self::$saxonProc->parseXmlString($xml);

          $stage2 = self::$saxonProc->newXslt30Processor();
          $stage2->compileFromString($xsl);
	  $stage3 = self::$saxonProc->newXslt30Processor();
          $stage3->compileFromString($xsl);

	  $stage4 = self::$saxonProc->newXslt30Processor();
          $stage4->compileFromString($xsl);

	  $stage5 = self::$saxonProc->newXslt30Processor();
          $stage5->compileFromString($xsl);

	  $stage1->setProperty("!indent", "no");
          $d1 = $stage1->applyTemplatesReturningValue(in);
	  $stage2->setProperty("!indent", "no");
          $d2 = $stage2->applyTemplatesReturningValue(d1);
	  $stage3->setProperty("!indent", "no");
          $d3 = stage3.applyTemplatesReturningValue(d2);
	  $stage4->setProperty("!indent", "no");
          $d4 = stage4.applyTemplatesReturningValue(d3);
	  $stage5->setProperty("!indent", "no");
          $sw = stage5.applyTemplatesReturningString(d4);
          $this->assertStringContainsString($sw, "<a><a><a><a><a><z/></a></a></a></a></a>");

         }
/*

    public function testPipelineShort(): void {
        try {
            Processor processor = new Processor(true);

            String xsl = "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                    "<xsl:template match='/'><a><xsl:copy-of select='.'/></a></xsl:template>" +
                    "</xsl:stylesheet>";
            String xml = "<z/>";
            Xslt30Processor stage1 = new Xslt30Processor(processor);

            Xslt30Processor stage2 = new Xslt30Processor(processor);


            stage1.createStylesheetFromString(null, xsl, null, null);
            stage2.createStylesheetFromString(null, xsl, null, null);
            String[] params = new String[]{"!omit-xml-declaration"};
            Object[] values = new Object[]{"yes"};

            XdmValue out = stage1.applyTemplatesReturningValue(null, stage1.parseXmlString(xml), null, params, values);
            String sw = stage2.applyTemplatesReturningString(null, out, null, params, values);
            System.err.println(sw);
            assertTrue("output", sw.contains("<a><a><z/></a></a>"));

        } catch (SaxonApiException err) {
            err.printStackTrace();
            fail(err.getMessage());
        }
    }

    public function testSchemaAware11(): void {
        // Create a Processor instance.
        try {

            Processor proc = new Processor(true);
            proc.setConfigurationProperty(FeatureKeys.XSD_VERSION, "1.1");
            Xslt30Processor transformer = new Xslt30Processor(proc);
            transformer.createStylesheetFromString(null,
                    "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                            "<xsl:import-schema><xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                            "<xs:element name='e'><xs:complexType><xs:sequence><xs:element name='p'/></xs:sequence><xs:assert test='exists(p)'/></xs:complexType></xs:element>" +
                            "</xs:schema></xsl:import-schema>" +
                            "<xsl:variable name='v'><e><p/></e></xsl:variable>" +
                            "<xsl:template name='main'><xsl:copy-of select='$v' validation='strict'/></xsl:template>" +
                            "</xsl:stylesheet>", null, null);


            String[] params = new String[]{"!indent"};
            Object[] values = new Object[]{"no"};

            String sw = transformer.callTemplateReturningString(null, null,  "main", params, values);
            assertTrue(sw.contains("<e>"));
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }


    public function testCallFunction() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                xmlns:f='http://localhost/'" +
                    "                version='3.0'>" +
                    "                <xsl:function name='f:add' visibility='public'>" +
                    "                  <xsl:param name='a'/><xsl:param name='b'/>" +
                    "                  <xsl:sequence select='$a + $b'/></xsl:function>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            XdmValue v = transformer.callFunctionReturningValue(null, null, "{http://localhost/}add", new XdmValue[]{new XdmAtomicValue(2), new XdmAtomicValue(3)}, null, null);
            assertEquals(((XdmAtomicValue) v).getLongValue(), 5);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

  
  public function testCallFunctionArgConversion() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                xmlns:f='http://localhost/'" +
                    "                version='3.0'>" +
                    "                <xsl:function name='f:add' visibility='public'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:double'/>" +
                    "                   <xsl:sequence select='$a + $b'/>" +
                    "                </xsl:function>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            XdmValue v = transformer.callFunctionReturningValue(null, null, "{http://localhost/}add", new XdmValue[]{new XdmAtomicValue(2), new XdmAtomicValue(3)}, null, null);
            assertEquals(((XdmAtomicValue) v).getDoubleValue(), 5.0e0);
            assertEquals(((XdmAtomicValue) v).getPrimitiveTypeName().getLocalName(), "double");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    public function testCallFunctionWrapResults() throws SaxonApiException {
        try {
            Processor processor = new Processor(true);
            processor.setConfigurationProperty(FeatureKeys.GENERATE_BYTE_CODE, "false");
            Xslt30Processor transformer = new Xslt30Processor(processor);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                xmlns:f='http://localhost/'" +
                    "                version='3.0'>" +
                    "                <xsl:param name='x' as='xs:integer'/>" +
                    "                <xsl:param name='y' select='.+2'/>" +
                    "                <xsl:function name='f:add' visibility='public'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:double'/>" +
                    "                   <xsl:sequence select='$a + $b + $x + $y'/>" +
                    "                </xsl:function>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            String[] params = new String[]{"param:x", "!omit-xml-declaration", "item"};
            Object[] pvalues = new Object[]{new XdmAtomicValue(30), "yes", new XdmAtomicValue(20)};

            String sw = transformer.callFunctionReturningString(null, null, "{http://localhost/}add", new Object[]{new XdmAtomicValue(2), new XdmAtomicValue(3)}, params, pvalues);

            assertEquals("57", sw);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }



    public function testCallFunctionArgInvalid() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                xmlns:f='http://localhost/'" +
                    "                version='2.0'>" +
                    "                <xsl:function name='f:add'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:double'/>" +
                    "                   <xsl:sequence select='$a + $b'/>" +
                    "                </xsl:function>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);
            XdmValue v = transformer.callFunctionReturningValue(null, null, "{http://localhost/}add", new Object[]{new XdmAtomicValue(2), new XdmAtomicValue("3")}, null, null);
            fail("Failed to throw error");
        } catch (SaxonApiException e) {
            System.err.println(e.getMessage());
            // OK
        }
    }


    public function testCallNamedTemplateWithParams() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(true);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template name='t'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='$a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);


            String[] params = new String[]{"!omit-xml-declaration", "itparam:a", "itparam:b"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12), new XdmAtomicValue(5)};
            String sw = transformer.callTemplateReturningString(null, null, "t", params, pvalues);
            assertEquals("output", "17", sw.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    public function testCallNamedTemplateWithParamsRequired() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(true);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template name='t'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float' required='yes'/>" +
                    "                   <xsl:sequence select='$a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            String[] params = new String[]{"!omit-xml-declaration", "itparam:a"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12)};
            String sw = transformer.callTemplateReturningString(null, null, "t", params, pvalues);

            fail("failed to detect error");
        } catch (SaxonApiException e) {
            assertTrue("message", e.getMessage().contains("required parameter $b"));
        }
    }


    public function testCallNamedTemplateWithTunnelParams() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template name='t'>" +
                    "                   <xsl:call-template name='u'/>" +
                    "                </xsl:template>" +
                    "                <xsl:template name='u'>" +
                    "                   <xsl:param name='a' as='xs:double' tunnel='yes'/>" +
                    "                   <xsl:param name='b' as='xs:float' tunnel='yes'/>" +
                    "                   <xsl:sequence select='$a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);
            String[] params = new String[]{"!omit-xml-declaration", "itparam:a", "itparam:b", "tunnel"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12), new XdmAtomicValue(5), true};
            String sw = transformer.callTemplateReturningString(null, null, "t", params, pvalues);

            assertEquals(sw.toString(), "17");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public function testCallTemplateRuleWithParams() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template match='*'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='name(.), $a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);
            String[] params = new String[]{"!omit-xml-declaration", "itparam:a", "itparam:b"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12), new XdmAtomicValue(5)};

            String sw = transformer.applyTemplatesReturningString(null, transformer.parseXmlString("<e/>"), null, params, pvalues);

            assertEquals(sw.toString(), "e 17");
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public function testCallTemplateWithResultValidation(): void {
        try {
            Xslt30Processor transformer = new Xslt30Processor(true);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0' exclude-result-prefixes='#all'>" +
                    "                <xsl:import-schema><xs:schema><xs:element name='x' type='xs:int'/></xs:schema></xsl:import-schema>" +
                    "                <xsl:template name='main'>" +
                    "                   <xsl:result-document validation='strict'>" +
                    "                     <x>3</x>" +
                    "                   </xsl:result-document>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            String[] params = new String[]{"!omit-xml-declaration"};
            Object[] pvalues = new Object[]{"yes"};
            String sw = transformer.callTemplateReturningString(null, null, "main", params, pvalues);
            assertEquals("output", "<x>3</x>", sw);
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }

    public function testCallTemplateWithResultValidationFailure(): void {
        try {
            Xslt30Processor transformer = new Xslt30Processor(true);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0' expand-text='yes' exclude-result-prefixes='#all'>" +
                    "                <xsl:import-schema><xs:schema><xs:element name='x' type='xs:int'/></xs:schema></xsl:import-schema>" +
                    "                <xsl:param name='p'>zzz</xsl:param>" +
                    "                <xsl:template name='main'>" +
                    "                   <xsl:result-document validation='strict'>" +
                    "                     <x>{$p}</x>" +
                    "                   </xsl:result-document>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);

            String[] params = new String[]{"!omit-xml-declaration"};
            Object[] pvalues = new Object[]{"yes"};
            String sw = transformer.callTemplateReturningString(null, null, "main", params, pvalues);
            fail("unexpected success");

        } catch (SaxonApiException e) {
            System.err.println("Failed as expected: " + e.getMessage());
        }
    }

    public function testCallTemplateNoParamsRaw(): void {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);
            transformer.createStylesheetFromString(null, "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                    "<xsl:template name='xsl:initial-template'><xsl:sequence select='42'/></xsl:template>" +
                    "</xsl:stylesheet>", null, null);

            String[] params = new String[]{"outvalue"};
            Object[] pvalues = new Object[]{true};
            XdmValue result = transformer.callTemplateReturningValue(null, null,null, params, pvalues);

            assertTrue("result is atomic value", result instanceof XdmAtomicValue);
            assertEquals("result is 42", 42, ((XdmAtomicValue) result).getLongValue());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }



    public function testCallNamedTemplateWithParamsRaw() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template name='t'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='$a+1, $b+1'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);


            String[] params = new String[]{"outvalue", "itparam:a", "itparam:b"};
            Object[] pvalues = new Object[]{true, new XdmAtomicValue(12), new XdmAtomicValue(5)};
            XdmValue val = transformer.callTemplateReturningValue(null, null, "t", params, pvalues);

            assertEquals(2, val.size());
            assertEquals(13, ((XdmAtomicValue) val.itemAt(0)).getLongValue());
            assertEquals(6, ((XdmAtomicValue) val.itemAt(1)).getLongValue());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }


    public function testCatalog(){

        Xslt30Processor transformer = new Xslt30Processor(false);
        Processor proc = transformer.getProcessor();


        try {
            XmlCatalogResolver.setCatalog(CWD_DIR+"../../catalog-test/catalog.xml", proc.getUnderlyingConfiguration(), true);

            transformer.applyTemplatesReturningValue(CWD_DIR+"../../catalog-test/", "example.xml","test1.xsl",null, null);
        } catch (XPathException e) {
            e.printStackTrace();
            fail();
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }

    }



    public function testApplyTemplatesRaw() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template match='*'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='., $a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);
            XdmNode node = transformer.parseXmlString("<e/>");

            String[] params = new String[]{"outvalue", "itparam:a", "itparam:b"};
            Object[] pvalues = new Object[]{true, new XdmAtomicValue(12), new XdmAtomicValue(5)};
            XdmValue result = transformer.applyTemplatesReturningValue(null, node, null, params, pvalues);


            assertEquals("size", 2, result.size());
            XdmItem first = result.itemAt(0);
            assertTrue("t1", first instanceof XdmNode);
            assertEquals("v1", ((XdmNode) first).getNodeName().getLocalName(), "e");
            XdmItem second = result.itemAt(1);
            assertTrue("t2", second instanceof XdmAtomicValue);
            assertEquals("v2", ((XdmAtomicValue) second).getDoubleValue(), 17e0);
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }


    public function testApplyTemplatesToSerializer() throws SaxonApiException {
        try {
            Xslt30Processor transformer = new Xslt30Processor(false);

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:output method='text' item-separator='~~'/>" +
                    "                <xsl:template match='.'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='., $a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            transformer.createStylesheetFromString(null, source, null, null);
            String[] params = new String[]{"!omit-xml-declaration", "itparam:a", "itparam:b", "outvalue"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12), new XdmAtomicValue(5), "yes"};
            String sw = transformer.applyTemplatesReturningString(null, new XdmAtomicValue(16), null, params, pvalues);

            assertEquals("16~~17", sw);
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }



    public function testApplyTemplatesToXdm() throws SaxonApiException {
        try {


            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template match='*'>" +
                    "                   <xsl:param name='a' as='xs:double'/>" +
                    "                   <xsl:param name='b' as='xs:float'/>" +
                    "                   <xsl:sequence select='., $a + $b'/>" +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";
            Xslt30Processor transformer = new Xslt30Processor(false);
            transformer.createStylesheetFromString(null, source, null, null);
            String[] params = new String[]{"!omit-xml-declaration", "itparam:a", "itparam:b", "outvalue"};
            Object[] pvalues = new Object[]{"yes", new XdmAtomicValue(12), new XdmAtomicValue(5), "yes"};
            XdmNode input = transformer.parseXmlString("<e/>");
            XdmValue result = transformer.applyTemplatesReturningValue(null, input, null, params, pvalues);

            assertEquals("size", 2, result.size());
            XdmItem first = result.itemAt(0);
            assertTrue("t1", first instanceof XdmNode);
            assertEquals("v1", ((XdmNode) first).getNodeName().getLocalName(), "e");
            XdmItem second = result.itemAt(1);
            assertTrue("t2", second instanceof XdmAtomicValue);
            assertEquals("v2", ((XdmAtomicValue) second).getDoubleValue(), 17e0);
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }


    public function testResultDocument(): void {
        // bug 2771
        try {
            String xsl = "<xsl:stylesheet version=\"3.0\" \n" +
                    "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                    "\n" +
                    "  <xsl:template match='a'>\n" +
                    "    <c>d</c>\n" +
                    "  </xsl:template>\n" +
                    "\n" +
                    "  <xsl:template match='whatever'>\n" +
                    "    <xsl:result-document href='out.xml'>\n" +
                    "      <e>f</e>\n" +
                    "    </xsl:result-document>\n" +
                    "  </xsl:template>\n" +
                    "\n" +
                    "</xsl:stylesheet>";
            Xslt30Processor transformer = new Xslt30Processor(false);
            transformer.createStylesheetFromString(null, xsl, null, null);
            XdmNode input = transformer.parseXmlString("<a>b</a>");

            XdmValue xdmValue = transformer.applyTemplatesReturningValue(null, input, null, null, null);

            assertEquals("size", 1, xdmValue.size());
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }




    public function testApplyTemplatesToFile(): void {

          try {
              String xsl = "<xsl:stylesheet version=\"3.0\" \n" +
                      "  xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
                      "\n" +
                      "  <xsl:template match='a'>\n" +
                      "    <c>d</c>\n" +
                      "  </xsl:template>\n" +
                      "</xsl:stylesheet>";
              Xslt30Processor transformer = new Xslt30Processor(false);
              transformer.createStylesheetFromString(null, xsl, null, null);
              XdmNode input = transformer.parseXmlString("<a>b</a>");
              Object [] values = new Object[]{"output123.xml"};
              String [] params = new String[]{"o"};
              transformer.applyTemplatesReturningFile(ConfigTest.DATA_DIR + "/sandpit", input, null, null,params, values);
              File tempFile = new File(ConfigTest.DATA_DIR + "/sandpit/output123.xml");

              assertTrue("File exists:", tempFile.exists());
              if(tempFile.exists()) {
                  tempFile.delete();
              }
          } catch (SaxonApiException e) {
              e.printStackTrace();
              fail();
          }
      }

    public function testItemSeparatorToSerializer(): void {
        try {

            String sr =
                    "<xsl:stylesheet version='2.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>" +
                            "  <xsl:template name='go'>"
                            + "<xsl:comment>start</xsl:comment><a><b/><c/></a><xsl:comment>end</xsl:comment>"
                            + "</xsl:template>" +
                            "</xsl:stylesheet>";
            Xslt30Processor transformer = new Xslt30Processor(false);
            transformer.createStylesheetFromString(null, sr, null, null);
            String[] params = new String[]{"!method", "!indent", "!item-separator"};
            Object[] pvalues = new Object[]{"xml", "no", "+++"};
            String sw = transformer.callTemplateReturningString(null, null, "go", params, pvalues);
            System.err.println(sw);
            assertTrue(sw.contains("<!--start-->+++"));
            assertTrue(sw.contains("+++<!--end-->"));
            assertTrue(sw.contains("<a><b/><c/></a>"));
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    public function testSequenceResult() throws SaxonApiException {
        try {

            String source = "<?xml version='1.0'?>" +
                    "                <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'" +
                    "                xmlns:xs='http://www.w3.org/2001/XMLSchema'" +
                    "                version='3.0'>" +
                    "                <xsl:template name='xsl:initial-template' as='xs:integer+'>" +
                    "                     <xsl:sequence select='(1 to 5)' />        " +
                    "                </xsl:template>" +
                    "                </xsl:stylesheet>";

            Xslt30Processor transformer = new Xslt30Processor(false);
            transformer.createStylesheetFromString(null, source, null, null);
            String[] params = new String[]{"outvalue"};
            Object[] pvalues = new Object[]{true};
            XdmValue res = transformer.callTemplateReturningValue(null, null, null, params, pvalues);
            int count = res.size();
            XdmAtomicValue value = (XdmAtomicValue) res.itemAt(0);
        } catch (SaxonApiException e) {
            e.printStackTrace();
            fail();
        }
    }

*/



}
