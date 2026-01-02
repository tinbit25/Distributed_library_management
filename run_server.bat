@echo off
cd server
echo ====================================================
echo      DISTRIBUTED LIBRARY SYSTEM SERVER
echo ====================================================
echo.
echo Starting Server on PORT 8081...
echo.
echo [INFO] Local Access:      http://localhost:8081/
echo [INFO] Network Access:    http://[YOUR_IP]:8081/
echo.
echo DO NOT run 'http-server'. This Java server does everything!
echo.
echo [INFO] Compiling Source Code...
if not exist "bin" mkdir bin
javac -d bin -sourcepath src -cp "lib/*" src/main/Main.java
if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed! Please check your Java code.
    pause
    exit /b %errorlevel%
)
echo [INFO] Compilation SUCCESS.
echo.
java -cp "lib/mysql-connector-j-9.5.0.jar;bin" main.Main
pause
