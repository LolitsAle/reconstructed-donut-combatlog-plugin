$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mavenVersion = '3.9.14'
$toolsDir = Join-Path $root '.tools'
$mavenDir = Join-Path $toolsDir ("apache-maven-" + $mavenVersion)
$zipPath = Join-Path $toolsDir ("apache-maven-" + $mavenVersion + "-bin.zip")
$mavenCmd = Join-Path $mavenDir 'bin\mvn.cmd'

if (!(Test-Path $mavenCmd)) {
    New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
    $url = "https://archive.apache.org/dist/maven/maven-3/$mavenVersion/binaries/apache-maven-$mavenVersion-bin.zip"
    Write-Host "Downloading Maven $mavenVersion..."
    Invoke-WebRequest -Uri $url -OutFile $zipPath
    Expand-Archive -Path $zipPath -DestinationPath $toolsDir -Force
}

& $mavenCmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Built jar: target/DonutCombatLog-3.1.2-custom.jar"
