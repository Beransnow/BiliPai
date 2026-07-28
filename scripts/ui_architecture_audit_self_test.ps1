[CmdletBinding()]
param(
    [string]$RepoRoot
)

$ErrorActionPreference = "Stop"
if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
    $RepoRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
}
$audit = Join-Path $RepoRoot "scripts/ui_architecture_audit.ps1"
$probeRoot = Join-Path ([IO.Path]::GetTempPath()) ("bilipai-ui-audit-" + [guid]::NewGuid().ToString("N"))
$probe = Join-Path $probeRoot "IllegalStyleProbe.kt"

try {
    New-Item -ItemType Directory -Path $probeRoot | Out-Null
    @"
package com.android.purebilibili.feature.auditprobe

import androidx.compose.runtime.Composable
import com.android.purebilibili.core.theme.UiPreset

@Composable
fun IllegalStyleProbe(preset: UiPreset) = Unit
"@ | Set-Content -LiteralPath $probe -Encoding utf8

    $ErrorActionPreference = "Continue"
    $negativeOutput = & powershell -NoProfile -File $audit -RepoRoot $RepoRoot -SyntheticFeatureFile $probe 2>&1
    $negativeExitCode = $LASTEXITCODE
    $ErrorActionPreference = "Stop"
    if ($negativeExitCode -eq 0) {
        throw "Negative audit unexpectedly passed.`n$($negativeOutput -join [Environment]::NewLine)"
    }
    Write-Output "SELF_TEST_NEGATIVE_OK"

    $ErrorActionPreference = "Continue"
    $stageGateOutput = & powershell -NoProfile -File $audit `
        -RepoRoot $RepoRoot `
        -StyleFeatureMax 0 `
        -LocalFeatureMax 0 `
        -IosFeatureCallersMax 0 2>&1
    $stageGateExitCode = $LASTEXITCODE
    $ErrorActionPreference = "Stop"
    $stageGateText = $stageGateOutput -join [Environment]::NewLine
    if ($stageGateExitCode -eq 0 -or $stageGateText -notmatch "exceeds baseline maximum") {
        throw "Strict stage gate unexpectedly passed or did not report its limit.`n$($stageGateOutput -join [Environment]::NewLine)"
    }
    Write-Output "SELF_TEST_STAGE_GATE_OK"

    Remove-Item -LiteralPath $probe -Force
    $cleanOutput = & powershell -NoProfile -File $audit -RepoRoot $RepoRoot 2>&1
    $cleanOutput | Write-Output
    if ($LASTEXITCODE -ne 0 -or $cleanOutput -notcontains "AUDIT_OK") {
        throw "Clean audit did not recover to AUDIT_OK."
    }
    Write-Output "SELF_TEST_CLEANUP_OK"
}
finally {
    if (Test-Path -LiteralPath $probeRoot) {
        Remove-Item -LiteralPath $probeRoot -Recurse -Force
    }
}
