@echo off
echo 停止跳蚤市场容器...
cd /d %~dp0
docker-compose down
echo.
echo 所有容器已停止
pause
