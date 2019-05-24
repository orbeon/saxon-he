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

    def version(self):
        """
        version(self)
        Get the Saxon Version.

        Return:
            str: The Saxon version
        """
        cdef const char* c_string = self.thisptr.version()
        ustring = c_string.decode('UTF-8')
        return ustring


    def release(self):
        """
        release(self) 
        Clean up and destroy Java VM to release memory used."""

        self.thisptr.release()

    def set_cwd(self, cwd):
        """
        set_cwd(self, cwd)
        Set the current working directory.

        Args:
            cwd (str): current working directory
        """
        py_value_string = cwd.encode('UTF-8') if cwd is not None else None
        cdef char * c_str_ = py_value_string if cwd is not None else ""
        self.thisptr.setcwd(cwd)
    def get_cwd(self):
        """
        get_cwd(self)
        Get the current working directory.

        Returns:
            str: The current working directory

        """
        cdef const char* c_string = self.thisptr.getcwd()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring


    def set_resources_directory(self, dir_):
        """
        set_resources_directory(self, dir_) 
        Set saxon resources directory 

        Args:
            dir_ (str): A string of the resources directory which Saxon will use


        """
        py_value_string = dir_.encode('UTF-8') if dir_ is not None else None
        cdef char * c_str_ = py_value_string if dir_ is not None else ""
        self.thisptr.setResourcesDirectory(c_str_)
    def get_resources_directory(self):
        """
        get_resources_directory(self)
        Get the resources directory

        Returns:
            str: The resources directory

        """
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
    def is_schema_aware(self):
        """
        is_schema_aware(self)
        Check is the processor is Schema aware. A licensed Saxon-EE/C product is schema aware 

        Returns:
            bool: IS the processor is schema aware
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
        Create an QName Xdm value from string representation in clark notation

        Args:
            str_ (str): The value given in a string form in clark notation. {uri}local

        Returns:
            PyAtomicValue: The corresonding value

        """
        return self.thisptr.getStringValue(item.derivedptr)

    def parse_xml(self, **kwds):
        """
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
        Check if an exception has occurred internally within Saxon/C

        Returns:
            boolean: True or False if an exception has been reported internally in Saxon/C
        """
        return self.thisptr.exceptionOccurred()

    def exception_clear(self):
        """
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
        """Set the current working directory.

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

     def set_output_file(self, outputfile):
        py_filename_string = outputfile.encode('UTF-8') if outputfile is not None else None
        cdef char * c_outputfile = py_filename_string if outputfile is not None else ""
        self.thisxptr.setOutputFile(c_outputfile)

     def set_jit_compilation(self, bool jit):
        cdef bool c_jit
        c_jit = jit
        self.thisxptr.setJustInTimeCompilation(c_jit)
        #else:
        #raise Warning("setJustInTimeCompilation: argument must be a boolean type. JIT not set")
     def set_parameter(self, name, PyXdmValue value):
        self.thisxptr.setParameter(name, value.thisvptr)

     def get_parameter(self, name):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getParameter(c_name)
        return val

     def remove_parameter(self, name):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        return self.thisxptr.removeParameter(c_name)
     def set_property(self, name, value):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        self.thisxptr.setProperty(c_name, value)
     def clear_parameters(self):
        self.thisxptr.clearParameters()
     def clear_properties(self):
        self.thisxptr.clearProperties()
     def get_xsl_messages(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getXslMessages()
        return val

     def transform_to_string(self, **kwds):
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
          elif key == "xdm_value":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
          elif len(kwds) > 0:
            raise Warning("Warning: transform_to_string should only the following keyword arguments: (source_file, stylesheet_file, xdm_value)")

        cdef const char* c_string            
        if len(kwds) == 0:
          c_string = self.thisxptr.transformToString()
        else:     
          c_string = self.thisxptr.transformFileToString(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)

        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def transform_to_file(self, **kwds):
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
          elif key == "xdm_value":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
            
        if len(kwds) == 0:
          self.thisxptr.transformToFile()
        else:     
          self.thisxptr.transformFileToFile(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL, c_outputfile if py_output_string is not None else NULL)


     def transform_to_value(self, **kwds):
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
          elif key == "xdm_value":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
        cdef PyXdmValue py_xdm_value = PyXdmValue()
            
        if len(kwds) == 0:
          py_xdm_value.thisvptr = self.thisxptr.transformToValue()
        else:     
          py_xdm_value.thisvptr = self.thisxptr.transformFileToValue(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)
        return py_xdm_value
     
     def compile_stylesheet(self, **kwds):
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
        self.thisxptr.releaseStylesheet()

     def exception_occurred(self):
        return self.thisxptr.exceptionOccurred()
     def check_exception(self):
        cdef const char* c_string = self.thisxptr.checkException()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def exception_clear(self):
        self.thisxptr.exceptionClear()
     def exception_count(self):
        return self.thisxptr.exceptionCount()
     def getError_message(self, i):
        cdef const char* c_string = self.thisxptr.getErrorMessage(i)
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def getError_code(self, i):
        cdef const char* c_string = self.thisxptr.getErrorCode(i)
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

cdef class PyXQueryProcessor:
     cdef saxoncClasses.XQueryProcessor *thisxqptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxqptr = NULL
     def __dealloc__(self):
        if self.thisxqptr != NULL:
           del self.thisxqptr

     def set_context(self, ** kwds):
        py_error_message = "Error: setContext should only contain one of the following keyword arguments: (fileName|xdmItem)"
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
        py_value_string = output_file.encode('UTF-8') if output_file is not None else None
        c_outfile = py_value_string if output_file is not None else ""
        self.thisxqptr.setOutputFile(c_outfile)
     def set_parameter(self, name, PyXdmValue value):
        self.thisxqptr.setParameter(name, value.thisvptr)
     def remove_parameter(self, name):
        py_value_string = name.encode('UTF-8') if name is not None else None
        c_name = py_value_string if name is not None else ""
        self.thisxqptr.removeParameter(c_name)
     def set_property(self, name, str value):
        py_name_string = name.encode('UTF-8') if name is not None else None
        c_name = py_name_string if name is not None else ""

        py_value_string = value.encode('UTF-8') if value is not None else None
        c_value = py_value_string if value is not None else ""
        self.thisxqptr.setProperty(c_name, c_value)
     def clear_parameters(self):
        self.thisxqptr.clearParameters()
     def clear_properties(self):
        self.thisxqptr.clearProperties()
     def set_updating(self, updating):
        self.thisxqptr.setUpdating(updating)

     def run_query_to_value(self, ** kwds):
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

     def runQueryToString(self, ** kwds):
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

     def runQueryToFile(self, ** kwds):
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
        py_prefix_string = prefix.encode('UTF-8') if prefix is not None else None
        c_prefix = py_prefix_string if prefix is not None else ""
        py_uri_string = uri.encode('UTF-8') if uri is not None else None
        c_uri = py_uri_string if uri is not None else ""
        self.thisxqptr.declareNamespace(c_prefix, c_uri)

     def set_query_file(self, file_name):
        py_filename_string = file_name.encode('UTF-8') if file_name is not None else None
        c_filename = py_filename_string if file_name is not None else ""
        self.thisxqptr.setQueryFile(c_filename)

     def set_query_content(self, content):
        py_content_string = content.encode('UTF-8') if content is not None else None
        c_content = py_content_string if content is not None else ""
        self.thisxqptr.setQueryContent(c_content)
     def set_query_base_uri(self, baseURI):
        self.thisxqptr.setQueryBaseURI(baseURI)
     def setcwd(self, cwd):
        self.thisxqptr.setcwd(cwd)
     def check_exception(self):
        return self.thisxqptr.checkException()
     def exceptionOccurred(self):
        return self.thisxqptr.exceptionOccurred()
     def exception_clear(self):
        self.thisxqptr.exceptionClear()
     def exception_count(self):
        return self.thisxqptr.exceptionCount()
     def getErrorMessage(self, i):
        return self.thisxqptr.getErrorMessage(i)
     def get_error_code(self, i):
        return self.thisxqptr.getErrorCode(i)

cdef class PyXPathProcessor:
     cdef saxoncClasses.XPathProcessor *thisxpptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxpptr = NULL
     def __dealloc__(self):
        if self.thisxpptr != NULL:
           del self.thisxpptr
     def set_base_uri(self, uriStr):
        self.thisxpptr.setBaseURI(uriStr)
     def evaluate(self, xpathStr):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxpptr.evaluate(xpathStr)
        return val
     def evaluate_single(self, xpathStr):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisxpptr.evaluateSingle(xpathStr)
        return val
     def setContextItem(self, PyXdmItem item):
        self.thisxpptr.setContextItem(item.derivedptr)
     def setcwd(self, cwd):
        self.thisxpptr.setcwd(cwd)
     def set_context_File(self, file_name): 
        self.thisxpptr.setContextFile(file_name)
     def effective_boolean_value(self, xpathStr):
        return self.thisxpptr.effectiveBooleanValue(xpathStr)
     def set_parameter(self, name, PyXdmValue value):
        self.thisxpptr.setParameter(name, value.thisvptr)
     def remove_parameter(self, name):
        self.thisxpptr.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxpptr.setProperty(name, value)
     def declareNamespace(self, prefix, uri):
        self.thisxpptr.declareNamespace(prefix, uri)
     def clear_parameters(self):
        self.thisxpptr.clearParameters()
     def clear_properties(self):
        self.thisxpptr.clearProperties()
     def check_exception(self):
        return self.thisxpptr.checkException()
     def exception_occurred(self):
        return self.thisxpptr.exceptionOccurred()
     def exception_clear(self):
        self.thisxpptr.exceptionClear()
     def exception_count(self):
        return self.thisxpptr.exceptionCount()
     def get_error_message(self, i):
        self.thisxpptr.getErrorMessage(i)
     def get_error_code(self, i):
        return self.thisxpptr.getErrorCode(i)


cdef class PySchemaValidator:
     cdef saxoncClasses.SchemaValidator *thissvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thissvptr = NULL
     def __dealloc__(self):
        if self.thissvptr != NULL:
           del self.thissvptr

     def setcwd(self, cwd):
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
        self.thissvprt.setParameter(value.thisvptr)
     def remove_parameter(self, name):
        self.thissvprt.removeParameter(name)
     def setProperty(self, name, value):
        self.thissvprt.setProperty(name, value.thisvptr)
     def clear_parameters(self):
        self.thissvprt.clearParameters()
     def clearProperties(self):
        self.thissvprt.clearProperties()
     def exceptionOccurred(self):
        return self.thissvprt.exceptionOccurred()
     def check_exception(self):
        return self.thissvprt.checkException()
     def exception_clear(self):
        self.thissvprt.exceptionClear()
     def exception_count(self):
        return self.thissvprt.exceptionCount()
     def get_error_message(self, i):
        return self.thissvprt.getErrorMessage(i)
     def get_error_code(self, i):
        return self.thissvprt.getErrorCode(i)
     def setLax(self, l):
        self.thissvprt.setLax(l)

cdef class PyXdmValue:
     cdef saxoncClasses.XdmValue *thisvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmValue:
            self.thisvptr = new saxoncClasses.XdmValue() 
     def __dealloc__(self):
        if type(self) is PyXdmValue:
           del self.thisvptr


     def add_xdm_item(self, PyXdmItem value):
        self.thisvptr.addXdmItem(value.derivedptr)

     def get_head(self):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.getHead()
        return val

     def item_at(self, i):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.itemAt(i)
        return val

     def size(self):
        return self.thisvptr.size()

     def to_string(self):
        return self.thisvptr.toString()

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

