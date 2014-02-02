@ECHO OFF
set SCRIPTDIR=%~dp0

@rem --------------------------------------------
@rem  build libs and apps
@rem --------------------------------------------

CALL mvn -Plibs clean install
IF ERRORLEVEL 1 GOTO END

CALL mvn -Papps clean package
IF ERRORLEVEL 1 GOTO END

@rem --------------------------------------------
@rem  build installer and launcher
@rem --------------------------------------------

pushd %SCRIPTDIR%
cd "%SCRIPTDIR%/jws"

CALL build.bat skipWait
IF ERRORLEVEL 1 GOTO END

popd

@rem --------------------------------------------------
@rem  move all parts of distribution pack to one place
@rem --------------------------------------------------

echo Moving all parts of distribution pack to one place
echo.

rd /S /Q "%SCRIPTDIR%\jws\target\dist\"
md "%SCRIPTDIR%\jws\target\dist\"

move /Y "%SCRIPTDIR%\app\target\dist-pack\*.*" "%SCRIPTDIR%\jws\target\dist\"
move /Y "%SCRIPTDIR%\jws\target\installer\dist\*.*" "%SCRIPTDIR%\jws\target\dist\"
move /Y "%SCRIPTDIR%\jws\target\launcher\dist\*.*" "%SCRIPTDIR%\jws\target\dist\"

echo.
echo Done

:END
ECHO.
PAUSE