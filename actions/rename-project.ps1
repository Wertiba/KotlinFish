<#
.SYNOPSIS
  One-shot template bootstrap: renames the com.picoding.fish package (and everything
  derived from it - rootProject.name, group, Dockerfile jar name, docker-compose
  container/volume names, the logs/*.log path) to your own project's identity.

.EXAMPLE
  .\actions\rename-project.ps1 -NewPackage com.acme.orders

.EXAMPLE
  .\actions\rename-project.ps1 -NewPackage com.acme.orderservice -NewName orders

.NOTES
  Run this once, right after cloning the template, before you've made changes you
  care about - review the diff with `git status`/`git diff` afterwards. Delete this
  script (and its .sh sibling) once you're happy with the result.
#>
param(
    [Parameter(Mandatory = $true)]
    [string]$NewPackage,

    [Parameter(Mandatory = $false)]
    [string]$NewName
)

$ErrorActionPreference = "Stop"

$OldPackage = "com.picoding.fish"
$OldGroup = "com.picoding"
$OldName = "fish"

if (-not $NewName) {
    $NewName = $NewPackage.Substring($NewPackage.LastIndexOf('.') + 1)
}
$lastDot = $NewPackage.LastIndexOf('.')
if ($lastDot -lt 0) {
    Write-Error "New package must have at least two segments, e.g. com.acme.orders"
}
$NewGroup = $NewPackage.Substring(0, $lastDot)

if ($NewPackage -notmatch '^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)+$') {
    Write-Error "Package must be lowercase, dot-separated identifiers, e.g. com.acme.orders"
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

$OldPath = $OldPackage -replace '\.', '\'
$NewPath = $NewPackage -replace '\.', '\'

$MainOld = Join-Path "src\main\kotlin" $OldPath
$TestOld = Join-Path "src\test\kotlin" $OldPath

if (-not (Test-Path $MainOld)) {
    Write-Error "Expected package directory '$MainOld' not found - has this already been renamed?"
}

$inGit = $false
try {
    $inGit = ((git rev-parse --is-inside-work-tree 2>$null) -eq "true")
} catch {}

Write-Host "Renaming package: $OldPackage -> $NewPackage"
Write-Host "Renaming project: name '$OldName' -> '$NewName', group '$OldGroup' -> '$NewGroup'"
Write-Host ""

function Move-Tree($oldDir, $newDir) {
    if (-not (Test-Path $oldDir)) { return }
    $parent = Split-Path $newDir
    if ($parent -and -not (Test-Path $parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    if ($inGit) {
        git mv $oldDir $newDir
    } else {
        Move-Item $oldDir $newDir
    }
}

Move-Tree $MainOld (Join-Path "src\main\kotlin" $NewPath)
Move-Tree $TestOld (Join-Path "src\test\kotlin" $NewPath)

# Clean up now-empty package directories left behind by the move (e.g. com\picoding).
foreach ($base in @("src\main\kotlin", "src\test\kotlin")) {
    if (-not (Test-Path $base)) { continue }
    $changed = $true
    while ($changed) {
        $changed = $false
        Get-ChildItem -Path $base -Recurse -Directory |
            Sort-Object { $_.FullName.Length } -Descending |
            ForEach-Object {
                if ((Get-ChildItem $_.FullName -Force | Measure-Object).Count -eq 0) {
                    Remove-Item $_.FullName -Force
                    $changed = $true
                }
            }
    }
}

function Replace-InFile($path, $pattern, $replacement) {
    if (-not (Test-Path $path)) { return }
    $content = Get-Content $path -Raw
    $updated = $content -replace $pattern, $replacement
    if ($updated -ne $content) {
        Set-Content -Path $path -Value $updated -NoNewline -Encoding utf8
    }
}

# Rewrite every source/config file that references the old package.
$escapedOldPackage = [regex]::Escape($OldPackage)
$candidates = Get-ChildItem -Path "src" -Recurse -File -Include "*.kt", "*.kts" -ErrorAction SilentlyContinue
$candidates += Get-Item "build.gradle.kts", "settings.gradle.kts", "Dockerfile", "README.md" -ErrorAction SilentlyContinue

foreach ($file in ($candidates | Sort-Object FullName -Unique)) {
    $content = Get-Content $file.FullName -Raw
    if ($content -match $escapedOldPackage) {
        Replace-InFile $file.FullName $escapedOldPackage $NewPackage
    }
}

Replace-InFile "settings.gradle.kts" "rootProject\.name = `"$OldName`"" "rootProject.name = `"$NewName`""
Replace-InFile "build.gradle.kts" "group = `"$OldGroup`"" "group = `"$NewGroup`""

$versionMatch = Select-String -Path "build.gradle.kts" -Pattern 'version = "([^"]+)"' | Select-Object -First 1
if ($versionMatch) {
    $version = $versionMatch.Matches[0].Groups[1].Value
    $escapedVersion = [regex]::Escape($version)
    Replace-InFile "Dockerfile" "$OldName-$escapedVersion\.jar" "$NewName-$version.jar"
}

Replace-InFile "src\main\resources\application.yaml" "logs/$OldName\.log" "logs/$NewName.log"
Replace-InFile "README.md" "logs/$OldName\.log" "logs/$NewName.log"

if (Test-Path "docker-compose.yml") {
    Replace-InFile "docker-compose.yml" "container_name: kotlinapp" "container_name: $NewName"
    Replace-InFile "docker-compose.yml" "container_name: db" "container_name: $NewName-db"
    Replace-InFile "docker-compose.yml" "pgdata:" "${NewName}_pgdata:"
    Replace-InFile "docker-compose.yml" "applogs:" "${NewName}_applogs:"
}

Write-Host "Done."
Write-Host ""
Write-Host "Next steps:"
Write-Host "  - skim README.md - the prose (title, description) still talks about the old project"
Write-Host "  - update .env / .env.example: admin credentials, JWT secret, DB name"
Write-Host "  - git status / git diff to review everything that changed"
Write-Host "  - delete actions\rename-project.sh and actions\rename-project.ps1 once you're happy"
