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
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeBooleanValue(b)
        return val

    def makeQNameValue(self, str1):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeQNameValue(str1)
        return val

    def makeAtomicValue(self, typei, value):
        cdef PyXdmAtomicValue val = PyXdmAtomicValue()
        val.derivedaptr = val.derivedptr = val.thisvptr = self.thisptr.makeAtomicValue(typei, value)
        return val

    def getStringValue(self, PyXdmItem item):
        return self.thisptr.getStringValue(item.derivedptr)

    def parseXmlFromString(self, source):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromString(source)
        return val

    def parseXmlFromFile(self, source):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromFile(source)
        return val

    def parseXmlFromUri(self,  source):
        cdef PyXdmNode val = PyXdmNode()
        val.derivednptr = val.derivedptr = val.thisvptr = self.thisptr.parseXmlFromUri(source)
        return val

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
        self.thisxprt.setcwd(cwd)
     def getcwd(self):
        return self.thisxprt.getcwd()
     def setSourceFromFile(self, filename):
        self.thisxprt.setSourceFromFile(filename)
     def setOutputFile(self, outfile):
        self.thisxprt.setOutputFile(outfile)
     def setJustInTimeCompilation(self, jit):
        self.thisxprt.setJustInTimeCompilation(jit)
     def setParameter(self, name, value):
        self.thisxprt.setParameter(value.thisvptr)
     def getParameter(self, name):
        return self.thisxprt.getParameter(name)
     def removeParameter(self, name):
        self.thisxprt.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxprt.setProperty(name, value.thisvptr)
     def clearParameters(self):
        self.thisxprt.clearParameters()
     def clearProperties(self):
        self.thisxprt.clearProperties()
     def getXslMessages(self):
        return self.thisxprt.getXslMessages()
     def transformFileToFile(self, sourcefile, stylesheetfile, outputfile):
        self.thisxprt.transformFileToFile(sourcefile, stylesheetfile, outputfile)	
     def transformFileToString(self, sourcefile, stylesheetfile):
        return self.thisxprt.transformFileToString(sourcefile, stylesheetfile)
     def transformFileToValue(self, sourcefile, stylesheetfile):
        cdef PyXdmValue val = PyXdmValue()
        val.thisxptr = self.thisxprt.transformFileToValue(sourcefile, stylesheetfile)
        return val
     def compileFromFile(self, stylesheet):
        self.thisxprt.compileFromFile(stylesheet)
     def compileFromString(self, stylesheet):
        self.thisxprt.compileFromString(stylesheet)
     def compileFromStringAndSave(self, stylesheet, filename):
        self.thisxprt.compileFromStringAndSave(stylesheet, filename)
     def compileFromFileAndSave(self, xslFilename, filename):
        self.thisxprt.compileFromFileAndSave(xslFilename, filename)
     def compileFromXdmNode(self, node):
        self.thisxprt.compileFromXdmNode(node.thisvptr)
     def releaseStylesheet(self):
        self.thisxprt.releaseStylesheet()
     def transformToString(self):
        return self.thisxprt.transformToString()
     def transformToValue(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisxptr = self.thisxprt.transformToValue()
        return val
     def transformToFile(self):
        self.thisxprt.transformToFile()
     def exceptionOccurred(self):
        return self.thisxprt.exceptionOccurred()
     def checkException(self):
        return self.thisxprt.checkException()
     def exceptionClear(self):
        self.thisxprt.exceptionClear()
     def exceptionCount(self):
        return self.thisxprt.exceptionCount()
     def getErrorMessage(self, i):
        return self.thisxprt.getErrorMessage(i)
     def getErrorCode(self, i):
        return self.thisxprt.getErrorCode(i)

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
     cdef saxoncClasses.SchemaValidator *thissvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thissvptr = NULL
     def __dealloc__(self):
        if self.thissvptr != NULL:
           del self.thissvptr

cdef class PyXdmValue:
     cdef saxoncClasses.XdmValue *thisvptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmValue:
            self.thisvptr = new saxoncClasses.XdmValue() 
     def __dealloc__(self):
        if self.thisvptr != NULL:
           del self.thisvptr

cdef class PyXdmItem(PyXdmValue):
     cdef saxoncClasses.XdmItem *derivedptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmItem:
            self.derivedptr = self.thisvptr = new saxoncClasses.XdmItem()
     def __dealloc__(self):
        if type(self) is PyXdmValue:
            del self.derivedptr


cdef class PyXdmNode(PyXdmItem):
     cdef saxoncClasses.XdmNode *derivednptr      # hold a C++ instance which we're wrapping

     # def __cinit__(self):
      #   if type(self) is PyXdmNode:
      #       self.derivednptr = self.derivedptr = self.thisvptr = new saxoncClasses.XdmNode()
     def __dealloc__(self):
        del self.derivednptr

cdef class PyXdmAtomicValue(PyXdmItem):
     cdef saxoncClasses.XdmAtomicValue *derivedaptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        if type(self) is PyXdmNode:
            self.derivedaptr = self.derivedptr = self.thisvptr = new saxoncClasses.XdmAtomicValue()
     def __dealloc__(self):
        del self.derivedaptr
         
    
