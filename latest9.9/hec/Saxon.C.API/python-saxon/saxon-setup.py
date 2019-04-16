from distutils.core import setup
from distutils.extension import Extension
from Cython.Distutils import build_ext
from Cython.Build import cythonize

setup(ext_modules=cythonize([Extension("saxonc", ["saxonc.pyx", "../SaxonProcessor.cpp", "../SaxonCGlue.c", "../SaxonCXPath.c", "../XdmValue.cpp", "../XdmItem.cpp", "../XdmNode.cpp", "../XdmAtomicValue.cpp", "../XsltProcessor.cpp", "../XQueryProcessor.cpp","../XPathProcessor.cpp","../SchemaValidator.cpp"], language="c++",)],include_dirs = ['/home/ond1/work/svn/latest9.9-saxonc/Saxon.C.API/jni', "/home/ond1/work/svn/latest9.9-saxonc/Saxon.C.API/jni/unix"],
cmdclass = {'build_ext': build_ext}))
