@echo off
chcp 65001 >nul
echo 🚀 启动 AcademicHub MCP 服务器...
echo.

REM 检查Node.js是否安装
where node >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 错误: 未检测到 Node.js
    echo 请先安装 Node.js: https://nodejs.org/
    pause
    exit /b 1
)

for /f "tokens=*" %%i in ('node --version') do set NODE_VERSION=%%i
echo ✅ Node.js 版本: %NODE_VERSION%
echo.

REM 启动arXiv MCP服务器
echo 📚 启动 arXiv MCP 服务器 (端口 8002)...
start "arXiv MCP Server" cmd /k "npx -y @modelcontextprotocol/server-arxiv --port 8002"

REM 等待服务启动
timeout /t 3 /nobreak >nul

echo.
echo 🎉 所有 MCP 服务器已启动！
echo.
echo 📋 服务信息:
echo   - arXiv MCP: http://localhost:8002
echo.
echo 💡 提示:
echo   - 关闭窗口即可停止服务
echo   - 如需查看日志，请查看对应的命令行窗口
echo.
pause
