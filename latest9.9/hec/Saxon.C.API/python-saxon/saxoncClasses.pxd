from libcpp cimport bool


cdef extern from "../SaxonProcessor.h":
    cdef cppclass SaxonProcessor:
        SaxonProcessor(bool) except +
        bool license
        char * version()
        void release()

        # set the current working directory
        void setcwd(char* cwd)
        const char* getcwd()

        #SaxonProcessor * getProcessor();

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
        const char* getcwd()

        #Set the source document from a XdmValue for the transformation.
        #void setSourceFromXdmValue(XdmItem * value)

        #Set the source from file for the transformation.
        void setSourceFromFile(const char * filename)

        #Set the output file of where the transformation result is sent
        void setOutputFile(const char* outfile)

        void setJustInTimeCompilation(bool jit)

        void setParameter(const char* name, XdmValue*value)

        XdmValue* getParameter(const char* name)

        removeParameter(const char* name)

        void setProperty(const char* name, const char* value)

        void clearParameters(bool deleteValues=false)

        void clearProperties()

        XdmValue * getXslMessages()

        void transformFileToFile(const char* sourcefile, const char* stylesheetfile, const char* outputfile)
        
        const char * transformFileToString(const char* sourcefile, const char* stylesheetfile)

        XdmValue * transformFileToValue(const char* sourcefile, const char* stylesheetfile)

        void compileFromFile(const char* stylesheet)

        void compileFromString(const char* stylesheet)

        void compileFromStringAndSave(const char* stylesheet, const char* filename)

        void compileFromFileAndSave(const char* xslFilename, const char* filename)

        void compileFromXdmNode(XdmNode * node)

        void releaseStylesheet()

        const char * transformToString()

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

    cdef cppclass XPathProcessor:
        XPathProcessor() except +

    cdef cppclass XQueryProcessor:
        XQueryProcessor() except +


cdef extern from "../XdmValue.h":
    cdef cppclass XdmValue:
        XdmValue() except +
        XdmValue * addXdmValueWithType(const char * tStr, const char * val)
        void addXdmItem(XdmItem *val)
        #void releaseXdmValue()

        XdmItem * getHead()

        XdmItem * itemAt(int)

        int size()

        const char * toString()

cdef extern from "../XdmItem.h":
    cdef cppclass XdmItem(XdmValue):
        XdmItem() except +

cdef extern from "../XdmNode.h":
    cdef cppclass XdmNode(XdmItem):
        getNodeName()

cdef extern from "../XdmAtomicValue.h":
    cdef cppclass XdmAtomicValue(XdmItem):
        XdmAtomicValue() except +

