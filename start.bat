@echo off
echo 启动跳蚤市场容器...
cd /d %~dp0
docker-compose up -d
echo.
echo 等待服务启动...
timeout /t 5 /nobreak >nul
echo.
echo 服务状态:
docker ps --format "table {{.Names}}\t{{.Status}}"
echo.
echo 访问地址:
echo   前端: http://localhost
echo   后端: http://localhost:8080
echo   MinIO: http://localhost:9000
pause
