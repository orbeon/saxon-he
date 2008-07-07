

rem ====================================================
rem         compile the API files
rem ====================================================

rem param 1: source code directory        e.g. MyJava\build\n\csource
rem param 2: .NET dll dir                 e.g. MyJava\build\n\dll
rem param 3: version                      e.g. 8.9.0.1

set Path=C:\Program Files\Microsoft.NET\SDK\v2.0\Bin\;C:\WINDOWS\Microsoft.NET\Framework\v2.0.50727\;%PATH%
rem set LIB=C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\lib\;"C:\Program Files\Microsoft.NET\SDK\v1.1\Lib\";%LIB%
rem set INCLUDE=C:\Program Files\Microsoft Visual Studio .NET 2003\Vc7\include\;"C:\Program Files\Microsoft.NET\SDK\v1.1\include\";%INCLUDE%

set APISOURCE=%1/api/Saxon.Api
set CMDSOURCE=%1/cmd
set SMPSOURCE=%1/samples
set DLLDIR=%2
set VER=%3

cd %APISOURCE%

csc /target:module /out:%DLLDIR%/saxon9api.netmodule /doc:../apidoc.xml ^
    /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    *.cs
al /keyfile:c:\MyJava\build\saxondotnet.snk /comp:Saxonica /prod:Saxon /v:%VER% %DLLDIR%/saxon9api.netmodule ^
   /out:%DLLDIR%/saxon9api.dll

rem =====================================================
rem - install saxon9api assembly in the Global Assembly Cache
rem =====================================================

cd %DLLDIR%
set NET="c:\Program Files\Microsoft.NET\SDK\v2.0\Bin"
runas /user:adminstrator %NET%\gacutil /if saxon9api.dll 


rem ====================================================
rem         compile the command files
rem ====================================================


cd %CMDSOURCE%

csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Transform.exe ^
    Transform.cs
csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Query.exe ^
    Query.cs
csc /target:exe /win32icon:c:\MyDotNet\icons\gyfu.ico ^
    /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/IKVM.Runtime.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9sa.dll ^
    /out:%DLLDIR%/Validate.exe ^
    Validate.cs


rem =====================================================
rem         compile the issued sample applications
rem =====================================================


cd %SMPSOURCE%

csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
		 /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/saxon9api.dll ^
		 /out:%DLLDIR%/samples/Examples.exe ^
		 Examples.cs
csc  /target:library /debug+ ^
     /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/saxon9.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/samples/SampleExtensions.dll ^
    SampleExtensions.cs 
     

rem =====================================================
rem           compile test drivers
rem =====================================================

cd %SMPSOURCE%
   
csc  /target:exe /win32icon:c:\MyDotNet\icons\csharp.ico /debug+ ^
     /r:%DLLDIR%/IKVM.OpenJDK.ClassLibrary.dll;%DLLDIR%/saxon9api.dll ^
     /out:%DLLDIR%/tests/TestRunner.exe ^
     TestRunnerForm.cs TestRunnerForm.Designer.cs TestRunnerProgram.cs XsltTestSuiteDriver.cs XQueryTestSuiteDriver.cs ^
     SchemaTestSuiteDriver.cs IFeedbackListener.cs FileComparer.cs  
