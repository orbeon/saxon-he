from tempfile import mkstemp
import pytest
from saxonc import *
import os


@pytest.fixture
def saxonproc():
    return PySaxonProcessor()


def test_create_bool():
    """Create SaxonProcessor object with a boolean argument"""
    sp1 = PySaxonProcessor(True)
    sp2 = PySaxonProcessor(False)
    assert isinstance(sp1, PySaxonProcessor)
    assert isinstance(sp2, PySaxonProcessor)


@pytest.mark.skip('Error: SaxonDll.processor is NULL in constructor(configFile)')
def test_create_config():
    """Create SaxonProcessor object with a configuration file argument"""
    conf_xml = b"""\
    <configuration xmlns="http://saxon.sf.net/ns/configuration" edition="HE">
    <global
        allowExternalFunctions="true"
        allowMultiThreading="true"
        allowOldJavaUriFormat="false"
        collationUriResolver="net.sf.saxon.lib.StandardCollationURIResolver"
        collectionUriResolver="net.sf.saxon.lib.StandardCollectionURIResolver"
        compileWithTracing="false"
        defaultCollation="http://www.w3.org/2005/xpath-functions/collation/codepoint"
        defaultCollection="file:///e:/temp"
        dtdValidation="false"
        dtdValidationRecoverable="true"
        errorListener="net.sf.saxon.StandardErrorListener"
        expandAttributeDefaults="true"
        lazyConstructionMode="false"
        lineNumbering="true"
        optimizationLevel="10"
        preEvaluateDocFunction="false"
        preferJaxpParser="true"
        recognizeUriQueryParameters="true"
        schemaValidation="strict"
        serializerFactory=""
        sourceParser=""
        sourceResolver=""
        stripWhitespace="all"
        styleParser=""
        timing="false"
        traceExternalFunctions="true"
        traceListener="net.sf.saxon.trace.XSLTTraceListener"
        traceOptimizerDecisions="false"
        treeModel="tinyTreeCondensed"
        uriResolver="net.sf.saxon.StandardURIResolver"
        usePiDisableOutputEscaping="false"
        useTypedValueCache="true"
        validationComments="false"
        validationWarnings="true"
        versionOfXml="1.0"
        xInclude="false"
      />
      <xslt
        initialMode=""
        initialTemplate=""
        messageReceiver=""
        outputUriResolver=""
        recoveryPolicy="recoverWithWarnings"
        schemaAware="false"
        staticErrorListener=""
        staticUriResolver=""
        styleParser=""
        version="2.1"
        versionWarning="false">
        <extensionElement namespace="http://saxon.sf.net/sql"
            factory="net.sf.saxon.option.sql.SQLElementFactory"/>
      </xslt>
      <xquery
        allowUpdate="true"
        constructionMode="preserve"
        defaultElementNamespace=""
        defaultFunctionNamespace="http://www.w3.org/2005/xpath-functions"
        emptyLeast="true"
        inheritNamespaces="true"
        moduleUriResolver="net.sf.saxon.query.StandardModuleURIResolver"
        preserveBoundarySpace="false"
        preserveNamespaces="true"
        requiredContextItemType="document-node()"
        schemaAware="false"
        staticErrorListener=""
        version="1.1"
        />
      <xsd
        occurrenceLimits="100,250"
        schemaUriResolver="com.saxonica.sdoc.StandardSchemaResolver"
        useXsiSchemaLocation="false"
        version="1.1"
      />
      <serialization
        method="xml"
        indent="yes"
        saxon:indent-spaces="8"
        xmlns:saxon="http://saxon.sf.net/"/>
      <localizations defaultLanguage="en" defaultCountry="US">
        <localization lang="da" class="net.sf.saxon.option.local.Numberer_da"/>
        <localization lang="de" class="net.sf.saxon.option.local.Numberer_de"/>
      </localizations>
      <resources>
        <externalObjectModel>net.sf.saxon.option.xom.XOMObjectModel</externalObjectModel>
        <extensionFunction>s9apitest.TestIntegrationFunctions$SqrtFunction</extensionFunction>
        <schemaDocument>file:///c:/MyJava/samples/data/books.xsd</schemaDocument>
        <schemaComponentModel/>
      </resources>
      <collations>
        <collation uri="http://www.w3.org/2005/xpath-functions/collation/codepoint"
                   class="net.sf.saxon.sort.CodepointCollator"/>
        <collation uri="http://www.microsoft.com/collation/caseblind"
                   class="net.sf.saxon.sort.CodepointCollator"/>
        <collation uri="http://example.com/french" lang="fr" ignore-case="yes"/>
      </collations>
    </configuration>
    """
    try:
        fd, fname = mkstemp(suffix='.xml')
        os.write(fd, conf_xml)
        os.close(fd)
        if not os.path.exists(fname):
            raise IOError('%s does not exist' % fname)

        with open(fname, 'r') as f:
            print(f.read())

        sp = SaxonProcessor(fname.encode('utf-8'))
        assert isinstance(sp, SaxonProcessor)
    finally:
        os.unlink(fname)


