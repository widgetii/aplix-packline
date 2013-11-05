cls
@echo off

set PROGRAMNAME=PackLine
set SCRIPTDIR=%~dp0
set SHELLAPP=%SCRIPTDIR%packline.bat

@rem --------------------------------------------
@rem  display confirmation
@rem --------------------------------------------

echo In order to register "%PROGRAMNAME%" as system shell, type 'r';
echo In order to restore standard shell, type 'u';
echo Type any other command for quit.
echo.

set /P CONFIRM=Please, enter a command (r/u/any other key)? 
cls

if "%CONFIRM%"=="r" (
	call :REGISTER_SHELL
	goto :WAITREACTION
)
if "%CONFIRM%"=="R" (
	call :REGISTER_SHELL
	goto :WAITREACTION
)
if "%CONFIRM%"=="u" (
	call :UNREGISTER_SHELL
	goto :WAITREACTION
)
if "%CONFIRM%"=="U" (
	call :UNREGISTER_SHELL
	goto :WAITREACTION
)
goto :QUIT

@rem --------------------------------------------
@rem  subroutine: register shell
@rem --------------------------------------------

:REGISTER_SHELL
reg add "HKCU\Software\Microsoft\Windows NT\CurrentVersion\Winlogon" /v "Shell" /t REG_SZ /d "%SHELLAPP%" /f

if ERRORLEVEL 1 (
	echo ERROR: Failed to register "%PROGRAMNAME%".
	goto QUIT
)

echo "%PROGRAMNAME%" was registered successfully as system shell.
EXIT /B

@rem --------------------------------------------
@rem  subroutine: unregister shell
@rem --------------------------------------------

:UNREGISTER_SHELL
reg add "HKCU\Software\Microsoft\Windows NT\CurrentVersion\Winlogon" /v "Shell" /t REG_SZ /d "-" /f

if ERRORLEVEL 1 (
	echo ERROR: Failed to unregister "%PROGRAMNAME%".
	goto QUIT
)

echo "%PROGRAMNAME%" was unregistered. The standard system shell has been restored.
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