set jdkdir=C:\Program Files\Saxonica\SaxonEEC1.1.3\Saxon.C.API\jni

cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32"  testXPath.cpp ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCXPath.c ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XdmValue.cpp ../../Saxon.C.API/XdmItem.cpp ../../Saxon.C.API/XdmAtomicValue.cpp ../../Saxon.C.API/XdmNode.cpp ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XQueryProcessor.cpp ../../Saxon.C.API/XSLTProcessor.cpp ../../Saxon.C.API/XPathProcessor.cpp ../../Saxon.C.API/SchemaValidator.cpp
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32"  testXSLT.cpp ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCXPath.c ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XdmValue.cpp ../../Saxon.C.API/XdmItem.cpp ../../Saxon.C.API/XdmAtomicValue.cpp ../../Saxon.C.API/XdmNode.cpp ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XQueryProcessor.cpp ../../Saxon.C.API/XSLTProcessor.cpp ../../Saxon.C.API/XPathProcessor.cpp ../../Saxon.C.API/SchemaValidator.cpp
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32"  testXQuery.cpp ../../Saxon.C.API/SaxonCGlue.c ../../Saxon.C.API/SaxonCXPath.c ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XdmValue.cpp ../../Saxon.C.API/XdmItem.cpp ../../Saxon.C.API/XdmAtomicValue.cpp ../../Saxon.C.API/XdmNode.cpp ../../Saxon.C.API/SaxonProcessor.cpp ../../Saxon.C.API/XQueryProcessor.cpp ../../Saxon.C.API/XSLTProcessor.cpp ../../Saxon.C.API/XPathProcessor.cpp ../../Saxon.C.API/SchemaValidator.cpp

