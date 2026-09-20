@echo off
setlocal
cd /d "%~dp0"

py -3 --version >nul 2>&1
if not errorlevel 1 goto usar_py

python --version >nul 2>&1
if not errorlevel 1 goto usar_python

echo Nao foi possivel encontrar o Python 3 nesta maquina.
echo Instale o Python 3 ou ajuste o PATH e tente novamente.
pause
exit /b 1

:usar_py
py -3 -B -u "%~dp0administrador\central.py" %*
goto terminou

:usar_python
python -B -u "%~dp0administrador\central.py" %*

:terminou
set "CODIGO=%ERRORLEVEL%"
if "%CODIGO%"=="0" exit /b 0
echo.
echo A Central de Baralhos terminou com erro %CODIGO%.
pause
exit /b %CODIGO%
