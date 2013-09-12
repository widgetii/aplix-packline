@ECHO OFF

@rem --------------------------------------------
@rem  set parameters
@rem --------------------------------------------

SETLOCAL ENABLEDELAYEDEXPANSION

set SCRIPTDIR=%~dp0
pushd %SCRIPTDIR%..
set "RESOLVED_APP_HOME=%CD%"
popd

set JARFILE=%RESOLVED_APP_HOME%\bin\packline.jar
set LIBDIR=%RESOLVED_APP_HOME%\lib
set APP_CONFIG=%RESOLVED_APP_HOME%\conf\packline.xconf

for %%l in (%JARFILE% %LIBDIR%\*.jar) do set LOCALCLASSPATH=%%l;!LOCALCLASSPATH!

set JAVA_OPTS=%JAVA_OPTS% -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dprism.verbose=true

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
