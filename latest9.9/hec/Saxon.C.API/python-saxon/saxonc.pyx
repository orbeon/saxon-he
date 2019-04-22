cimport saxoncClasses
from libcpp cimport bool
   
cdef class PySaxonProcessor:
    cdef saxoncClasses.SaxonProcessor *thisptr      # hold a C++ instance which we're wrapping
    def __cinit__(self, bool license):
        self.thisptr = new saxoncClasses.SaxonProcessor(license)
    def __dealloc__(self):
        del self.thisptr
    def version(self):
        return self.thisptr.version()
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

    def newXQueryProcessor(self)
        cdef PyXQueryProcessor val = PyXQueryProcessor()
        val.thisxqtr = self.thisptr.newXQueryProcessor()
        return val

    def newXPathProcessor(self)
        cdef PyXPathProcessor val = PyXPathProcessor()
        val.thisxpptr = self.thisptr.newXPathProcessor()
        return val

    def newSchemaValidator(self)
        cdef PySchemaValidator val = PySchemaValidator()
        val.thisvptr = self.thisptr.newSchemaValidator()
        return val

    def makeStringValue(self, str1)

    def makeStringValue(self, str1)

    def makeIntegerValue(self, i)

    def makeDoubleValue(double d)

    def makeFloatValue(float)

    def makeLongValue(long l)

    def makeBooleanValue(bool b)

    def makeQNameValue(const char* str)

    def makeAtomicValue(const char* type, const char* value)

    def getStringValue(XdmItem * item)

    def parseXmlFromString(const char* source)

    def parseXmlFromFile(const char* source)

    def parseXmlFromUri(const char* source)

    def getNodeKind(jobject)

    def isSchemaAware()

    def exceptionOccurred()

    def exceptionClear()
  
    def getException()



cdef class PyXsltProcessor:
     cdef saxoncClasses.XsltProcessor *thisxptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxptr = NULL
     def __dealloc__(self):
        if self.thisxptr != NULL:
           del self.thisxptr
     def setcwd(self, cwd)
        self.thisxprt.setcwd(cwd)
     def getcwd(self)
        return self.thisxprt.getcwd()
     def setSourceFromFile(self, filename)
        self.thisxprt.setSourceFromFile(filename)
     def setOutputFile(self, outfile)
        self.thisxprt.setOutputFile(outfile)
     def setJustInTimeCompilation(self, jit)
        self.thisxprt.setJustInTimeCompilation(jit)
     def setParameter(self name, value)
        self.thisxprt.setParameter(value.thisvptr)
     def getParameter(self, name)
        return self.thisxprt.getParameter(name)
     def removeParameter(self, name)
        self.thisxprt.removeParameter(name)
     def setProperty(self, name, value)
        self.thisxprt.setProperty(name, value.thisvptr)
     def clearParameters(self)
        self.thisxprt.clearParameters()
     def clearProperties(self)
        self.thisxprt.clearProperties()
     def getXslMessages(self)
        return self.thisxprt.getXslMessages()
     def transformFileToFile(self, sourcefile, stylesheetfile, outputfile)
        self.thisxprt.transformFileToFile(sourcefile, stylesheetfile, outputfile)	
     def transformFileToString(self, sourcefile, stylesheetfile)
        return self.thisxprt.transformFileToString(sourcefile, stylesheetfile)
     def transformFileToValue(self, sourcefile, stylesheetfile)
        cdef PyXdmValue val = PyXdmValue()
        val.thisxptr = self.thisxprt.transformFileToValue(sourcefile, stylesheetfile)
        return val
     def compileFromFile(self, stylesheet)
        self.thisxprt.compileFromFile(stylesheet)
     def compileFromString(self, stylesheet)
        self.thisxprt.compileFromString(stylesheet)
     def compileFromStringAndSave(self, stylesheet, filename)
        self.thisxprt.compileFromStringAndSave(stylesheet, filename)
     def compileFromFileAndSave(self, xslFilename, filename)
        self.thisxprt.compileFromFileAndSave(xslFilename, filename)
     def compileFromXdmNode(self, node)
        self.thisxprt.compileFromXdmNode(node.thisvptr)
     def releaseStylesheet(self)
        self.thisxprt.releaseStylesheet()
     def transformToString(self)
        return self.thisxprt.transformToString()
     def transformToValue(self)
        cdef PyXdmValue val = PyXdmValue()
        val.thisxptr = self.thisxprt.transformToValue()
	return val
     def transformToFile(self)
        self.thisxprt.transformToFile()
     def exceptionOccurred(self)
        return self.thisxprt.exceptionOccurred()
     def checkException(self)
        self.thisxprt.checkException()
     def exceptionClear(self)
        self.thisxprt.exceptionClear()
     def exceptionCount(self)

     def getErrorMessage(self, i)

     def getErrorCode(self, i)

cdef class PyXQueryProcessor:
     cdef saxoncClasses.XQueryProcessor *thisxqptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxqptr = NULL
     def __dealloc__(self):
        if self.thisxqptr != NULL:
           del self.thisxqptr

cdef class PyXPathProcessor:
     cdef saxoncClasses.XPathProcessor *thisxpptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxpptr = NULL
     def __dealloc__(self):
        if self.thisxpptr != NULL:
           del self.thisxpptr

cdef class PySchemaValidator:
     cdef saxoncClasses.SchemaValidator *thisvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisvptr = NULL
     def __dealloc__(self):
        if self.thisvptr != NULL:
           del self.thisvptr

cdef class PyXdmValue:
     cdef saxoncClasses.XdmValue *thisvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisvptr = new saxoncClasses.XdmValue()
     def __dealloc__(self):
        if self.thisvptr != NULL:
           del self.thisvptr


         
    
