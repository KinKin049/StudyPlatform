$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$frontendDir = Join-Path $repoRoot 'studyplatform-vue'
$backendDir = Join-Path $repoRoot 'StudyPlatform-back'
$localConfig = Join-Path $backendDir 'application-local.properties'
$localConfigExample = Join-Path $backendDir 'application-local.example.properties'

function Write-Step($message) {
  Write-Host ""
  Write-Host "==> $message" -ForegroundColor Cyan
}

function Test-Command($name, $installHint) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    Write-Host "缺少命令：$name" -ForegroundColor Red
    Write-Host $installHint -ForegroundColor Yellow
    exit 1
  }
}

Write-Host "StudyPlatform 跨电脑初始化脚本" -ForegroundColor Green
Write-Host "项目目录：$repoRoot"

Write-Step "检查基础环境"
Test-Command node "请安装 Node.js 22.18+ 或 24.12+，然后重新打开终端。"
Test-Command npm "Node.js 安装后应自带 npm，请检查 PATH。"
Test-Command java "请安装 JDK 21，并确保 java 在 PATH 中。"

Write-Host "Node: $(node -v)"
Write-Host "npm : $(npm -v)"
Write-Host "Java:"
java -version

Write-Step "安装前端依赖"
Push-Location $frontendDir
if (Test-Path 'package-lock.json') {
  npm ci
} else {
  npm install
}
Pop-Location

Write-Step "准备后端本地配置"
if (-not (Test-Path $localConfig)) {
  Copy-Item $localConfigExample $localConfig
  Write-Host "已复制：StudyPlatform-back\application-local.properties" -ForegroundColor Green
  Write-Host "请按你的新电脑 MySQL 密码和 AI Key 修改这个文件。" -ForegroundColor Yellow
} else {
  Write-Host "已存在：StudyPlatform-back\application-local.properties，跳过复制。"
}

Write-Step "预下载并编译后端依赖"
Push-Location $backendDir
.\mvnw.cmd -q -DskipTests compile
Pop-Location

Write-Step "完成"
Write-Host "下一步：" -ForegroundColor Green
Write-Host "1. 确认 MySQL 已创建数据库 study_platform。"
Write-Host "2. 修改 StudyPlatform-back\application-local.properties。"
Write-Host "3. 运行 scripts\start-backend.ps1 和 scripts\start-frontend.ps1。"
