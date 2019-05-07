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
        self.thisxptr.setcwd(cwd)

     def setSourceFromFile(self, filename):
        self.thisxptr.setSourceFromFile(filename)
     def setOutputFile(self, outfile):
        self.thisxptr.setOutputFile(outfile)
     def setJustInTimeCompilation(self, jit):
        self.thisxptr.setJustInTimeCompilation(jit)
     def setParameter(self, name, PyXdmValue value):
        self.thisxptr.setParameter(name, value.thisvptr)
     def getParameter(self, name):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getParameter(name)
        return val
     def removeParameter(self, name):
        return self.thisxptr.removeParameter(name)
     def setProperty(self, name, value):
        self.thisxptr.setProperty(name, value)
     def clearParameters(self):
        self.thisxptr.clearParameters()
     def clearProperties(self):
        self.thisxptr.clearProperties()
     def getXslMessages(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.getXslMessages()
        return val

     def transformFileToFile(self, sourcefile, stylesheetfile, outputfile):
        self.thisxptr.transformFileToFile(sourcefile, stylesheetfile, outputfile)	
     def transformFileToString(self, sourcefile, stylesheetfile):
        return self.thisxptr.transformFileToString(sourcefile, stylesheetfile)
     def transformFileToValue(self, sourcefile, stylesheetfile):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.transformFileToValue(sourcefile, stylesheetfile)
        return val
     def compileFromFile(self, stylesheet):
        self.thisxptr.compileFromFile(stylesheet)
     def compileFromString(self, stylesheet):
        self.thisxptr.compileFromString(stylesheet)
     def compileFromStringAndSave(self, stylesheet, filename):
        self.thisxptr.compileFromStringAndSave(stylesheet, filename)
     def compileFromFileAndSave(self, xslFilename, filename):
        self.thisxptr.compileFromFileAndSave(xslFilename, filename)
     def compileFromXdmNode(self, PyXdmNode node):
        self.thisxptr.compileFromXdmNode(node.derivednptr)
     def releaseStylesheet(self):
        self.thisxptr.releaseStylesheet()
     def transformToString(self):
        return self.thisxptr.transformToString()
     def transformToValue(self):
        cdef PyXdmValue val = PyXdmValue()
        val.thisvptr = self.thisxptr.transformToValue()
        return val
     def transformToFile(self):
        self.thisxptr.transformToFile()
     def exceptionOccurred(self):
        return self.thisxptr.exceptionOccurred()
     def checkException(self):
        return self.thisxptr.checkException()
     def exceptionClear(self):
        self.thisxptr.exceptionClear()
     def exceptionCount(self):
        return self.thisxptr.exceptionCount()
     def getErrorMessage(self, i):
        return self.thisxptr.getErrorMessage(i)
     def getErrorCode(self, i):
        return self.thisxptr.getErrorCode(i)

cdef class PyXQueryProcessor:
     cdef saxoncClasses.XQueryProcessor *thisxqptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxqptr = NULL
     def __dealloc__(self):
        if self.thisxqptr != NULL:
           del self.thisxqptr
     def setContextItem(self, PyXdmItem value):
        self.thisxqptr.setContextItem(value.derivedptr)
     def setOutputFile(self, outfile):
        self.thisxqptr.setOutputFile(outfile)
     def setContextItemFromFile(self, filename): 
        self.thisxqptr.setContextItemFromFile(filename)
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
        if self.thisvptr != NULL:
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
         
    
