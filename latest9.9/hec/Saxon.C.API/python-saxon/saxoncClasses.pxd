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