def test_create_procs():
    """Create XPathProcessor, XsltProcessor from SaxonProcessor object"""
    sp = PySaxonProcessor()
    xp = sp.new_xpath_processor()
    xsl = sp.new_xslt_processor()
    assert isinstance(xp, PyXPathProcessor)
    assert isinstance(xsl, PyXsltProcessor)


def test_version():
    """SaxonProcessor version string content"""
    sp = PySaxonProcessor()
    ver = sp.version
    
    assert ver.startswith('Saxon/C ')
    assert ver.endswith('from Saxonica')



'''PyXsltProcessor test cases '''

def test_xslt_processor():
    sp = PySaxonProcessor()
    xsltproc = sp.new_xslt_processor()
    node_ = sp.parse_xml(xml_file_name="cat.xml")
    xsltproc.set_source(node=node_)
    xsltproc.compile_stylesheet(stylesheet_text="<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>       <xsl:param name='values' select='(2,3,4)' /><xsl:output method='xml' indent='yes' /><xsl:template match='*'><output><xsl:value-of select='//person[1]'/><xsl:for-each select='$values' ><out><xsl:value-of select='. * 3'/></out></xsl:for-each></output></xsl:template></xsl:stylesheet>")
    output2 = xsltproc.transform_to_string()
    assert 'text1' in output2

def test_Xslt_from_file(saxonproc):
    xsltproc = saxonproc.new_xslt_processor()
    result = xsltproc.transform_to_value(source_file='cat.xml', stylesheet_file='test.xsl')
    assert result is not None
    assert 'text3' in result.string_value


def test_Xslt_from_file_error(saxonproc):
    xsltproc = saxonproc.new_xslt_processor()
    result = xsltproc.transform_to_value(source_file='cat.xml', stylesheet_file='test-error.xsl')
    assert result is None
    assert xsltproc.exception_occurred()
    assert xsltproc.exception_count() == 1

def test_xslt_parameter(saxonproc):
    input_ = saxonproc.parse_xml(xml_text="<out><person>text1</person><person>text2</person><person>text3</person></out>")
    value1 = saxonproc.make_integer_value(10)
    trans = saxonproc.new_xslt_processor()
    trans.set_parameter("numParam",value1)
    assert value1 is not None

    trans->set_source(xdm_node=inputi)
    output_ = trans.transform_to_string(stylesheet_file="test.xsl")
    print(output_)

    
    
''' PyXQueryProcessor '''

def test_return_document_node(saxonproc):
    node = saxonproc.parse_xml(xml_text='<foo/>')
    xqc = saxonproc.new_xquery_processor()
    xqc.set_query_content('document{.}')
    xqc.set_context(xdm_item=node)
    result = xqc.run_query_to_value()
    if isinstance(result, PyXdmNode):
        assert result.node_kind == PyXdmNodeKind.DOCUMENT

def testxQuery1(saxonproc):
    query_proc = saxonproc.new_xquery_processor()
    query_proc.clear_properties()
    query_proc.clear_parameters()
    query_proc.set_property("s", "cat.xml")

    query_proc.set_property("qs", "<out>{count(/out/person)}</out>")

    result = query_proc.run_query_to_string()
    assert not result == None

    query_proc.set_cwd(".")
    query_proc.run_query_to_file(output_file_name="catOutput.xml")
    assert os.path.exists("catOutput.xml")
    node = saxonproc.parse_xml(xml_file_name='catOutput.xml')
    xp = saxonproc.new_xpath_processor()
    xp.set_context(xdm_item=node)
    assert xp.effective_boolean_value("/out/text()=3")    
    if os.path.exists('catOutput.xml'):
        os.remove("catOutput.xml")


def test_default_namespace(saxonproc):
    query_proc = saxonproc.new_xquery_processor()
    query_proc.declare_namespace("", "http://one.uri/")
    node = saxonproc.parse_xml(xml_text="<foo xmlns='http://one.uri/'><bar/></foo>")
    query_proc.set_context(xdm_item=node)
    query_proc.set_query_content("/foo")

    value = query_proc.run_query_to_value()

    assert value.size == 1 


def test_XQuery_line_number():
    ''' No license file given therefore result will return None'''
    proc = PySaxonProcessor(True)
    proc.set_configuration_property("l", "on")
    query_proc = proc.new_xquery_processor()
    
    query_proc.set_property("s", "cat.xml")
    query_proc.declare_namespace("saxon","http://saxon.sf.net/")

    query_proc.set_property("qs", "saxon:line-number(doc('cat.xml')/out/person[1])")

    result = query_proc.run_query_to_string()
    assert result == None
    

