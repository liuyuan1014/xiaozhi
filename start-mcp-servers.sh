#!/bin/bash

# AcademicHub MCP服务器启动脚本

echo "🚀 启动 AcademicHub MCP 服务器..."
echo ""

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "❌ 错误: 未检测到 Node.js"
    echo "请先安装 Node.js: https://nodejs.org/"
    exit 1
fi

echo "✅ Node.js 版本: $(node --version)"
echo ""

# 启动arXiv MCP服务器
echo "📚 启动 arXiv MCP 服务器 (端口 8002)..."
npx -y @modelcontextprotocol/server-arxiv --port 8002 &
ARXIV_PID=$!

# 等待服务启动
sleep 3

# 检查服务是否启动成功
if ps -p $ARXIV_PID > /dev/null; then
    echo "✅ arXiv MCP 服务器启动成功 (PID: $ARXIV_PID)"
else
    echo "❌ arXiv MCP 服务器启动失败"
    exit 1
fi

echo ""
echo "🎉 所有 MCP 服务器已启动！"
echo ""
echo "📋 服务信息:"
echo "  - arXiv MCP: http://localhost:8002"
echo ""
echo "💡 提示:"
echo "  - 按 Ctrl+C 停止所有服务"
echo "  - 查看日志: tail -f mcp-arxiv.log"
echo ""

# 保持脚本运行
wait $ARXIV_PID
