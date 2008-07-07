
rem === This batch file builds the saxon9.dll and saxon9sa.dll assemblies,
rem === leaving them in c:\MyDotNet\bin\release. They are also moved into the
rem === General Assembly Cache. The script takes off where the Ant build finishes,
rem === that is with the two JAR files in c:/MyJava/build/temp/n/jar

rem === This file is designed to be invoked from task ikvmc in the Ant build file build.xml

rem === Set argument 1 to "debug" to build for development/debugging, or to "live" for live running
rem === Set argument 2 to the directory containing the JAR files e.g. MyJava\build\n\jar
rem === Set argument 3 to the .NET DLL directory e.g. MyJava\build\n\dll
rem === Set argument 4 to the Saxon version e.g. 8.9.0.1

set SAXON_NET_VERSION=%4
set IKVMVER=ikvm-0.36.0.11
set IKVM=c:\MyDotNet\%IKVMVER%\bin
set SRC=c:\MyJava\build\temp\n\jsource

set SAXON_BUILD_BN=c:\saxon-build\%SAXON_NET_VERSION%\bn
set SAXON_BUILD_SAN=c:\saxon-build\%SAXON_NET_VERSION%\san

set SAXON_JAR_DIR=%2


rem === Variables identifying release version ===

set SAXON_RELEASE_DIR=%3

echo Building saxon9 %SAXON_NET_VERSION% DLL 

rem use the two lines below when building for release...

if %1==debug goto c1debug

%IKVM%\ikvmc -assembly:saxon9 -target:library -keyfile:saxondotnet.snk -version:%SAXON_NET_VERSION% ^
             -debug -srcpath:c:\MyJava\saxon8.x %SAXON_JAR_DIR%\saxon9.jar ^
             -reference:mscorlib.dll -reference:System.Xml -reference:System ^
             -out:%SAXON_RELEASE_DIR%\saxon9.dll
             
%IKVM%\ikvmc -assembly:saxon9sa -target:library -keyfile:saxondotnet.snk -version:%SAXON_NET_VERSION% ^
             -reference:%SAXON_RELEASE_DIR%\saxon9.dll ^
             -reference:mscorlib.dll -reference:System.Xml -reference:System ^
             -reference:System.Security ^
             -debug -srcpath:c:\MyJava\saxon8.x %SAXON_JAR_DIR%\saxon9sa.jar ^
             -out:%SAXON_RELEASE_DIR%\saxon9sa.dll

rem Need to install the dlls in the Global Assembly Cache now for the rest of the script to work

cd %SAXON_RELEASE_DIR%
set NET="c:\Program Files\Microsoft.NET\SDK\v2.0\Bin"
runas /user:adminstrator %NET%\gacutil /if saxon9.dll 
runas /user:adminstrator %NET%\gacutil /if saxon9sa.dll 

goto c1resume

:c1debug
rem use the two lines below when building debug version...

%IKVM%\ikvmc -assembly:saxon9 -target:library  ^
             -reference:mscorlib.dll -reference:System.Xml -reference:System ^
			 -debug -srcpath:c:\MyJava\saxon8.x %SAXON_JAR_DIR%\saxon9.jar ^
			 -out:%SAXON_RELEASE_DIR%\saxon9.dll
						 
%IKVM%\ikvmc -assembly:saxon9sa -target:library ^
             -reference:%SAXON_RELEASE_DIR%\saxon9.dll ^
             -reference:mscorlib.dll -reference:System.Xml -reference:System ^
             -reference:System.Security ^
             -debug -srcpath:c:\MyJava\saxon8.x %SAXON_JAR_DIR%\saxon9sa.jar ^
             -out:%SAXON_RELEASE_DIR%\saxon9sa.dll

:c1resume
