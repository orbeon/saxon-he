from saxonc import *

with PySaxonProcessor(license=False) as saxonproc2:
    trans = saxonproc2.new_xslt30_processor()

    source = "<?xml version='1.0'?>  <xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'  xmlns:xs='http://www.w3.org/2001/XMLSchema'  version='3.0'>  <xsl:template match='*'>     <xsl:param name='a' as='xs:double'/>     <xsl:param name='b' as='xs:float'/>     <xsl:sequence select='., $a + $b'/>  </xsl:template>  </xsl:stylesheet>"

    trans.compile_stylesheet(stylesheet_text=source)
    node = saxonproc2.parse_xml(xml_text="<e/>")

    trans.set_result_as_raw_value(True)
    trans.set_initial_template_parameters(False, {"a":saxonproc2.make_integer_value(12), "b":saxonproc2.make_integer_value(5)})
    trans.set_initial_match_selection(xdm_value=node)
    result = trans.apply_templates_returning_string()
    print(result)
    """ saxonproc2.set_cwd('.')
    print(saxonproc2.version)
    saxonproc2.set_configuration_property("xsdversion", "1.1")
    val = saxonproc2.new_schema_validator()
    
    val.register_schema(xsd_file="family-ext.xsd")

    val.register_schema(xsd_file="family.xsd")
    val.validate(source_file="family.xml")
    nodea = val.validation_report

    print(val.exception_occurred())
    if nodea is None:
        print(val.get_error_message(1))
        print('node returned is None')
    else:
        print('About to output node content:\r'+ nodea.string_value)

    invalid_xml = "<?xml version='1.0'?><request><a/><!--comment--></request>"
    sch1 = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' elementFormDefault='qualified' attributeFormDefault='unqualified'><xs:element name='request'><xs:complexType><xs:sequence><xs:element name='a' type='xs:string'/><xs:element name='b' type='xs:string'/></xs:sequence><xs:assert test='count(child::node()) = 3'/></xs:complexType></xs:element></xs:schema>"
    input_ = saxonproc2.parse_xml(xml_text=invalid_xml)

    val.register_schema(xsd_text=sch1)
    val.set_output_file('validationOutput.txt')
    node =val.validate_to_node('invalid.xml')
    print(val.exception_occurred())
    if node is None:
        print('node returned is None')
    else:
        print('About to output node content:\r')
 
    print(val.get_error_message(1))"""

