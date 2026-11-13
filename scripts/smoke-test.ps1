# Quick smoke check (Windows PowerShell 5.1 / PowerShell 7): build all modules without tests,
# then run the Stage 1 core demo.
# Usage (project root): powershell -ExecutionPolicy Bypass -File scripts\smoke-test.ps1
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Host "Maven build failed" -ForegroundColor Red; exit 1 }
java -cp core/target/classes com.scasp.app.Main
if ($LASTEXITCODE -ne 0) { exit 1 }
Write-Host ""
Write-Host 'Smoke check completed: all modules compiled and the core demo ran. Run "mvn test" for the JUnit suites.'
