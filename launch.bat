@echo off
setlocal

where java >nul 2>&1
if errorlevel 1 (
    echo [ОШИБКА] Java не найдена. Установите Java 21+ с https://adoptium.net
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VER=%%v
)

echo Запуск VoidRP Launcher...
java ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.nio=ALL-UNNAMED ^
  -Xmx256m ^
  -jar "%~dp0build\libs\voidrp-launcher-1.0.0.jar"

if errorlevel 1 (
    echo.
    echo [ОШИБКА] Лаунчер завершился с ошибкой.
    echo Убедитесь что установлена Java 21+: java -version
    pause
)
