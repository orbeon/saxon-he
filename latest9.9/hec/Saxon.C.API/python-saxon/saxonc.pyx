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
        self.thisprt.isSchemaAware()
    def newXsltProcessor(self):
        cdef PyXsltProcessor val = PyXsltProcessor()
        val.thisxptr = self.thisptr.newXsltProcessor()
        return val

cdef class PyXsltProcessor:
     cdef saxoncClasses.XsltProcessor *thisxptr      # hold a C++ instance which we're wrapping

     def __cinit__(self):
        self.thisxptr = NULL
     def __dealloc__(self):
        if self.thisxptr != NULL:
           del self.thisxptr
         
    
