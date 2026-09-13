# 运行脚本（Windows）
# 需要 JDK 11 或更高版本（JDK 17/21 均可）

$ErrorActionPreference = "Stop"

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host "未找到 javac，请先安装 JDK 11+ 并将其加入 PATH。" -ForegroundColor Red
    Write-Host "例如：winget install Microsoft.OpenJDK.21"
    exit 1
}

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root

if (-not (Test-Path "build")) { New-Item -ItemType Directory -Path "build" | Out-Null }

Write-Host "编译中..." -ForegroundColor Cyan
javac -encoding UTF-8 -d build SnakeGame.java

Write-Host "启动游戏..." -ForegroundColor Green
java -cp build SnakeGame
