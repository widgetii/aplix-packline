@ECHO OFF

@rem --------------------------------------------
@rem  determine script path
@rem --------------------------------------------

SETLOCAL ENABLEDELAYEDEXPANSION

set SCRIPTDIR=%~dp0
pushd %SCRIPTDIR%..
set RESOLVED_APP_HOME=%CD%
popd

@rem --------------------------------------------
@rem  determine processor architecture
@rem --------------------------------------------

@rem SET ARCH=win-32
@rem SET ARCH=win-64

IF not DEFINED ARCH (
	CALL :DETERMINE_ARCH
	IF not DEFINED ARCH goto :WAITREACTION
)
echo Processor architecture is "%ARCH%"

@rem --------------------------------------------
@rem  set parameters
@rem --------------------------------------------

set JARFILE=%RESOLVED_APP_HOME%\bin
set LIBDIR=%RESOLVED_APP_HOME%\lib
set APP_CONFIG=%RESOLVED_APP_HOME%\conf\packline.xconf
set RXTX_LIBS=%RESOLVED_APP_HOME%\lib\rxtx\%ARCH%

for %%l in ("%JARFILE%\*.jar" "%LIBDIR%\*.jar") do set LOCALCLASSPATH=%%l;!LOCALCLASSPATH!

set JAVA_OPTS=%JAVA_OPTS% -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -Dprism.verbose=true "-Djava.library.path=%RXTX_LIBS%" -Xms1024m -Xmx1024m

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
goto QUIT

@rem --------------------------------------------
@rem  subroutine: determine processor architecture
@rem --------------------------------------------

:DETERMINE_ARCH
set RegQry=HKLM\Hardware\Description\System\CentralProcessor\0
REG.exe Query %RegQry% > checkOS.arch

if not exist checkOS.arch (
	echo ERROR: Can't determine processor architecture.
	EXIT /B
)

Find /i "x86" < checkOS.arch > checkOS.arch2
If %ERRORLEVEL% == 0 (
	set ARCH=win-32
) ELSE (
	set ARCH=win-64
)
del checkOS.arch
del checkOS.arch2
EXIT /B

@rem --------------------------------------------
@rem  wait user reaction
@rem --------------------------------------------

:WAITREACTION
echo.
PAUSE

@rem --------------------------------------------
@rem  quit program
@rem --------------------------------------------

:QUIT
