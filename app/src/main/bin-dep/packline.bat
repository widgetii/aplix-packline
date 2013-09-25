@ECHO OFF

@rem --------------------------------------------
@rem  determine script path
@rem --------------------------------------------

SETLOCAL ENABLEDELAYEDEXPANSION

set SCRIPTDIR=%~dp0
pushd %SCRIPTDIR%..
set "RESOLVED_APP_HOME=%CD%"
popd

@rem --------------------------------------------
@rem  determine processor architecture
@rem --------------------------------------------
 
Set RegQry=HKLM\Hardware\Description\System\CentralProcessor\0
REG.exe Query %RegQry% > checkOS.arch
Find /i "x86" < checkOS.arch > checkOS.arch2
If %ERRORLEVEL% == 0 (
    set ARCH=win-32
) ELSE (
    set ARCH=win-64
)
del checkOS.arch
del checkOS.arch2

@rem --------------------------------------------
@rem  set parameters
@rem --------------------------------------------

set JARFILE=%RESOLVED_APP_HOME%\bin\packline.jar
set LIBDIR=%RESOLVED_APP_HOME%\lib
set APP_CONFIG=%RESOLVED_APP_HOME%\conf\packline.xconf
set RXTX_LIBS=%RESOLVED_APP_HOME%\lib\rxtx\%ARCH%

for %%l in ("%JARFILE%" "%LIBDIR%"\*.jar) do set LOCALCLASSPATH=%%l;!LOCALCLASSPATH!

set JAVA_OPTS=%JAVA_OPTS% -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dprism.verbose=true -Djava.library.path="%RXTX_LIBS%"

@rem --------------------------------------------
@rem  gather command line arguments
@rem --------------------------------------------

set CMD_LINE_ARGS=%1
if ""%1""=="""" goto doneStart
shift
:setupArgs
if ""%1""=="""" goto doneStart
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setupArgs
:doneStart

@rem --------------------------------------------
@rem  run program
@rem --------------------------------------------

call java %JAVA_OPTS% -cp "%LOCALCLASSPATH%" com.javafx.main.Main "--config=%APP_CONFIG%" %CMD_LINE_ARGS%
