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

    def release(self):
        self.thisptr.release()
    def setcwd(self, cwd):
        self.thisptr.setcwd(cwd)
    def getcwd(self):
        self.thisptr.getcwd()
    def setResourcesDirectory(self, diri):
        self.thisptr.setResourcesDirectory(diri)
    def getResourcesDirectory(self):
        return self.thisptr.getResourcesDirectory()
    def setConfigurationProperty(self, name, value):
        self.thisptr.setConfigurationProperty(name, value)
    def clearConfigurationProperties(self):
        self.thisptr.clearConfigurationProperties()
    def isSchemaAware(self):
        return self.thisprt.isSchemaAware()
    def newXsltProcessor(self):
        cdef PyXsltProcessor val = PyXsltProcessor()
        val.thisxptr = self.thisptr.newXsltProcessor()
        return val

    def newXQueryProcessor(self):
        cdef PyXQueryProcessor val = PyXQueryProcessor()
        val.thisxqptr = self.thisptr.newXQueryProcessor()
        return val

    def newXPathProcessor(self):
        cdef PyXPathProcessor val = PyXPathProcessor()
        val.thisxpptr = self.thisptr.newXPathProcessor()
        return val

    def newSchemaValidator(self):
        cdef PySchemaValidator val = PySchemaValidator()
        val.thissvptr = self.thisptr.newSchemaValidator()
        return val

    def makeStringValue(self, str1):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeStringValue(str1)
        return val

    def makeIntegerValue(self, i):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeIntegerValue(i)
        return val

    def makeDoubleValue(self, d):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeDoubleValue(d)
        return val

    def makeFloatValue(self, fl):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeFloatValue(fl)
        return val

    def makeLongValue(self, lg):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeLongValue(lg)
        return val

    def makeBooleanValue(self, b):
        cdef bool c_b = b
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeBooleanValue(c_b)
        return val

    def makeQNameValue(self, str1):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeQNameValue(str1)
        return val

    def makeAtomicValue(self, valueType, value):
        py_valueType_string = valueType.encode('UTF-8') if valueType is not None else None
        cdef char * c_valueType_string = py_valueType_string if valueType is not None else ""
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeAtomicValue(c_valueType_string, value)
        return val

    def getStringValue(self, PyXdmItem item):
        return self.thisptr.getStringValue(item.derivedptr)

    def parseXml(self, **kwds):
        py_error_message = "Error: parseXml should only contain one of the following keyword arguments: (xmlFileName|xmlText|xmlUri)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef PyXdmNode val = None
        cdef py_value = None
        cdef char * c_xml_string = NULL
        if "xmlText" in kwds:
          py_value = kwds["xmlText"]
          py_xml_text_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_xml_text_string if py_value is not None else "" 
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromString(c_xml_string)
          return val
        elif "xmlFileName" in kwds:
          py_value = kwds["xmlFileName"]
          py_filename_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_filename_string if py_value is not None else ""
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromFile(c_xml_string)
          return val 
        elif "xmlUri" in kwds:
          py_value = kwds["xmlUri"]
          py_uri_string = py_value.encode('UTF-8') if py_value is not None else None
          c_xml_string = py_uri_string if py_value is not None else ""
          val = PyXdmNode()
          val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromUri(c_xml_string)
          return val
        else:
           raise Exception(py_error_message)

    def exceptionOccurred(self):
        return self.thisptr.exceptionOccurred()

    def exceptionClear(self):
        self.thisptr.exceptionClear()



