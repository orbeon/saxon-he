from saxonc import *

with PySaxonProcessor(license=True) as saxonproc2:
    saxonproc2.set_cwd('.')
    print(saxonproc2.version)
    saxonproc2.set_configuration_property("xsdversion", "1.1")
    val = saxonproc2.new_schema_validator()
    

    invalid_xml = "<?xml version='1.0'?><request><a1/><!--comment--></request>"
    sch1 = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema' elementFormDefault='qualified' attributeFormDefault='unqualified'><xs:element name='request'><xs:complexType><xs:sequence><xs:element name='a' type='xs:string'/><xs:element name='b' type='xs:string'/></xs:sequence><xs:assert test='count(child::node()) = 3'/></xs:complexType></xs:element></xs:schema>"
    input_ = saxonproc2.parse_xml(xml_text=invalid_xml)

    val.register_schema(xsd_text=sch1)
    val.set_output_file('validationOutput.txt')
    val.validate(xdm_node=input_)

    node =val.validate_to_node(xdm_node=input_)
    print("Validation Exception occurred =",val.exception_occurred())
    if node is None:
        print('node returned is None')
    else:
        print('About to output node content:\r')

    nodea = val.validation_report

    print(val.exception_occurred())
    if nodea is None:
        print(val.get_error_message(1))
        print('node returned is None')
    else:
        print('About to output node content:\r'+ nodea.string_value)

