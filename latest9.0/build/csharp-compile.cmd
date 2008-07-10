

rem ====================================================
rem         compile the API files
rem ====================================================

rem param 1: source code directory        e.g. MyJava\build\n\csource
rem param 2: .NET dll dir                 e.g. MyJava\build\n\dll
rem param 3: version                      e.g. 8.9.0.1

set Path=C:\Program Files\Microsoft.NET\SDK\v1.1\Bin\;C:\WINDOWS\Microsoft.NET\Framework\v1.1.4322\;C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\bin\;C:\Program Files\Microsoft Visual Studio .NET 2003\Common7\IDE\;%PATH%
set LIB=C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\lib\;"C:\Program Files\Microsoft.NET\SDK\v1.1\Lib\";%LIB%
set INCLUDE=C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\include\;"C:\Program Files\Microsoft.NET\SDK\v1.1\include\";%INCLUDE%

set APISOURCE=%1/api/Saxon.Api
set CMDSOURCE=%1/cmd
set SMPSOURCE=%1/samples
set DLLDIR=%2
set VER=%3

cd %APISOURCE%

csc /target:module /out:%DLLDIR%/saxon9api.netmodule /doc:../apidoc.xml /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll *.cs
al /keyfile:c:\MyDotNet\make\saxondotnet.snk /comp:Saxonica /prod:Saxon /v:%VER% %DLLDIR%/saxon9api.netmodule /out:%DLLDIR%/saxon9api.dll

rem =====================================================
rem - install saxon9api assembly in the Global Assembly Cache
rem =====================================================

cd %DLLDIR%
set NET="c:\Program Files\Microsoft.NET\SDK\v1.1\Bin"
runas /user:adminstrator %NET%\gacutil /if saxon9api.dll 


rem ====================================================
rem         compile the command files
rem ====================================================


cd %CMDSOURCE%

csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Transform.exe ^
    Transform.cs
csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Query.exe ^
    Query.cs
csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Validate.exe ^
    Validate.cs


rem =====================================================
rem         compile the issued sample applications
rem =====================================================


cd %SMPSOURCE%

csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
		 /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
		 /out:%DLLDIR%/samples/XQueryExamples.exe ^
		 XQueryExamples.cs
csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/samples/XPathExample.exe ^
     XPathExample.cs
csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/samples/XsltExamples.exe ^
     XsltExamples.cs
csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/samples/SchemaExamples.exe ^
     SchemaExamples.cs
csc  /target:library /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9.dll; ^
     /out:%DLLDIR%/samples/SampleExtensions.dll ^
    SampleExtensions.cs 
     
rem =====================================================
rem   compile Sample Extensions with a strong name and install in the GAC
rem =====================================================

rem csc  /target:module /debug+ ^
rem      /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9.dll; ^
rem      /out:%DLLDIR%/samples/SampleExtensions.netmodule ^
rem      SampleExtensions.cs 

rem al /keyfile:c:\MyDotNet\make\saxondotnet.snk /comp:Saxonica /prod:Saxon /v:%VER% ^
rem      %DLLDIR%/samples/SampleExtensions.netmodule /out:%DLLDIR%/samples/SampleExtensions.dll
rem cd %DLLDIR%/samples
rem %NET%\gacutil /if SampleExtensions.dll 
rem %NET%\gacutil /l         

rem =====================================================
rem           compile test drivers
rem =====================================================

cd %SMPSOURCE%

csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/tests/XsltTestSuiteDriver.exe ^
     XsltTestSuiteDriver.cs
csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.GNU.Classpath.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/tests/XQueryTestSuiteDriver.exe ^
     XQueryTestSuiteDriver.cs


