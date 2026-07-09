$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$frontendDir = Join-Path $repoRoot 'studyplatform-vue'

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  Write-Host "缺少 Node.js：请先安装 Node.js 22.18+ 或 24.12+。" -ForegroundColor Red
  exit 1
}

Push-Location $frontendDir
if (-not (Test-Path 'node_modules')) {
  Write-Host "未发现 node_modules，正在根据 package-lock.json 安装依赖..." -ForegroundColor Yellow
  npm ci
}

Write-Host "启动前端：http://localhost:5173" -ForegroundColor Green
npm run dev
Pop-Location
