# distutils: language = c++

cimport saxoncClasses
from libcpp cimport bool
   
cdef class PySaxonProcessor:
    cdef saxoncClasses.SaxonProcessor *thisptr      # hold a C++ instance which we're wrapping
    def __cinit__(self, license):
        self.thisptr = new saxoncClasses.SaxonProcessor(license)
    def __dealloc__(self):
        del self.thisptr
    def version(self):
        cdef const char* c_string = self.thisptr.version()
        ustring = c_string.decode('UTF-8')
        return ustring

    def versionn(self):
        cdef const char* c_string = self.thisptr.version()
        ustring = c_string.decode('UTF-8')
        return ustring
    def release(self):
        self.thisptr.release()
    def set_cwd(self, cwd):
        self.thisptr.setcwd(cwd)
    def get_cwd(self):
        self.thisptr.getcwd()
    def set_resources_directory(self, dir_):
        self.thisptr.setResourcesDirectory(dir_)
    def get_resources_directory(self):
        return self.thisptr.getResourcesDirectory()
    def set_configuration_property(self, name, value):
        self.thisptr.setConfigurationProperty(name, value)
    def clear_configuration_properties(self):
        self.thisptr.clearConfigurationProperties()
    def is_schema_aware(self):
        return self.thisprt.isSchemaAware()
    def new_xslt_processor(self):
        cdef PyXsltProcessor val = PyXsltProcessor()
        val.thisxptr = self.thisptr.newXsltProcessor()
        return val

    def new_xquery_processor(self):
        cdef PyXQueryProcessor val = PyXQueryProcessor()
        val.thisxqptr = self.thisptr.newXQueryProcessor()
        return val

    def new_xpath_processor(self):
        cdef PyXPathProcessor val = PyXPathProcessor()
        val.thisxpptr = self.thisptr.newXPathProcessor()
        return val

    def new_schema_validator(self):
        cdef PySchemaValidator val = PySchemaValidator()
        val.thissvptr = self.thisptr.newSchemaValidator()
        return val

    def make_string_value(self, str_):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeStringValue(str_)
        return val

    def make_integer_value(self, i):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeIntegerValue(i)
        return val

    def make_double_value(self, d):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeDoubleValue(d)
        return val

    def make_float_value(self, fl):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeFloatValue(fl)
        return val

    def make_long_value(self, lg):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeLongValue(lg)
        return val

    def make_boolean_value(self, b):
        cdef bool c_b = b
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeBooleanValue(c_b)
        return val

    def make_qname_value(self, str_):
        py_value_string = str_.encode('UTF-8') if str_ is not None else None
        cdef char * c_str_ = py_value_string if str_ is not None else ""
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeQNameValue(c_str_)
        return val

    def make_atomic_value(self, value_type, value):
        py_valueType_string = value_type.encode('UTF-8') if value_type is not None else None
        cdef char * c_valueType_string = py_valueType_string if value_type is not None else ""
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeAtomicValue(c_valueType_string, value)
        return val

    def get_string_value(self, PyXdmItem item):
        return self.thisptr.getStringValue(item.derivedptr)

    def parse_xml(self, **kwds):
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
        return self.thisptr.exceptionOccurred()

    def exception_clear(self):
        self.thisptr.exceptionClear()



cdef class PyXsltProcessor:
     cdef saxoncClasses.XsltProcessor *thisxptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxptr = NULL
     def __dealloc__(self):
        if self.thisxptr != NULL:
           del self.thisxptr
     def set_cwd(self, cwd):
        py_cwd_string = cwd.encode('UTF-8') if cwd is not None else None
        cdef char * c_cwd = py_cwd_string if cwd is not None else "" 
        self.thisxptr.setcwd(c_cwd)

     def set_source(self, **kwds):
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

