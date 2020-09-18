set jdkdir=C:\Program Files\Saxonica\SaxonHEC1.2.1\Saxon.C.API\jni

cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DHEC Transform.c
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DHEC Query.c