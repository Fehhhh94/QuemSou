@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
@rem O cliente do wrapper roda no java.exe resolvido por JAVA_HOME/PATH (pode
@rem ser um Java 8 antigo com charset padrão Cp1252) — estas flags garantem
@rem que ele reemita em UTF-8, independente da JVM, a saída de texto que
@rem recebe do daemon. A outra metade do problema (o codepage com que o
@rem console EXIBE esses bytes) é tratada em :execute.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m" "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" "-Dstderr.encoding=UTF-8"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=


@rem Execute Gradle
@rem Um console Windows recém-aberto decodifica stdout com o codepage OEM
@rem (437/850), exibindo os bytes UTF-8 do build como lixo ("Γ£ô v├ílido" no
@rem lugar de "✓ válido") — validado por captura do screen buffer com e sem
@rem chcp 65001. Coloca o console em UTF-8 (65001) só durante o build e
@rem restaura o codepage anterior ao final, preservando o exit code. O parse
@rem pega o número após o ":" da saída do chcp, em qualquer idioma do Windows.
set GRADLEW_CP_ANTERIOR=
for /f "tokens=2 delims=:" %%c in ('chcp.com 2^>nul') do set GRADLEW_CP_ANTERIOR=%%c
if defined GRADLEW_CP_ANTERIOR set GRADLEW_CP_ANTERIOR=%GRADLEW_CP_ANTERIOR: =%
if defined GRADLEW_CP_ANTERIOR set GRADLEW_CP_ANTERIOR=%GRADLEW_CP_ANTERIOR:.=%
if defined GRADLEW_CP_ANTERIOR chcp.com 65001 >nul

"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -classpath "%CLASSPATH%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %*

set GRADLEW_EXIT_CODE=%ERRORLEVEL%
if defined GRADLEW_CP_ANTERIOR chcp.com %GRADLEW_CP_ANTERIOR% >nul
cmd /c exit %GRADLEW_EXIT_CODE%

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%GRADLE_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
