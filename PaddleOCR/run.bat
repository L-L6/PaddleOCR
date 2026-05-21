@echo off
title PaddleOCR
echo startpaddleOCR
cd /d%~dp0
.\jre\bin\java -jar app\try_paddle-0.0.1-SNAPSHOT.jar
echo.
echo exit
pause