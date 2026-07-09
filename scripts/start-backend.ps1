$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$backendDir = Join-Path $repoRoot 'StudyPlatform-back'
$localConfig = Join-Path $backendDir 'application-local.properties'
$localConfigExample = Join-Path $backendDir 'application-local.example.properties'

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  Write-Host "缺少 JDK 21：请先安装 Java，并确保 java 在 PATH 中。" -ForegroundColor Red
  exit 1
}

if (-not (Test-Path $localConfig)) {
  Copy-Item $localConfigExample $localConfig
  Write-Host "已创建 application-local.properties，请先填写 MySQL 密码和 AI Key 后再启动。" -ForegroundColor Yellow
  exit 1
}

Push-Location $backendDir
Write-Host "启动后端：http://localhost:8080" -ForegroundColor Green
.\mvnw.cmd spring-boot:run
Pop-Location
