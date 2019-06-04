from tempfile import mkstemp
import pytest
from saxonc import *

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


def test_create_init():
    """Only one init SaxonProcessor object can exist"""
    with pytest.raises(RuntimeError):
        PySaxonProcessor(license=True)


def test_version():
    """SaxonProcessor version string content"""
    sp = PySaxonProcessor()
    ver = sp.version
    
    assert ver.startswith('Saxon/C ')
    assert ver.endswith('from Saxonica')

def test_release():
    with PySaxonProcessor(license=False) as proc:
        xsltproc = proc.new_xslt_processor()
        document = proc.parse_xml(xml_text="<out><person>text1</person><person>text2</person><person>text3</person></out>")
        xsltproc.set_source(node=document)
        xsltproc.compile_stylesheet(stylesheet_text="<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>       <xsl:param name='values' select='(2,3,4)' /><xsl:output method='xml' indent='yes' /><xsl:template match='*'><output><xsl:value-of select='//person[1]'/><xsl:for-each select='$values' ><out><xsl:value-of select='. * 3'/></out></xsl:for-each></output></xsl:template></xsl:stylesheet>")
        output2 = xsltproc.transform_to_string()
        assert output2.startswith('<?xml version="1.0" encoding="UTF-8"?>\n<output>text1<out>6</out')


