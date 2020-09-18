set jdkdir=C:\Program Files\Saxonica\SaxonPEC1.2.1\Saxon.C.API\jni

cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32"  /DPEC Transform.c
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DPEC Query.c