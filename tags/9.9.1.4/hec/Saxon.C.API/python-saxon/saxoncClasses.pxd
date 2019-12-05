from libcpp cimport bool
from libcpp cimport string



cdef extern from "../SaxonProcessor.h":
    cdef cppclass SaxonProcessor:
        SaxonProcessor(bool) except +
        #SaxonProcessor(const char * configFile) except +
        bool license
        char * version()
        void release()

        # set the current working directory
        void setcwd(char* cwd)
        const char* getcwd()

        #SaxonProcessor * getProcessor()

        #set saxon resources directory
        void setResourcesDirectory(const char* dir)
        
        #get saxon resources directory
        const char * getResourcesDirectory()

        #Set a configuration property specific to the processor in use. 
        #Properties specified here are common across all the processors.
        void setConfigurationProperty(char * name, char * value)

        #Clear configuration properties specific to the processor in use. 
        void clearConfigurationProperties()

        bool isSchemaAware()

        XsltProcessor * newXsltProcessor()

        XQueryProcessor * newXQueryProcessor()

        XPathProcessor * newXPathProcessor()

        SchemaValidator * newSchemaValidator()

        XdmAtomicValue * makeStringValue(const char* str1)

        XdmAtomicValue * makeIntegerValue(int i)

        XdmAtomicValue * makeDoubleValue(double d)

        XdmAtomicValue * makeFloatValue(float)

        XdmAtomicValue * makeLongValue(long l)

        XdmAtomicValue * make_boolean_value(bool b)

        XdmAtomicValue * makeBooleanValue(bool b)

        XdmAtomicValue * makeQNameValue(const char* str)

        XdmAtomicValue * makeAtomicValue(const char* type, const char* value)

        const char * getStringValue(XdmItem * item)

        XdmNode * parseXmlFromString(const char* source)

        XdmNode * parseXmlFromFile(const char* source)

        XdmNode * parseXmlFromUri(const char* source)

        bool isSchemaAware()

        bool exceptionOccurred()

        void exceptionClear()


    cdef cppclass XsltProcessor:
        XsltProcessor() except +
        # set the current working directory
        void setcwd(const char* cwd)

        #Set the source document from an XdmNode for the transformation.
        void setSourceFromXdmNode(XdmNode * value)

        #Set the source from file for the transformation.
        void setSourceFromFile(const char * filename)

        #Set the output file of where the transformation result is sent
        void setOutputFile(const char* outfile)

        void setJustInTimeCompilation(bool jit)

        void setParameter(const char* name, XdmValue*value)

        XdmValue* getParameter(const char* name)

        bool removeParameter(const char* name)

        void setProperty(const char* name, const char* value)

        void clearParameters()

        void clearProperties()

        XdmValue * getXslMessages()


        void transformFileToFile(const char* sourcefile, const char* stylesheetfile, const char* outputfile)
        
        char * transformFileToString(const char* sourcefile, const char* stylesheetfile)

        XdmValue * transformFileToValue(const char* sourcefile, const char* stylesheetfile)

        void compileFromFile(const char* stylesheet)

        void compileFromString(const char* stylesheet)

        void compileFromStringAndSave(const char* stylesheet, const char* filename)

        void compileFromFileAndSave(const char* xslFilename, const char* filename)

        void compileFromXdmNodeAndSave(XdmNode * node, const char* outputfile)

        void compileFromXdmNode(XdmNode * node)

        void releaseStylesheet()

        char * transformToString()

        XdmValue * transformToValue()

        void transformToFile()

        bool exceptionOccurred()

        const char* checkException()

        void exceptionClear()
    
        int exceptionCount()

        const char * getErrorMessage(int)

        const char * getErrorCode(int)

    cdef cppclass SchemaValidator:
        SchemaValidator() except +

        void setcwd(const char* cwd)

        void registerSchemaFromFile(const char * xsd)

        void registerSchemaFromString(const char * schemaStr)

        void setOutputFile(const char * outputFile)

        void validate(const char * sourceFile)
   
        XdmNode * validateToNode(const char * sourceFile)

        void setSourceNode(XdmNode * source)

        XdmNode* getValidationReport()

        void setParameter(const char * name, XdmValue*value)

        bool removeParameter(const char * name)

        void setProperty(const char * name, const char * value)

        void clearParameters()

        void clearProperties()

        bool exceptionOccurred()

        const char* checkException()

        void exceptionClear()

        int exceptionCount()

    
        const char * getErrorMessage(int i)
     
        const char * getErrorCode(int i)

        void setLax(bool l)

    cdef cppclass XPathProcessor:
        XPathProcessor() except +

        void setBaseURI(const char * uriStr)

        XdmValue * evaluate(const char * xpathStr)
   
        XdmItem * evaluateSingle(const char * xpathStr)

        void setContextItem(XdmItem * item)
        
        void setcwd(const char* cwd)

        void setContextFile(const char * filename) 

        bool effectiveBooleanValue(const char * xpathStr)

        void setParameter(const char * name, XdmValue*value)

        bool removeParameter(const char * name)

        void setProperty(const char * name, const char * value)

        void declareNamespace(const char *prefix, const char * uri)

        void clearParameters()

        void clearProperties()

        bool exceptionOccurred()

        void exceptionClear()

        int exceptionCount()

        const char * getErrorMessage(int i)

        const char * getErrorCode(int i)

        const char* checkException()

    cdef cppclass XQueryProcessor:
        XQueryProcessor() except +

        void setContextItem(XdmItem * value) except +

        void setOutputFile(const char* outfile)

        void setContextItemFromFile(const char * filename) 

        void setParameter(const char * name, XdmValue*value)

        bool removeParameter(const char * name)

        void setProperty(const char * name, const char * value)

        void clearParameters()

        void clearProperties()

        void setUpdating(bool updating)

        XdmValue * runQueryToValue() except +
        const char * runQueryToString() except +

        void runQueryToFile() except +

        void declareNamespace(const char *prefix, const char * uri) except +

        void setQueryFile(const char* filename)

        void setQueryContent(const char* content)

        void setQueryBaseURI(const char * baseURI)

        void setcwd(const char* cwd)

        const char* checkException()

        bool exceptionOccurred()

        void exceptionClear()

        int exceptionCount()

        const char * getErrorMessage(int i)

        const char * getErrorCode(int i)


cdef extern from "../XdmValue.h":
    cdef cppclass XdmValue:
        XdmValue() except +

        void addXdmItem(XdmItem *val)
        #void releaseXdmValue()

        XdmItem * getHead()

        XdmItem * itemAt(int)

        int size()

        const char * toString()

        void incrementRefCount()

        void decrementRefCount()

        int getRefCount()

        int getType()

cdef extern from "../XdmItem.h":
    cdef cppclass XdmItem(XdmValue):
        XdmItem() except +
        const char * getStringValue()
        bool isAtomic()

cdef extern from "../XdmNode.h":
    cdef cppclass XdmNode(XdmItem):
        bool isAtomic()

        int getNodeKind()

        const char * getNodeName()

        XdmValue * getTypedValue()

        const char* getBaseUri()


        XdmNode* getParent()

        const char* getAttributeValue(const char *str)

        int getAttributeCount()

        XdmNode** getAttributeNodes()

        XdmNode** getChildren()

        int getChildCount()


cdef extern from "../XdmAtomicValue.h":
    cdef cppclass XdmAtomicValue(XdmItem):
        XdmAtomicValue() except +

        const char * getPrimitiveTypeName()

        bool getBooleanValue()

        double getDoubleValue()

        long getLongValue()