def testReusability(saxonproc):
    queryproc = saxonproc.new_xquery_processor()
    queryproc.clear_properties()
    queryproc.clear_parameters()

    input_ =  saxonproc.parse_xml(xml_text="<foo xmlns='http://one.uri/'><bar xmlns='http://two.uri'>12</bar></foo>")
    queryproc.declare_namespace("", "http://one.uri/")
    queryproc.set_query_content("declare variable $p as xs:boolean external; exists(/foo) = $p")

    queryproc.set_context(xdm_item=input_)

    value1 = saxonproc.make_boolean_value(True)
    queryproc.set_parameter("p",value1)
    result = queryproc.run_query_to_value()
    if result is not None:
        print('result type='+ str(result))    
    assert result is not None
    assert result.is_atomic
    assert result.boolean_value

    queryproc.clear_parameters()
    queryproc.clear_properties()    
    
    queryproc.declare_namespace("", "http://two.uri")
    queryproc.set_query_content("declare variable $p as xs:integer external; /*/bar + $p")
    
    queryproc.set_context(xdm_item=input_)

    value2 = saxonproc.make_long_value(6)
    queryproc.set_parameter("p",value2)
        
    result2 = queryproc.run_query_to_value()
    assert result2.integer_value == 18



'''PyXPathProcessor test cases'''


def test_xpath_proc(saxonproc):

    sp = saxonproc
    xp = saxonproc.new_xpath_processor()
    xp.set_context(file_name='cat.xml')
    assert xp.effective_boolean_value('count(//person) = 3')
    assert not xp.effective_boolean_value("/out/person/text() = 'text'")


def test_atomic_values():
    sp = PySaxonProcessor()
    value = sp.make_double_value(3.5)
    boolVal = value.boolean_value
    assert boolVal == True
    assert value.string_value == '3.5'
    assert value.double_value == 3.5
    assert value.int_value == 3
    primValue = value.primitive_type_name
    assert primValue == 'Q{http://www.w3.org/2001/XMLSchema}double'


def test_node_list():
    xml = """\
    <out>
        <person att1='value1' att2='value2'>text1</person>
        <person>text2</person>
        <person>text3</person>
    </out>
    """
    sp = PySaxonProcessor()
    
    node = sp.parse_xml(xml_text=xml)
    outNode = node.children[0]
    children = outNode.children
    personData = str(children)    
    assert ('<person att1' in personData)



def parse_xml_file():

    sp = PySaxonProcessor()
    
    node = sp.parse_xml(xml_file_name='cat.xml')
    outNode = node.children[0]
    assert outNode.name == 'out'


def test_node():
    xml = """\
    <out>
        <person att1='value1' att2='value2'>text1</person>
        <person>text2</person>
        <person>text3</person>
    </out>
    """
    sp = PySaxonProcessor()
    
    node = sp.parse_xml(xml_text=xml)
    assert node.node_kind == 9    
    assert node.size == 1
    outNode = node.children[0]
    assert outNode.name == 'out'
    assert outNode.node_kind == PyXdmNodeKind.ELEMENT
    children = outNode.children    
    attrs = children[1].attributes
    assert len(attrs) == 2
    assert children[1].get_attribute_value('att2') == 'value2'
    assert 'value2' in attrs[1].string_value 


def test_evaluate():
    xml = """\
    <out>
        <person att1='value1' att2='value2'>text1</person>
        <person>text2</person>
        <person>text3</person>
    </out>
    """
    sp = PySaxonProcessor()
    xp = sp.new_xpath_processor()
    
    node = sp.parse_xml(xml_text=xml)
    assert isinstance(node, PyXdmNode)
    xp.set_context(xdm_item=node)
    value = xp.evaluate('//person')
    assert isinstance(value, PyXdmValue)
    assert value.size == 3
    

def test_single():
    xml = """\
    <out>
        <person>text1</person>
        <person>text2</person>
        <person>text3</person>
    </out>
    """
    sp = PySaxonProcessor()
    xp = sp.new_xpath_processor()
    
    node = sp.parse_xml(xml_text=xml)
    assert isinstance(node, PyXdmNode)
    xp.set_context(xdm_item=node)
    item = xp.evaluate_single('//person[1]')
    assert isinstance(item, PyXdmItem)
    assert item.size == 1
    assert not item.is_atomic
    assert item.string_value == '<person>text1</person>'


'''Test case should be run last to test release() '''
def test_release():
    with PySaxonProcessor(license=False) as proc:
        xsltproc = proc.new_xslt_processor()
        document = proc.parse_xml(xml_text="<out><person>text1</person><person>text2</person><person>text3</person></out>")
        xsltproc.set_source(node=document)
        xsltproc.compile_stylesheet(stylesheet_text="<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>       <xsl:param name='values' select='(2,3,4)' /><xsl:output method='xml' indent='yes' /><xsl:template match='*'><output><xsl:value-of select='//person[1]'/><xsl:for-each select='$values' ><out><xsl:value-of select='. * 3'/></out></xsl:for-each></output></xsl:template></xsl:stylesheet>")
        output2 = xsltproc.transform_to_string()
        assert output2.startswith('<?xml version="1.0" encoding="UTF-8"?>\n<output>text1<out>6</out')


