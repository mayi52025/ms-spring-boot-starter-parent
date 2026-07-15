#!/usr/bin/env pwsh
# 提交前门禁：与 CI 一致，不依赖 IDEA 索引
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Write-Host "==> ms-console-ui: lint + typecheck + build"
Push-Location (Join-Path $root 'ms-console-ui')
npm run verify
Pop-Location

Write-Host "==> ms-spring-boot-autoconfigure: unit tests"
Push-Location $root
mvn test -pl ms-spring-boot-autoconfigure -q
Pop-Location

Write-Host "OK: pre-commit verify passed"
