"""@package saxonc
This documentation details the Python API for Saxon/C, which has been written in cython. 

Saxon/C is a cross-compiled variant of Saxon from the Java platform to the C/C++ platform. Saxon/C provides processing in XSLT, XQuery and XPath, and Schema validation. Main classes: PySaxonProcessor, PyXsltProcessor, PyXQueryProcessor, PyXdmValue, PyXdmItem, PyXdmNode and PyXdmAtomicValue."""


# distutils: language = c++

cimport saxoncClasses
from libcpp cimport bool
#import contextlib
   
cdef class PySaxonProcessor:
    """An SaxonProcessor acts as a factory for generating XQuery, XPath, Schema and XSLT compilers.
    """
    cdef saxoncClasses.SaxonProcessor *thisptr      # hold a C++ instance which we're wrapping

    ##
    # The Constructor
    # @param license Flag that a license is to be used
    # @contextlib.contextmanager
    def __cinit__(self, license=False):
        """
        __cinit__(self, license=False)
        The constructor.

        :param bool license: Flag that a license is to be used. The Default is false.
        :type license: bool
        """
        self.thisptr = new saxoncClasses.SaxonProcessor(license)
            
    def __dealloc__(self):
        """The destructor."""
        del self.thisptr

    def __enter__(self):
      """enter method for use with the keyword 'with' context"""
      return self

    def __exit__(self, exception_type, exception_value, traceback):
        """The exit method for the context PySaxonProcessor. Here we release the Jet VM resources.
        If we have more than one live PySaxonProcessor object the release() method has no effect.
        """
        self.thisptr.release()

    property version:
        """
        Get the Saxon Version.

        Getter:
            str: The Saxon version
        """
        def __get__(self):        
            cdef const char* c_string = self.thisptr.version()
            ustring = c_string.decode('UTF-8')
            return ustring


    def release(self):
        """
        release(self) 
        Clean up and destroy Java VM to release memory used."""

        self.thisptr.release()


    @property
    def cwd(self):
        """
        cwd Property represents the current working directorty

        :str: Get or set the current working directory"""
        cdef const char* c_string = self.thisptr.getcwd()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring


    @cwd.setter
    def cwd(self, cwd):
         py_value_string = cwd.encode('UTF-8') if cwd is not None else None
         cdef char * c_str_ = py_value_string if cwd is not None else ""
         self.thisptr.setcwd(cwd)
    

    @property
    def resources_directory(self, dir_):
        """
        Property to set or get resources directory 
        
        :str: A string of the resources directory which Saxon will use

        """
        py_value_string = dir_.encode('UTF-8') if dir_ is not None else None
        cdef char * c_str_ = py_value_string if dir_ is not None else ""
        self.thisptr.setResourcesDirectory(c_str_)

    @resources_directory.setter   
    def resources_directory(self):
        cdef const char* c_string = self.thisptr.getResourcesDirectory()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring
    def set_configuration_property(self, name, value):
        """
        set_configuration_property(self, name, value)
        Set configuration property specific to the processor in use.
        Properties set here are common across all processors. 

        Args:
            name (str): The name of the property
            value (str): The value of the property

        Example:
          'l': 'on' or 'off' - to enable the line number

        """
        py_value_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_str_ = py_value_string if name is not None else ""
        self.thisptr.setConfigurationProperty(c_str_, value)
    def clear_configuration_properties(self):
        """
        clear_configuration_properties(self)
        Clear the configurations properties in use by the processor 

        """
        self.thisptr.clearConfigurationProperties()

    @property
    def is_schema_aware(self):
        """
        Check is the processor is Schema aware. A licensed Saxon-EE/C product is schema aware 

        ":bool: Indicate if the processor is schema aware, True or False otherwise
        """
        return self.thisprt.isSchemaAware()

    def new_xslt_processor(self):
        """
        new_xslt_processor(self)
        Create an PyXsltProcessor. A PyXsltProcessor is used to compile and execute XSLT stylesheets. 

        Returns: 
            PyXsltProcessor: a newly created PyXsltProcessor

        """
        cdef PyXsltProcessor val = PyXsltProcessor()
        val.thisxptr = self.thisptr.newXsltProcessor()
        return val

    def new_xquery_processor(self):
        """
        new_xquery_processor(self)
        Create an PyXqueryProcessor. A PyXQueryProcessor is used to compile and execute XQuery queries. 

        Returns: 
            PyXQueryProcessor: a newly created PyXQueryProcessor

        """
        cdef PyXQueryProcessor val = PyXQueryProcessor()
        val.thisxqptr = self.thisptr.newXQueryProcessor()
        return val

    def new_xpath_processor(self):
        """
        new_xpath_processor(self)
        Create an PyXPathProcessor. A PyXPathProcessor is used to compile and execute XPath expressions. 

        Returns: 
            PyXsltProcessor: a newly created XsltProcessor

        """
        cdef PyXPathProcessor val = PyXPathProcessor()
        val.thisxpptr = self.thisptr.newXPathProcessor()
        return val

    def new_schema_validator(self):
        """
        new_schema_validator(self)
        Create a PySchemaValidator which can be used to validate instance documents against the schema held by this 

        Returns: 
            PySchemaValidator: a newly created PySchemaValidator

        """
        cdef PySchemaValidator val = PySchemaValidator()
        val.thissvptr = self.thisptr.newSchemaValidator()
        return val

    def make_string_value(self, str_):
        """
        make_string_value(self, str_)
        Factory method. Unlike the constructor, this avoids creating a new StringValue in the case
        of a zero-length string (and potentially other strings, in future)
        
        Args:
            str_ (str): the String value. Null is taken as equivalent to "".

        Returns:
            PyXdmAtomicValue: The corresponding Xdm StringValue

        """
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeStringValue(str_)
        return val

    def make_integer_value(self, value):
        """
        make_integer_value(self, value)
        Factory method: makes either an Int64Value or a BigIntegerValue depending on the value supplied
        
        Args:
            value (int): The supplied primitive integer value

        Returns:
            PyXdmAtomicValue: The corresponding Xdm value which is a BigIntegerValue or Int64Value as appropriate

        """
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeIntegerValue(value)
        return val

    def make_double_value(self, value):
        """
        make_double_value(self, value)
        Factory method: makes a double value

        Args:
            value (double): The supplied primitive double value 

        Returns:
            PyXdmAtomicValue: The corresponding Xdm Value
        """
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeDoubleValue(value)
        return val

    def make_float_value(self, value):
        """
        make_float_value(self, value)
        Factory method: makes a float value

        Args:
            value (float): The supplied primitive float value 

        Returns:
            PyXdmAtomicValue: The corresponding Xdm Value
        """

        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeFloatValue(value)
        return val

    def make_long_value(self, value):
        """
        make_long_value(self, value)
        Factory method: makes either an Int64Value or a BigIntegerValue depending on the value supplied

        Args:
            value (long): The supplied primitive long value 

        Returns:
            PyXdmAtomicValue: The corresponding Xdm Value
        """
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeLongValue(value)
        return val

    def make_boolean_value(self, value):
        """
        make_boolean_value(self, value)
        Factory method: makes a XdmAtomicValue representing a boolean Value

        Args:
            value (boolean): True or False, to determine which boolean value is required

        Returns:
            PyAtomicValue: The corresonding XdmValue
        """
        cdef bool c_b = value
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeBooleanValue(c_b)
        return val

    def make_qname_value(self, str_):
        """
        make_qname_value(self, str_)
        Create an QName Xdm value from string representation in clark notation

        Args:
            str_ (str): The value given in a string form in clark notation. {uri}local

        Returns:
            PyAtomicValue: The corresonding value

        """
        py_value_string = str_.encode('UTF-8') if str_ is not None else None
        cdef char * c_str_ = py_value_string if str_ is not None else ""
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeQNameValue(c_str_)
        return val

    def make_atomic_value(self, value_type, value):
        """
        make_atomic_value(self, value_type, value)
        Create an QName Xdm value from string representation in clark notation

        Args:
            str_ (str): The value given in a string form in clark notation. {uri}local

        Returns:
            PyAtomicValue: The corresonding value

        """

        py_valueType_string = value_type.encode('UTF-8') if value_type is not None else None
        cdef char * c_valueType_string = py_valueType_string if value_type is not None else ""
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeAtomicValue(c_valueType_string, value)
        return val

    def get_string_value(self, PyXdmItem item):
        """
        get_string_value(self, PyXdmItem item)
        Create an QName Xdm value from string representation in clark notation

        Args:
            str_ (str): The value given in a string form in clark notation. {uri}local

        Returns:
            PyAtomicValue: The corresonding value

        """
        return self.thisptr.getStringValue(item.derivedptr)

    def parse_xml(self, **kwds):
        """
        parse_xml(self, **kwds)
        Parse a lexical representation, source file or uri of the source document and return it as an Xdm Node

        Args:
            **kwds : The possible keyword arguments must be one of the follow (xml_file_name|xml_text|xml_uri)

        Returns:
            PyXdmNode: The Xdm Node representation of the XML document

        Raises:
            Exception: Error if the keyword argument is not one of xml_file_name|xml_text|xml_uri.
        """
        py_error_message = "Error: parseXml should only contain one of the following keyword arguments: (xml_file_name|xml_text|xml_uri)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef PyXdmNode val = None
        cdef py_value = None
        cdef char * c_xml_string = NULL
        if "xml_text" in kwds:
          py_value = kwds["xml_text"]
          py_xml_text_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_xml_text_string if py_value is not None else "" 
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromString(c_xml_string)
          return val
        elif "xml_file_name" in kwds:
          py_value = kwds["xml_file_name"]
          py_filename_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_filename_string if py_value is not None else ""
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromFile(c_xml_string)
          return val 
        elif "xml_uri" in kwds:
          py_value = kwds["xml_uri"]
          py_uri_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_uri_string if py_value is not None else ""
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromUri(c_xml_string)
          return val
        else:
           raise Exception(py_error_message)

    def exception_occurred(self):
        """
        exception_occurred(self)
        Check if an exception has occurred internally within Saxon/C

        Returns:
            boolean: True or False if an exception has been reported internally in Saxon/C
        """
        return self.thisptr.exceptionOccurred()

    def exception_clear(self):
        """
        exceltion_clear(self)
        Clear any exception thrown internally in Saxon/C.


        """
        self.thisptr.exceptionClear()



