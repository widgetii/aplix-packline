@ECHO OFF

CALL mvn -Pinstaller clean package
IF ERRORLEVEL 1 GOTO END

CALL mvn -Plauncher clean package
IF ERRORLEVEL 1 GOTO END

:END
ECHO.
IF NOT "%1"=="skipWait" PAUSE
