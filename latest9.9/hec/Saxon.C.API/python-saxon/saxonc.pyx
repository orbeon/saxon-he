from libcpp cimport bool

cdef extern from "../SaxonProcessor.h":
    cdef cppclass SaxonProcessor:
        SaxonProcessor(bool)
        bool license
        char * version()
        void release()
     
   
cdef class PySaxonProcessor:
    cdef SaxonProcessor *thisptr      # hold a C++ instance which we're wrapping
    def __cinit__(self, bool license):
        self.thisptr = new SaxonProcessor(license)
    def __dealloc__(self):
        del self.thisptr
    def version(self):
        return self.thisptr.version()
    def release(self):
        self.thisptr.release()
    
