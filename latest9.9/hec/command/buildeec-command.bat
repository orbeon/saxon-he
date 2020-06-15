set jdkdir=C:\Program Files\Saxonica\SaxonEEC1.2.1\Saxon.C.API\jni

cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DEEC Transform.c
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DEEC Query.c
cl /EHsc "-I%jdkdir%" "-I%jdkdir%\win32" /DEEC Validate.c