cdef class PyXsltProcessor:
     """An PyXsltProcessor represents factory to compile, load and execute a stylesheet.
     It is possible to cache the context and the stylesheet in the PyXsltProcessor """

     cdef saxoncClasses.XsltProcessor *thisxptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        """Default constructor """
        self.thisxptr = NULL
     def __dealloc__(self):
        if self.thisxptr != NULL:
           del self.thisxptr
     def set_cwd(self, cwd):
        """
        set_cwd(self, cwd)
        Set the current working directory.

        Args:
            cwd (str): current working directory
        """
        py_cwd_string = cwd.encode('UTF-8') if cwd is not None else None
        cdef char * c_cwd = py_cwd_string if cwd is not None else "" 
        self.thisxptr.setcwd(c_cwd)

     def set_source(self, **kwds):
        """Set the source document for the transformation.

        Args:
            **kwds: Keyword argument can only be one of the following: file_name|node
        Raises:
            Exception: Exception is raised if eyword argument is not one of file_name or node.
        """

        py_error_message = "Error: setSource should only contain one of the following keyword arguments: (file_name|node)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef py_value = None
        cdef py_value_string = None
        cdef char * c_source
        cdef PyXdmNode xdm_node = None
        if "file_name" in kwds:
            py_value = kwds["file_name"]
            py_value_string = py_value.encode('UTF-8') if py_value is not None else None
            c_source = py_value_string if py_value is not None else "" 
            self.thisxptr.setSourceFromFile(c_source)
        elif "node" in kwds:
            xdm_node = kwds["node"]
            self.thisxptr.setSourceFromXdmNode(xdm_node.derivednptr)
        else:
          raise Exception(py_error_message)

     def set_output_file(self, output_file):
        """
        set_output_file(self, output_file)
        Set the output file where the output of the transformation will be sent

        Args:
            output_file (str): The output file supplied as a str

        """
        py_filename_string = output_file.encode('UTF-8') if output_file is not None else None
        cdef char * c_outputfile = py_filename_string if output_file is not None else ""
        self.thisxptr.setOutputFile(c_outputfile)

     def set_jit_compilation(self, bool jit):
        """
        set_jit_compilation(self, jit)
        Say whether just-in-time compilation of template rules should be used.

        Args:
            jit (bool): True if just-in-time compilation is to be enabled. With this option enabled,
                static analysis of a template rule is deferred until the first time that the
                template is matched. This can improve performance when many template
                rules are rarely used during the course of a particular transformation; however,
                it means that static errors in the stylesheet will not necessarily cause the
                compile(Source) method to throw an exception (errors in code that is
                actually executed will still be notified but this may happen after the compile(Source)
                method returns). This option is enabled by default in Saxon-EE, and is not available
                in Saxon-HE or Saxon-PE.
                Recommendation: disable this option unless you are confident that the
                stylesheet you are compiling is error-free. 

        """
        cdef bool c_jit
        c_jit = jit
        self.thisxptr.setJustInTimeCompilation(c_jit)
        #else:
        #raise Warning("setJustInTimeCompilation: argument must be a boolean type. JIT not set")
     def set_parameter(self, name, PyXdmValue value):
        """
        set_parameter(self, PyXdmValue value)
        Set the value of a stylesheet parameter

        Args:
            name (str): the name of the stylesheet parameter, as a string. For namespaced parameter use the JAXP solution i.e. "{uri}name
            value (PyXdmValue): the value of the stylesheet parameter, or null to clear a previously set value

        """
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        self.thisxptr.setParameter(c_name, value.thisvptr)

     def get_parameter(self, name):
        """
        get_parameter(self, name)
        Get a parameter value by a given name

        Args:
            name (str): The name of the stylesheet parameter

        Returns:
            PyXdmValue: The Xdm value of the parameter  


        """
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getParameter(c_name)
        return val

     def remove_parameter(self, name):
        """
        remove_parameter(self, name)
        Remove the parameter given by name from the PyXsltProcessor. The parameter will not have any affect on the stylesheet if it has not yet been executed

        Args:
            name (str): The name of the stylesheet parameter

        Returns:
            bool: True if the removal of the parameter has been successful, False otherwise.

        """

        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        return self.thisxptr.removeParameter(c_name)

     def set_property(self, name, value):
        """
        set_property(self, name, value)
        Set a property specific to the processor in use.
 
        Args:
            name (str): The name of the property
            value (str): The value of the property

        Example:
            XsltProcessor: set serialization properties (names start with '!' i.e. name "!method" -> "xml")\r
            'o':outfile name,\r
            'it': initial template,\r 
            'im': initial mode,\r
            's': source as file name\r
            'm': switch on message listener for xsl:message instructions,\r
            'item'| 'node' : source supplied as an XdmNode object,\r
            'extc':Set the native library to use with Saxon for extension functions written in C/C++/PHP\r

        """

        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        py_value_string = value.encode('UTF-8') if value is not None else None
        cdef char * c_value = py_value_string if value is not None else ""
        self.thisxptr.setProperty(c_name, c_value)

     def clear_parameters(self):
        """
        clear_parameter(self)
        Clear all parameters set on the processor for execution of the stylesheet
        """

        self.thisxptr.clearParameters()
     def clear_properties(self):
        """
        clear_properties(self)
        Clear all properties set on the processor
        """

        self.thisxptr.clearProperties()
     def get_xsl_messages(self):
        """
        Get the messages written using the <code>xsl:message</code> instruction
        get_xsl_message(self)
        
        Returns:
            PyXdmValue: Messages returned as an XdmValue. 

        """

        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getXslMessages()
        return val

     def transform_to_string(self, **kwds):
        """
        transform_to_string(self, **kwds)
        Execute transformation to string.

        Args:
            **kwds: Possible arguments: source_file (str) or xdm_node (PyXdmNode). Other allowed argument: stylesheet_file (str)


        Example:

            1) result = xsltproc.transform_to_string(source_file="cat.xml", stylesheet_file="test1.xsl")

            2) xsltproc.set_source("cat.xml")\r
               result = xsltproc.transform_to_string(stylesheet_file="test1.xsl")


            3) node = saxon_proc.parse_xml(xml_text="<in/>")\r
               result = xsltproc.transform_to_string(stylesheet_file="test1.xsl", xdm_node= node)
        """

        cdef char * c_sourcefile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "source_file":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else "" 
            if key == "stylesheet_file":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdm_node":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
          elif len(kwds) > 0:
            raise Warning("Warning: transform_to_string should only the following keyword arguments: (source_file, stylesheet_file, xdm_node)")

        cdef const char* c_string            
        if len(kwds) == 0:
          c_string = self.thisxptr.transformToString()
        else:     
          c_string = self.thisxptr.transformFileToString(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)

        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def transform_to_file(self, **kwds):
        """
        transform_to_file(self, **kwds)
        Execute transformation to a file. It is possible to specify the as an argument or using the set_output_file method.       
        Args:
            **kwds: Possible optional arguments: source_file (str) or xdm_node (PyXdmNode). Other allowed argument: stylesheet_file (str), output_file (str)


        Example:

            1) xsltproc.transform_to_file(source_file="cat.xml", stylesheet_file="test1.xsl", output_file="result.xml")

            2) xsltproc.set_source("cat.xml")\r
               xsltproc.setoutput_file("result.xml")\r
               xsltproc.transform_to_file(stylesheet_file="test1.xsl")


            3) node = saxon_proc.parse_xml(xml_text="<in/>")\r
               xsltproc.transform_to_file(output_file="result.xml", stylesheet_file="test1.xsl", xdm_node= node)        
        """
        cdef char * c_sourcefile
        cdef char * c_outputfile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        py_output_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "source_file":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else ""
            if key == "output_file":
              py_output_string = value.encode('UTF-8') if value is not None else None
              c_outputfile = py_output_string if value is not None else ""  
            if key == "stylesheet_file":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdm_node":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
            
        if len(kwds) == 0:
          self.thisxptr.transformToFile()
        else:     
          self.thisxptr.transformFileToFile(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL, c_outputfile if py_output_string is not None else NULL)


     def transform_to_value(self, **kwds):
        """
        transform_to_value(self, **kwds)
        Execute transformation to an Xdm Node

        Args:
            **kwds: Possible optional arguments: source_file (str) or xdm_node (PyXdmNode). Other allowed argument: stylesheet_file (str)



        Returns:
            PyXdmNode: Result of the transformation as an PyXdmNode object


        Example:

            1) node = xsltproc.transform_to_value(source_file="cat.xml", stylesheet_file="test1.xsl")

            2) xsltproc.set_source("cat.xml")\r
               node = xsltproc.transform_to_value(stylesheet_file="test1.xsl")


            3) node = saxon_proc.parse_xml(xml_text="<in/>")\r
               node = xsltproc.transform_tovalue(stylesheet_file="test1.xsl", xdm_node= node)        
        """
        cdef char * c_sourcefile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "source_file":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else ""  
            if key == "stylesheet_file":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdm_node":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
        cdef PyXdmValue py_xdm_value = PyXdmValue()
            
        if len(kwds) == 0:
          py_xdm_value.thisvptr = self.thisxptr.transformToValue()
        else:     
          py_xdm_value.thisvptr = self.thisxptr.transformFileToValue(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)
        return py_xdm_value
     
     def compile_stylesheet(self, **kwds):
        """
        compile_stylesheet(self, **kwds)
        Compile a stylesheet  received as text, uri or as a node object. The compiled stylesheet is cached and available for execution later. It is also possible to save the compiled stylesheet (SEF file) given the option 'save' and 'output_file'
   
        Args:
            **kwds: Possible keyword arguments stylesheet_text (str), stylesheet_uri (str) or stylsheetnode (PyXdmNode). Also possible to add the options save (boolean) and output_file, which creates an exported stylesheet to file (SEF). 

        Example:
            1. xsltproc.compile_stylesheet(stylesheet_text="<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' version='2.0'>       <xsl:param name='values' select='(2,3,4)' /><xsl:output method='xml' indent='yes' /><xsl:template match='*'><output><xsl:value-of select='//person[1]'/><xsl:for-each select='$values' ><out><xsl:value-of select='. * 3'/></out></xsl:for-each></output></xsl:template></xsl:stylesheet>")

            2. xsltproc.compile_stylesheet(stylesheet_file="test1.xsl", save=True, output_file="test1.sef")
        """
        py_error_message = "CompileStylesheet should only be one of the keyword option: (stylesheet_text|stylesheet_uri|stylesheet_node), also in allowed in addition the optional keyword 'save' boolean with the keyword 'outputfile' keyword"
        if len(kwds) >3:
          raise Exception(py_error_message)
        cdef char * c_outputfile
        cdef char * c_stylesheet
        py_output_string = None
        py_stylesheet_string = None
        py_save = False
        cdef int option = 0
        cdef PyXdmNode py_xdmNode = None
        if kwds.keys() >= {"stylesheet_text", "stylesheet_uri"}:
          raise Exception(py_error_message)
        if kwds.keys() >= {"stylesheet_text", "stylesheet_node"}:
          raise Exception(py_error_message)
        if kwds.keys() >= {"stylesheet_node", "stylesheet_uri"}:
          raise Exception(py_error_message)

        if ("save" in kwds) and kwds["save"]==True:
          del kwds["save"]
          if "output_file" not in kwds:
            raise Exception("Output file option not in keyword arugment for compile_stylesheet")
          py_output_string = kwds["output_file"].encode('UTF-8')
          c_outputfile = py_output_string
          if "stylesheet_text" in kwds:
            py_stylesheet_string = kwds["stylesheet_text"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromStringAndSave(c_stylesheet, c_outputfile)
          elif "stylesheet_uri" in kwds:
            py_stylesheet_string = kwds["stylesheet_uri"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromFileAndSave(c_stylesheet, c_outputfile)
          elif "stylesheetNode" in kwds:
            py_xdmNode = kwds["stylesheet_node"]
            #if not isinstance(py_value, PyXdmNode):
              #raise Exception("StylesheetNode keyword arugment is not of type XdmNode")
            #value = PyXdmNode(py_value)
            self.thisxptr.compileFromXdmNodeAndSave(py_xdmNode.derivednptr, c_outputfile)
          else:
            raise Exception(py_error_message)
        else:
          if "stylesheet_text" in kwds:
            py_stylesheet_string = kwds["stylesheet_text"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromString(c_stylesheet)
          elif "stylesheet_uri" in kwds:
            py_stylesheet_string = kwds["stylesheetUri"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromFile(c_stylesheet)
          elif "stylesheet_node" in kwds:
            py_xdmNode = kwds["stylesheet_node"]
            #if not isinstance(py_value, PyXdmNode):
              #raise Exception("StylesheetNode keyword arugment is not of type XdmNode")
            #value = PyXdmNode(py_value)
            self.thisxptr.compileFromXdmNode(py_xdmNode.derivednptr)
          else:
            raise Exception(py_error_message)

     def release_stylesheet(self):
        """
        release_stylesheet(self)
        Release cached stylesheet

        """
        self.thisxptr.releaseStylesheet()

     def exception_occurred(self):
        """
        exception_occurred(self)
        Checks for pending exceptions without creating a local reference to the exception object
        Returns:
            boolean: True when there is a pending exception; otherwise return False        

        """
        return self.thisxptr.exceptionOccurred()

     def check_exception(self):
        """
        check_exception(self)
        Check for exception thrown and get message of the exception.
  
        Returns:
            str: Returns the exception message if thrown otherwise return None

        """
        cdef const char* c_string = self.thisxptr.checkException()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def exception_clear(self):
        """
        exception_clear(self)
        Clear any exception thrown

        """
        self.thisxptr.exceptionClear()

     def exception_count(self):
        """
        excepton_count(self)
        Get number of errors reported during execution.

        Returns:
            int: Count of the exceptions thrown during execution
        """
        return self.thisxptr.exceptionCount()

     def get_error_message(self, index):
        """
        get_error_message(self, index)
        A transformation may have a number of errors reported against it. Get the ith error message if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The message of the i'th exception. Return None if the i'th exception does not exist.
        """
        cdef const char* c_string = self.thisxptr.getErrorMessage(index)
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def get_error_code(self, index):
        """
        get_error_code(self, index)
        A transformation may have a number of errors reported against it. Get the i'th error code if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The error code associated with the i'th exception. Return None if the i'th exception does not exist.

        """
        cdef const char* c_string = self.thisxptr.getErrorCode(index)
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

cdef class PyXQueryProcessor:
     """An PyXQueryProcessor object represents factory to compile, load and execute queries. """

     cdef saxoncClasses.XQueryProcessor *thisxqptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        """
        __cinit__(self)
        Constructor for PyXQueryProcessor

        """
        self.thisxqptr = NULL

     def __dealloc__(self):
        """
        dealloc(self)


        """
        if self.thisxqptr != NULL:
           del self.thisxqptr

     def set_context(self, ** kwds):
        """
        set_context(self, **kwds)
        Set the initial context for the query
   
        Args:
            **kwds : Possible keyword argument file_name (str) or xdm_item (PyXdmItem)

        """
        py_error_message = "Error: set_context should only contain one of the following keyword arguments: (file_name|xdm_item)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef py_value = None
        cdef py_value_string = None
        cdef char * c_source
        cdef PyXdmItem xdm_item = None
        if "file_name" in kwds:
            py_value = kwds["file_name"]
            py_value_string = py_value.encode('UTF-8') if py_value is not None else None
            c_source = py_value_string if py_value is not None else "" 
            self.thisxptr.setContextFromFile(c_source)
        elif "xdm_item" in kwds:
            xdm_item = kwds["xdm_item"]
            self.thisxptr.setContext(xdm_item.derivednptr)
        else:
          raise Exception(py_error_message)

     def set_output_file(self, output_file):
        """
        set_output_file(self, output_file)
        Set the output file where the result is sent

        Args:
            output_file (str): Name of the output file
        """
        py_value_string = output_file.encode('UTF-8') if output_file is not None else None
        c_outfile = py_value_string if output_file is not None else ""
        self.thisxqptr.setOutputFile(c_outfile)

     def set_parameter(self, name, PyXdmValue value):
        """
        set_parameter(self, name, PyXdmValue value)
        Set the value of a query parameter

        Args:
            name (str): the name of the stylesheet parameter, as a string. For namespaced parameter use the JAXP solution i.e. "{uri}name
            value (PyXdmValue): the value of the query parameter, or None to clear a previously set value

        """
        self.thisxqptr.setParameter(name, value.thisvptr)

     def remove_parameter(self, name):
        """
        remove_parameter(self, name)
        Remove the parameter given by name from the PyXQueryProcessor. The parameter will not have any affect on the query if it has not yet been executed

        Args:
            name (str): The name of the query parameter

        Returns:
            bool: True if the removal of the parameter has been successful, False otherwise.

        """
        py_value_string = name.encode('UTF-8') if name is not None else None
        c_name = py_value_string if name is not None else ""
        self.thisxqptr.removeParameter(c_name)

     def set_property(self, name, str value):
        """
        set_property(self, name, value)
        Set a property specific to the processor in use.
 
        Args:
            name (str): The name of the property
            value (str): The value of the property

        Example:
            PyXQueryProcessor: set serialization properties (names start with '!' i.e. name "!method" -> "xml")\r
            'o':outfile name,\r
            'dtd': Possible values 'on' or 'off' to set DTD validation,\r 
            'resources': directory to find Saxon data files,\r 
            's': source as file name,\r
        """

        py_name_string = name.encode('UTF-8') if name is not None else None
        c_name = py_name_string if name is not None else ""

        py_value_string = value.encode('UTF-8') if value is not None else None
        c_value = py_value_string if value is not None else ""
        self.thisxqptr.setProperty(c_name, c_value)

     def clear_parameters(self):
        """
        clear_parameter(self)
        Clear all parameters set on the processor
        """
        self.thisxqptr.clearParameters()

     def clear_properties(self):
        """
        clear_parameter(self)
        Clear all properties set on the processor
        """
        self.thisxqptr.clearProperties()

     def set_updating(self, updating):
        """
        set_updating(self, updating)
        Say whether the query is allowed to be updating. XQuery update syntax will be rejected during query compilation unless this flag is set. XQuery Update is supported only under Saxon-EE/C.


        Args:
            updating (bool): true if the query is allowed to use the XQuery Update facility (requires Saxon-EE/C). If set to false, the query must not be an updating query. If set to true, it may be either an updating or a non-updating query.


        """
        self.thisxqptr.setUpdating(updating)

     def run_query_to_value(self, ** kwds):
        """
        run_query_to_value(self, **kwds)
        Execute query and output result as an PyXdmValue object 

        Args:
            **kwds: Keyword arguments with the possible options input_file_name (str) or input_xdm_item (PyXdmItem). Possible to supply query with the arguments 'query_file' or 'query_text', which are of type str.

        Returns:
            PyXdmValue: Output result as an PyXdmValue
        """
        cdef PyXdmValue val
        if len(kwds) == 0:
          val = PyXdmValue()
          val.thisvptr = self.thisxqptr.runQueryToValue()
          return val
        elif "input_file_name" in kwds:
          self.set_context(kwds["input_file_name"])
        elif "input_xdm_item" in kwds:
          self.set_context(xdm_item=(kwds["xdm_item"]))

        if "query_file" in kwds:
          self.set_query_file(kwds["output_file_name"])
        elif "query_text" in kwds:
          self.set_query_content(kwds["query_text"])
        val = PyXdmValue()
        val.thisvptr = self.thisxqptr.runQueryToValue()
        return val

     def run_query_to_string(self, ** kwds):
        """
        run_query_to_string(self, **kwds)
        Execute query and output result as a string 

        Args:
            **kwds: Keyword arguments with the possible options input_file_name (str) or input_xdm_item (PyXdmItem). Possible to supply query with the arguments 'query_file' or 'query_text', which are of type str.

        Returns:
            str: Output result as a string
        """
        if len(kwds) == 0:
          return self.thisxqptr.runQueryToString()
        if "input_file_name" in kwds:
          self.set_context(kwds["input_file_name"])
        elif "input_xdm_item" in kwds:
          self.set_context(xdm_item=(kwds["xdm_item"]))
        if "query_file" in kwds:
          self.set_query_file(kwds["output_file_name"])
        elif "query_text" in kwds:
          self.set_query_content(kwds["query_text"])
        return self.thisxqptr.runQueryToString()

     def run_query_to_file(self, ** kwds):
        """
        run_query_to_file(self, **kwds)
        Execute query with the result saved to file

        Args:
            **kwds: Keyword arguments with the possible options input_file_name (str) or input_xdm_item (PyXdmItem). The Query can be supplied with the arguments 'query_file' or 'query_text', which are of type str. The name of the output file is specified as the argument output_file_name.


        """
        if len(kwds) == 0:
          self.thisxqptr.runQueryToFile()
        if "input_file_name" in kwds:
          self.set_context(file_name=(kwds["input_file_name"]))
        elif "input_xdm_item" in kwds:
          self.set_context(xdm_item=(kwds["xdm_item"]))
        if "output_file_name" in kwds:
          self.set_output_file(kwds["output_file_name"])
        else:
          raise Exception("Error: output_file_name required in method run_query_to_file")

        if "query_file" in kwds:
          self.set_query_file(kwds["output_file_name"])
        elif "query_text" in kwds:
          self.set_query_content(kwds["query_text"])
        self.thisxqptr.runQueryToFile()

     def declare_namespace(self, prefix, uri):
        """
        declare_namespace(self, prefix, uri)
        Declare a namespace binding part of the static context for queries compiled using this. This binding may be overridden by a binding that appears in the query prolog. The namespace binding will form part of the static context of the query, but it will not be copied into result trees unless the prefix is actually used in an element or attribute name.

        Args:
            prefix (str): The namespace prefix. If the value is a zero-length string, this method sets the default namespace for elements and types.
            uri (uri) : The namespace URI. It is possible to specify a zero-length string to "undeclare" a namespace; in this case the prefix will not be available for use, except in the case where the prefix is also a zero length string, in which case the absence of a prefix implies that the name is in no namespace.

        """
        py_prefix_string = prefix.encode('UTF-8') if prefix is not None else None
        c_prefix = py_prefix_string if prefix is not None else ""
        py_uri_string = uri.encode('UTF-8') if uri is not None else None
        c_uri = py_uri_string if uri is not None else ""
        self.thisxqptr.declareNamespace(c_prefix, c_uri)

     def set_query_file(self, file_name):
        """
        set_query_file(self, file_name)
        Set the query to be executed as a file

        Args:
            file_name (str): The file name for the query


        """
        py_filename_string = file_name.encode('UTF-8') if file_name is not None else None
        c_filename = py_filename_string if file_name is not None else ""
        self.thisxqptr.setQueryFile(c_filename)

     def set_query_content(self, content):
        """
        set_query_content(self)
        Set the query to be executed as a string

        Args:
            content (str): The query content suplied as a string

        """
        py_content_string = content.encode('UTF-8') if content is not None else None
        c_content = py_content_string if content is not None else ""
        self.thisxqptr.setQueryContent(c_content)

     def set_query_base_uri(self, base_uri):
        """
        set_query_base_uri(self, base_uri)
        Set the static base query for the query     

        Args:
            base_uri (str): The static base URI; or None to indicate that no base URI is available
        """
        py_content_string = base_uri.encode('UTF-8') if base_uri is not None else None
        c_content = py_content_string if base_uri is not None else ""
        self.thisxqptr.setQueryBaseURI(base_uri)

     def set_cwd(self, cwd):
        """
        set_cwd(self, cwd)
        Set the current working directory.

        Args:
            cwd (str): current working directory
        """
        py_cwd_string = cwd.encode('UTF-8') if cwd is not None else None
        c_cwd = py_cwd_string if cwd is not None else ""
        self.thisxqptr.setcwd(cwd)

     def check_exception(self):
        """
        check_exception(self)
        Check for exception thrown and get message of the exception.
  
        Returns:
            str: Returns the exception message if thrown otherwise return None

        """
        return self.thisxqptr.checkException()

     def exceptionOccurred(self):
        """
        exceptionOccurred(self)
        Checks for pending exceptions without creating a local reference to the exception object

        Returns:
            boolean: True when there is a pending exception; otherwise return False

        """
        return self.thisxqptr.exceptionOccurred()

     def exception_clear(self):
        """
        exception_clear(self)
        Clear any exception thrown

        """
        self.thisxqptr.exceptionClear()

     def exception_count(self):
        """
        excepton_count(self)
        Get number of errors reported during execution.

        Returns:
            int: Count of the exceptions thrown during execution
        """
        return self.thisxqptr.exceptionCount()

     def get_error_message(self, index):
        """
        get_error_message(self, index)
        A transformation may have a number of errors reported against it. Get the ith error message if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The message of the i'th exception. Return None if the i'th exception does not exist.
        """
        return self.thisxqptr.getErrorMessage(index)

     def get_error_code(self, index):
        """
        get_error_code(self, index)
        A transformation may have a number of errors reported against it. Get the i'th error code if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The error code associated with the i'th exception. Return None if the i'th exception does not exist.

        """
        return self.thisxqptr.getErrorCode(index)

cdef class PyXPathProcessor:
     """An XPathProcessor represents factory to compile, load and execute the XPath query. """
     cdef saxoncClasses.XPathProcessor *thisxpptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        """
        cinit(self)
        Constructor for PyXPathProcessor 

        """
        self.thisxpptr = NULL
     def __dealloc__(self):
        """
        dealloc(self)


        """
        if self.thisxpptr != NULL:
           del self.thisxpptr

     def set_base_uri(self, uri):
        """
        set_base_uri(self, uri)
        Set the static base URI for XPath expressions compiled using this PyXPathCompiler. The base URI is part of the static context, and is used to resolve any relative URIs appearing within an XPath expression, for example a relative URI passed as an argument to the doc() function. If no static base URI is supplied, then the current working directory is used.


        Args:
            uri (str): This string will be used as the static base URI

        """
        self.thisxpptr.setBaseURI(uri)

     def evaluate(self, xpath_str):
        """
        evaluate(self, xpath_str)

        Args:
            xpath_str (str): The XPath query suplied as a string

        Returns:
            PyXdmValue: 

        """
        py_string = xpath_str.encode('UTF-8') if xpath_str is not None else None
        c_xpath = py_string if xpath_str is not None else ""
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxpptr.evaluate(c_xpath)
        return val

     def evaluate_single(self, xpath_str):
        """
        evaluate_single(self, xpath_str)

        Args:
            xpath_str (str): The XPath query suplied as a string

        Returns:
            PyXdmItem: A single Xdm Item is returned 

        """
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisxpptr.evaluateSingle(xpath_str)
        return val
     def setContextItem(self, PyXdmItem item):
        self.thisxpptr.setContextItem(item.derivedptr)
     def set_cwd(self, cwd):
        """
        set_cwd(self, cwd)
        Set the current working directory.

        Args:
            cwd (str): current working directory
        """
        self.thisxpptr.setcwd(cwd)

     def set_context_file(self, file_name):
        """
        set_context_file(self, file_name)
        Set the query context source as a file location
  
        Args:
   


        """
        self.thisxpptr.setContextFile(file_name)
     def effective_boolean_value(self, xpathStr):
        return self.thisxpptr.effectiveBooleanValue(xpathStr)
     def set_parameter(self, name, PyXdmValue value):
        """
        set_parameter(self, name, PyXdmValue value)
        Set the value of a XPath parameter

        Args:
            name (str): the name of the XPath parameter, as a string. For namespaced parameter use the JAXP solution i.e. "{uri}name
            value (PyXdmValue): the value of the query parameter, or None to clear a previously set value

        """
        self.thisxpptr.setParameter(name, value.thisvptr)

     def remove_parameter(self, name):
        """
        remove_parameter(self, name)
        Remove the parameter given by name from the PyXPathProcessor. The parameter will not have any affect on the XPath if it has not yet been executed

        Args:
            name (str): The name of the XPath parameter

        Returns:
            bool: True if the removal of the parameter has been successful, False otherwise.

        """
        self.thisxpptr.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxpptr.setProperty(name, value)
     def declareNamespace(self, prefix, uri):
        self.thisxpptr.declareNamespace(prefix, uri)
     def clear_parameters(self):
        """
        clear_parameter(self)
        Clear all parameters set on the processor
        """
        self.thisxpptr.clearParameters()
     def clear_properties(self):
        """
        clear_parameter(self)
        Clear all properties set on the processor
        """
        self.thisxpptr.clearProperties()
     def check_exception(self):
        """
        check_exception(self)
        Check for exception thrown and get message of the exception.
  
        Returns:
            str: Returns the exception message if thrown otherwise return None

        """
        return self.thisxpptr.checkException()
     def exception_occurred(self):
        """
        exception_occurred(self)
        Check if an exception has occurred internally within Saxon/C

        Returns:
            boolean: True or False if an exception has been reported internally in Saxon/C
        """

        return self.thisxpptr.exceptionOccurred()

     def exception_clear(self):
        """
        exception_clear(self)
        Clear any exception thrown

        """
        self.thisxpptr.exceptionClear()
     def exception_count(self):
        """
        excepton_count(self)
        Get number of errors reported during execution.

        Returns:
            int: Count of the exceptions thrown during execution
        """
        return self.thisxpptr.exceptionCount()
     def get_error_message(self, index):
        """
        get_error_message(self, index)
        A transformation may have a number of errors reported against it. Get the ith error message if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The message of the i'th exception. Return None if the i'th exception does not exist.
        """
        self.thisxpptr.getErrorMessage(index)
     def get_error_code(self, index):
        """
        get_error_code(self, index)
        A transformation may have a number of errors reported against it. Get the i'th error code if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The error code associated with the i'th exception. Return None if the i'th exception does not exist.

        """
        return self.thisxpptr.getErrorCode(index)


cdef class PySchemaValidator:
     """An SchemaValidator represents factory for validating instance documents against a schema."""
     cdef saxoncClasses.SchemaValidator *thissvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thissvptr = NULL
     def __dealloc__(self):
        if self.thissvptr != NULL:
           del self.thissvptr

     def set_cwd(self, cwd):
        """
        set_cwd(self, cwd)
        Set the current working directory.

        Args:
            cwd (str): current working directory
        """
        self.thissvptr.setcwd(cwd)

     def register_schema_from_file(self, xsd):
        self.thissvptr.registerSchemaFromFile(xsd)
     def register_schema_from_string(self, schemaStr):
        self.thissvptr.registerSchemaFromString(schemaStr)
     def setOutputFile(self, outputFile):
        self.thissvptr.setOutputFile(outputFile)
     def validate(self, sourceFile):
        self.thissvptr.validate(sourceFile)
     def validate_to_node(self, sourceFile):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thissvptr.validateToNode(sourceFile)
        return val
     def set_source_node(self, PyXdmNode source):
        self.thissvptr.setSourceNode(source.derivednptr)

     def get_validation_report(self):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thissvptr.getValidationReport()
        return val
     def set_parameter(self, name, value):
        """
        set_parameter(self, name, PyXdmValue value)
        Set the value of the parameter for the Schema validator

        Args:
            name (str): the name of the schema parameter, as a string. For namespaced parameter use the JAXP solution i.e. "{uri}name
            value (PyXdmValue): the value of the parameter, or None to clear a previously set value

        """
        self.thissvprt.setParameter(value.thisvptr)
     def remove_parameter(self, name):
        """
        remove_parameter(self, name)
        Remove the parameter given by name from the PySchemaValidator. The parameter will not have any affect on the SchemaValidator if it has not yet been executed

        Args:
            name (str): The name of the schema parameter

        Returns:
            bool: True if the removal of the parameter has been successful, False otherwise.

        """
        self.thissvprt.removeParameter(name)
     def setProperty(self, name, value):
        self.thissvprt.setProperty(name, value.thisvptr)
     def clear_parameters(self):
        """
        clear_parameter(self)
        Clear all parameters set on the processor
        """
        self.thissvprt.clearParameters()
     def clear_properties(self):
        """
        clear_parameter(self)
        Clear all properties set on the processor
        """
        self.thissvprt.clearProperties()
     def exception_occurred(self):
        """
        exception_occurred(self)
        Check if an exception has occurred internally within Saxon/C

        Returns:
            boolean: True or False if an exception has been reported internally in Saxon/C
        """
        return self.thissvprt.exceptionOccurred()
     def check_exception(self):
        """
        check_exception(self)
        Check for exception thrown and get message of the exception.
  
        Returns:
            str: Returns the exception message if thrown otherwise return None


        """
        return self.thissvprt.checkException()
     def exception_clear(self):
        """
        exception_clear(self)
        Clear any exception thrown

        """
        self.thissvprt.exceptionClear()
     def exception_count(self):
        """
        excepton_count(self)
        Get number of errors reported during execution of the schema.

        Returns:
            int: Count of the exceptions thrown during execution
        """
        return self.thissvprt.exceptionCount()
     def get_error_message(self, index):
        """
        get_error_message(self, index)
        A transformation may have a number of errors reported against it. Get the ith error message if there are any errors

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The message of the i'th exception. Return None if the i'th exception does not exist.
        """
        return self.thissvprt.getErrorMessage(index)
     def get_error_code(self, index):
        """
        get_error_code(self, index)
        A transformation may have a number of errors reported against it. Get the i'th error code if there are any errors.

        Args:
            index (int): The i'th exception
        
        Returns:
            str: The error code associated with the i'th exception. Return None if the i'th exception does not exist.

        """
        return self.thissvprt.getErrorCode(index)
     def set_lax(self, lax):
        """
        set_lax(self, lax)
        The validation mode may be either strict or lax. \r
        The default is strict; this method may be called to indicate that lax validation is required. With strict validation, validation fails if no element declaration can be located for the outermost element. With lax validation, the absence of an element declaration results in the content being considered valid.
        
        Args:
            lax (boolean): lax True if validation is to be lax, False if it is to be strict

        """
        self.thissvprt.setLax(lax)

cdef class PyXdmValue:
     """Value in the XDM data model. A value is a sequence of zero or more items, each item being either an atomic value or a node. """    
     cdef saxoncClasses.XdmValue *thisvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        """
        cinit(self)
        Constructor for PyXdmValue

        """
        if type(self) is PyXdmValue:
            self.thisvptr = new saxoncClasses.XdmValue() 
     def __dealloc__(self):
        if type(self) is PyXdmValue:
           del self.thisvptr


     def add_xdm_item(self, PyXdmItem value):
        """
        add_xdm_tem(self, PyXdmItem value)
        Add PyXdmItem to the Xdm sequence

        Args:
            value (PyXdmItem): The PyXdmItem object
        """
        self.thisvptr.addXdmItem(value.derivedptr)

     def get_head(self):
        """
        get_head(self)
        Get the first item in the sequence

        Returns:
            PyXdmItem: The PyXdmItem or None if the sequence is empty

        """
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.getHead()
        if val.derivedptr == NULL :
            return None
        else:
            return val

     def item_at(self, index):
        """
        item_at(self, index)
        Get the n'th item in the value, counting from zero.
        
        Args:
            index (int): the index of the item required. Counting from zero
        Returns:
            PyXdmItem: Get the item indicated at the index. If the item does not exist return None.
        

        """
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.itemAt(index)
        if val.derivedptr == NULL:
            return None
        else:
            return val

     def size(self):
        """
        size(self)
        Get the number of items in the sequence
        
        Returns:
            int: The coutn of items in the sequence
        """
        return self.thisvptr.size()

     def to_string(self):
        """
        to_string(self)

        """
        cdef const char* c_string = self.thisvptr.toString()
        if c_string == NULL:
            return None
        else:
            ustring = c_string.decode('UTF-8')
            return ustring 

cdef class PyXdmItem(PyXdmValue):
     cdef saxoncClasses.XdmItem *derivedptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmItem:
            self.derivedptr = self.thisvptr = new saxoncClasses.XdmItem()
     def __dealloc__(self):
        if type(self) is PyXdmValue:
            del self.derivedptr
     def get_string_value(self):
        return self.derivedptr.getStringValue()
     def is_atomic(self):
        return self.derivedptr.isAtomic()

cdef class PyXdmNode(PyXdmItem):
     cdef saxoncClasses.XdmNode *derivednptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.derivednptr = self.derivedptr = self.thisvptr = NULL
     def __dealloc__(self):
        del self.derivednptr

     def get_node_kind(self):
        return self.derivednptr.getNodeKind()

      # def getNodeName(self):
         # return self.derivednptr.getNodeName()

     def get_typed_value(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.derivednptr.getTypedValue()
        return val

     def get_base_uri(self):
        return self.derivednptr.getBaseUri()

     def get_string_value(self):
        return self.derivedptr.getStringValue()

     def to_string(self):
        return self.derivedptr.toString()

     def get_parent(self):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.derivednptr.getParent()
        return val

     def get_attribute_value(self, stri):
        return self.derivednptr.getAttributeValue(stri)

     def get_attribute_count(self):
        return self.derivednptr.getAttributeCount()

     #  def getAttributeNodes(self):


      # def getChildren(self):

      # def getChildCount(self):


cdef class PyXdmAtomicValue(PyXdmItem):
     cdef saxoncClasses.XdmAtomicValue *derivedaptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmNode:
            self.derivedaptr = self.derivedptr = self.thisvptr = new saxoncClasses.XdmAtomicValue()
     def __dealloc__(self):
        del self.derivedaptr

