@echo off
setlocal
for /f "tokens=2 delims=:" %%c in ('chcp') do set "OLDCP=%%c"
chcp 65001 >nul
rem Windows cmd·PowerShell에서 setup.sh를 Git Bash로 실행한다. 설치 내용은 모두 setup.sh에 있다.
rem cmd·PowerShell에서 bash를 치면 Git Bash 대신 WSL이 잡히므로 이 파일을 둔다.
rem 프론트 저장소의 setup.bat와 내용이 같다. 고칠 때 둘 다 고친다.

rem git.exe 위치(Git\cmd 또는 Git\mingw64\bin)에서 Git\bin\bash.exe를 찾는다
set "GITBASH="
for /f "delims=" %%g in ('where git 2^>nul') do (
  if not defined GITBASH if exist "%%~dpg..\bin\bash.exe" set "GITBASH=%%~dpg..\bin\bash.exe"
  if not defined GITBASH if exist "%%~dpg..\..\bin\bash.exe" set "GITBASH=%%~dpg..\..\bin\bash.exe"
)
if not defined GITBASH (
  echo.
  echo [중단] Git for Windows가 없습니다. https://git-scm.com 에서 설치한 뒤 다시 실행해 주세요.
  set "RC=1"
  goto end
)

"%GITBASH%" "%~dp0setup.sh" %*
set "RC=%ERRORLEVEL%"

:end
rem 탐색기에서 더블클릭하면 끝나자마자 창이 닫힌다. 실패했거나 직접 할 일이 남았을 때(종료 코드 0이 아님)만 멈춘다
if not "%RC%"=="0" echo %cmdcmdline% | findstr /i /c:"%~nx0" >nul && pause
chcp %OLDCP% >nul
exit /b %RC%