cdef class PyXsltProcessor:
     cdef saxoncClasses.XsltProcessor *thisxptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxptr = NULL
     def __dealloc__(self):
        if self.thisxptr != NULL:
           del self.thisxptr
     def setcwd(self, cwd):
        py_cwd_string = cwd.encode('UTF-8') if cwd is not None else None
        cdef char * c_cwd = py_cwd_string if cwd is not None else "" 
        self.thisxptr.setcwd(c_cwd)

     def setSource(self, **kwds):
        py_error_message = "Error: setSource should only contain one of the following keyword arguments: (fileName|node)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef py_value = None
        cdef py_value_string = None
        cdef char * c_source
        cdef PyXdmNode xdm_node = None
        if "fileName" in kwds:
            py_value = kwds["fileName"]
            py_value_string = py_value.encode('UTF-8') if py_value is not None else None
            c_source = py_value_string if py_value is not None else "" 
            self.thisxptr.setSourceFromFile(c_source)
        elif "node" in kwds:
            xdm_node = kwds["node"]
            self.thisxptr.setSourceFromXdmNode(xdm_node.derivednptr)
        else:
          raise Exception(py_error_message)

     def setOutputFile(self, outputfile):
        py_filename_string = outputfile.encode('UTF-8') if outputfile is not None else None
        cdef char * c_outputfile = py_filename_string if outputfile is not None else ""
        self.thisxptr.setOutputFile(c_outputfile)
     def setJustInTimeCompilation(self, bool jit):
        cdef bool c_jit
        c_jit = jit
        self.thisxptr.setJustInTimeCompilation(c_jit)
        #else:
        #raise Warning("setJustInTimeCompilation: argument must be a boolean type. JIT not set")
     def setParameter(self, name, PyXdmValue value):
        self.thisxptr.setParameter(name, value.thisvptr)
     def getParameter(self, name):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getParameter(c_name)
        return val
     def removeParameter(self, name):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        return self.thisxptr.removeParameter(c_name)
     def setProperty(self, name, value):
        py_name_string = name.encode('UTF-8') if name is not None else None
        cdef char * c_name = py_name_string if name is not None else ""
        self.thisxptr.setProperty(c_name, value)
     def clearParameters(self):
        self.thisxptr.clearParameters()
     def clearProperties(self):
        self.thisxptr.clearProperties()
     def getXslMessages(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getXslMessages()
        return val

     def transformToString(self, **kwds):
        cdef char * c_sourcefile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "sourcefile":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else "" 
            if key == "stylesheetfile":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdmvalue":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)

        cdef const char* c_string            
        if len(kwds) == 0:
          c_string = self.thisxptr.transformToString()
        else:     
          c_string = self.thisxptr.transformFileToString(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)

        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def transformToFile(self, **kwds):
        cdef char * c_sourcefile
        cdef char * c_outputfile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        py_output_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "sourcefile":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else ""
            if key == "outputfile":
              py_output_string = value.encode('UTF-8') if value is not None else None
              c_outputfile = py_output_string if value is not None else ""  
            if key == "stylesheetfile":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdmvalue":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
            
        if len(kwds) == 0:
          self.thisxptr.transformToFile()
        else:     
          self.thisxptr.transformFileToFile(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL, c_outputfile if py_output_string is not None else NULL)


     def transformToValue(self, **kwds):
        cdef char * c_sourcefile
        cdef char * c_stylesheetfile
        py_source_string = None
        py_stylesheet_string = None
        for key, value in kwds.items():
          if isinstance(value, str):
            if key == "sourcefile":
              py_source_string = value.encode('UTF-8') if value is not None else None
              c_sourcefile = py_source_string if value is not None else ""  
            if key == "stylesheetfile":
              py_stylesheet_string = value.encode('UTF-8') if value is not None else None
              c_stylesheetfile = py_stylesheet_string if value is not None else ""
          elif key == "xdmvalue":
            if isinstance(value, PyXdmNode):
              self.setSourceFromXdmNode(value)
        cdef PyXdmValue py_xdm_value = PyXdmValue()
            
        if len(kwds) == 0:
          py_xdm_value.thisvptr = self.thisxptr.transformToValue()
        else:     
          py_xdm_value.thisvptr = self.thisxptr.transformFileToValue(c_sourcefile if py_source_string is not None else NULL, c_stylesheetfile if py_stylesheet_string is not None else NULL)
        return py_xdm_value
     
     def compileStylesheet(self, **kwds):
        py_error_message = "CompileStylesheet should only be one of the keyword option: (stylesheetText|stylesheetUri|stylesheetNode), also in allowed in addition the optional keyword 'save' boolean with the keyword 'outputfile' keyword"
        if len(kwds) >3:
          raise Exception(py_error_message)
        cdef char * c_outputfile
        cdef char * c_stylesheet
        py_output_string = None
        py_stylesheet_string = None
        py_save = False
        cdef int option = 0
        cdef PyXdmNode py_xdmNode = None
        if kwds.keys() >= {"stylesheetText", "stylesheetUri"}:
          raise Exception(py_error_message)
        if kwds.keys() >= {"stylesheetText", "stylesheetNode"}:
          raise Exception(py_error_message)
        if kwds.keys() >= {"stylesheetNode", "stylesheetUri"}:
          raise Exception(py_error_message)

        if ("save" in kwds) and kwds["save"]==True:
          del kwds["save"]
          if "outputFile" not in kwds:
            raise Exception("Output file option not in keyword arugment for compileStylesheet")
          py_output_string = kwds["outputFile"].encode('UTF-8')
          c_outputfile = py_output_string
          if "stylesheetText" in kwds:
            py_stylesheet_string = kwds["stylesheetText"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromStringAndSave(c_stylesheet, c_outputfile)
          elif "stylesheetUri" in kwds:
            py_stylesheet_string = kwds["stylesheetUri"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromFileAndSave(c_stylesheet, c_outputfile)
          elif "stylesheetNode" in kwds:
            py_xdmNode = kwds["stylesheetNode"]
            #if not isinstance(py_value, PyXdmNode):
              #raise Exception("StylesheetNode keyword arugment is not of type XdmNode")
            #value = PyXdmNode(py_value)
            self.thisxptr.compileFromXdmNodeAndSave(py_xdmNode.derivednptr, c_outputfile)
          else:
            raise Exception(py_error_message)
        else:
          if "stylesheetText" in kwds:
            py_stylesheet_string = kwds["stylesheetText"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromString(c_stylesheet)
          elif "stylesheetUri" in kwds:
            py_stylesheet_string = kwds["stylesheetUri"].encode('UTF-8')
            c_stylesheet = py_stylesheet_string
            self.thisxptr.compileFromFile(c_stylesheet)
          elif "stylesheetNode" in kwds:
            py_xdmNode = kwds["stylesheetNode"]
            #if not isinstance(py_value, PyXdmNode):
              #raise Exception("StylesheetNode keyword arugment is not of type XdmNode")
            #value = PyXdmNode(py_value)
            self.thisxptr.compileFromXdmNode(py_xdmNode.derivednptr)
          else:
            raise Exception(py_error_message)

     def releaseStylesheet(self):
        self.thisxptr.releaseStylesheet()

     def exceptionOccurred(self):
        return self.thisxptr.exceptionOccurred()
     def checkException(self):
        cdef const char* c_string = self.thisxptr.checkException()
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def exceptionClear(self):
        self.thisxptr.exceptionClear()
     def exceptionCount(self):
        return self.thisxptr.exceptionCount()
     def getErrorMessage(self, i):
        cdef const char* c_string = self.thisxptr.getErrorMessage(i)
        ustring = c_string.decode('UTF-8') if c_string is not NULL else None
        return ustring

     def getErrorCode(self, i):
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

     def setContext(self, ** kwds):
        py_error_message = "Error: setContext should only contain one of the following keyword arguments: (fileName|xdmItem)"
        if len(kwds) != 1:
          raise Exception(py_error_message)
        cdef py_value = None
        cdef py_value_string = None
        cdef char * c_source
        cdef PyXdmItem xdm_item = None
        if "fileName" in kwds:
            py_value = kwds["fileName"]
            py_value_string = py_value.encode('UTF-8') if py_value is not None else None
            c_source = py_value_string if py_value is not None else "" 
            self.thisxptr.setContextFromFile(c_source)
        elif "xdmItem" in kwds:
            xdm_item = kwds["xdmItem"]
            self.thisxptr.setContext(xdm_item.derivednptr)
        else:
          raise Exception(py_error_message)

     def setOutputFile(self, outfile):
        self.thisxqptr.setOutputFile(outfile)
     def setParameter(self, name, PyXdmValue value):
        self.thisxqptr.setParameter(name, value.thisvptr)
     def removeParameter(self, name):
        self.thisxqptr.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxqptr.setProperty(name, value)
     def clearParameters(self):
        self.thisxqptr.clearParameters()
     def clearProperties(self):
        self.thisxqptr.clearProperties()
     def setUpdating(self, updating):
        self.thisxqptr.setUpdating(updating)

     def executeQueryToFile(self, infilename, ofilename, query):
        self.thisxqptr.executeQueryToFile(infilename, ofilename, query)
     def executeQueryToValue(self, infilename, query):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxqptr.executeQueryToValue(infilename, query)
        return val
     def executeQueryToString(self, infilename, query):
        self.thsxqptr.executeQueryToString(infilename, query)
     def runQueryToValue(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxqptr.runQueryToValue()
        return val
     def runQueryToString(self):
        return self.thisxqptr.runQueryToString()

     def runQueryToFile(self):
        self.thisxqptr.runQueryToFile()

     def declareNamespace(self, prefix, uri):
        self.thisxqptr.declareNamespace(prefix, uri)
     def setQueryFile(self, filename):
        self.thisxqptr.setQueryFile(filename)
     def setQueryContent(self, content):
        self.thisxqptr.setQueryContent(content)
     def setQueryBaseURI(self, baseURI):
        self.thisxqptr.setQueryBaseURI(baseURI)
     def setcwd(self, cwd):
        self.thisxqptr.setcwd(cwd)
     def checkException(self):
        return self.thisxqptr.checkException()
     def exceptionOccurred(self):
        return self.thisxqptr.exceptionOccurred()
     def exceptionClear(self):
        self.thisxqptr.exceptionClear()
     def exceptionCount(self):
        return self.thisxqptr.exceptionCount()
     def getErrorMessage(self, i):
        return self.thisxqptr.getErrorMessage(i)
     def getErrorCode(self, i):
        return self.thisxqptr.getErrorCode(i)

cdef class PyXPathProcessor:
     cdef saxoncClasses.XPathProcessor *thisxpptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxpptr = NULL
     def __dealloc__(self):
        if self.thisxpptr != NULL:
           del self.thisxpptr
     def setBaseURI(self, uriStr):
        self.thisxpptr.setBaseURI(uriStr)
     def evaluate(self, xpathStr):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxpptr.evaluate(xpathStr)
        return val
     def evaluateSingle(self, xpathStr):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisxpptr.evaluateSingle(xpathStr)
        return val
     def setContextItem(self, PyXdmItem item):
        self.thisxpptr.setContextItem(item.derivedptr)
     def setcwd(self, cwd):
        self.thisxpptr.setcwd(cwd)
     def setContextFile(self, filename): 
        self.thisxpptr.setContextFile(filename)
     def effectiveBooleanValue(self, xpathStr):
        return self.thisxpptr.effectiveBooleanValue(xpathStr)
     def setParameter(self, name, PyXdmValue value):
        self.thisxpptr.setParameter(name, value.thisvptr)
     def removeParameter(self, name):
        self.thisxpptr.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxpptr.setProperty(name, value)
     def declareNamespace(self, prefix, uri):
        self.thisxpptr.declareNamespace(prefix, uri)
     def clearParameters(self):
        self.thisxpptr.clearParameters()
     def clearProperties(self):
        self.thisxpptr.clearProperties()
     def checkException(self):
        return self.thisxpptr.checkException()
     def exceptionOccurred(self):
        return self.thisxpptr.exceptionOccurred()
     def exceptionClear(self):
        self.thisxpptr.exceptionClear()
     def exceptionCount(self):
        return self.thisxpptr.exceptionCount()
     def getErrorMessage(self, i):
        self.thisxpptr.getErrorMessage(i)
     def getErrorCode(self, i):
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

     def registerSchemaFromFile(self, xsd):
        self.thissvptr.registerSchemaFromFile(xsd)
     def registerSchemaFromString(self, schemaStr):
        self.thissvptr.registerSchemaFromString(schemaStr)
     def setOutputFile(self, outputFile):
        self.thissvptr.setOutputFile(outputFile)
     def validate(self, sourceFile):
        self.thissvptr.validate(sourceFile)
     def validateToNode(self, sourceFile):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thissvptr.validateToNode(sourceFile)
        return val
     def setSourceNode(self, PyXdmNode source):
        self.thissvptr.setSourceNode(source.derivednptr)

     def getValidationReport(self):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thissvptr.getValidationReport()
        return val
     def setParameter(self, name, value):
        self.thissvprt.setParameter(value.thisvptr)
     def removeParameter(self, name):
        self.thissvprt.removeParameter(name)
     def setProperty(self, name, value):
        self.thissvprt.setProperty(name, value.thisvptr)
     def clearParameters(self):
        self.thissvprt.clearParameters()
     def clearProperties(self):
        self.thissvprt.clearProperties()
     def exceptionOccurred(self):
        return self.thissvprt.exceptionOccurred()
     def checkException(self):
        return self.thissvprt.checkException()
     def exceptionClear(self):
        self.thissvprt.exceptionClear()
     def exceptionCount(self):
        return self.thissvprt.exceptionCount()
     def getErrorMessage(self, i):
        return self.thissvprt.getErrorMessage(i)
     def getErrorCode(self, i):
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


     def addXdmItem(self, PyXdmItem value):
        self.thisvptr.addXdmItem(value.derivedptr)

     def getHead(self):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.getHead()
        return val

     def itemAt(self, i):
        cdef PyXdmItem val = PyXdmItem()
        val.derivedptr = val.thisvptr = self.thisvptr.itemAt(i)
        return val

     def size(self):
        return self.thisvptr.size()

     def toString(self):
        return self.thisvptr.toString()

cdef class PyXdmItem(PyXdmValue):
     cdef saxoncClasses.XdmItem *derivedptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmItem:
            self.derivedptr = self.thisvptr = new saxoncClasses.XdmItem()
     def __dealloc__(self):
        if type(self) is PyXdmValue:
            del self.derivedptr
     def getStringValue(self):
        return self.derivedptr.getStringValue()
     def isAtomic(self):
        return self.derivedptr.isAtomic()

cdef class PyXdmNode(PyXdmItem):
     cdef saxoncClasses.XdmNode *derivednptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.derivednptr = self.derivedptr = self.thisvptr = NULL
     def __dealloc__(self):
        del self.derivednptr

     def getNodeKind(self):
        return self.derivednptr.getNodeKind()

      # def getNodeName(self):
         # return self.derivednptr.getNodeName()

     def getTypedValue(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.derivednptr.getTypedValue()
        return val

     def getBaseUri(self):
        return self.derivednptr.getBaseUri()

     def getStringValue(self):
        return self.derivedptr.getStringValue()

     def toString(self):
        return self.derivedptr.toString()

     def getParent(self):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.derivednptr.getParent()
        return val

     def getAttributeValue(self, stri):
        return self.derivednptr.getAttributeValue(stri)

     def getAttributeCount(self):
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
         
    
