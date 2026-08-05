<#
.SYNOPSIS
  One-shot template bootstrap: renames the com.picoding.fish package (and everything
  derived from it - rootProject.name, group, Dockerfile jar name, docker-compose
  container/volume names, the logs/*.log path, and the project directory itself) to
  your own project's identity, and optionally creates the PostgreSQL database
  referenced by DB_URL.

.EXAMPLE
  .\actions\init-project.ps1 -NewPackage com.acme.orders

.EXAMPLE
  .\actions\init-project.ps1 -NewPackage com.acme.orderservice -NewName orders

.EXAMPLE
  .\actions\init-project.ps1 -NewPackage com.acme.orders -CreateDb

.EXAMPLE
  .\actions\init-project.ps1 -CreateDb   # only create the database, skip renaming

.NOTES
  Run the rename part once, right after cloning the template, before you've made
  changes you care about - review the diff with `git status`/`git diff` afterwards.
  Delete this script (and its .sh sibling) once you're happy with the result.
#>
param(
    [Parameter(Mandatory = $false)]
    [string]$NewPackage,

    [Parameter(Mandatory = $false)]
    [string]$NewName,

    [Parameter(Mandatory = $false)]
    [switch]$CreateDb
)

$ErrorActionPreference = "Stop"

$OldPackage = "com.picoding.fish"
$OldGroup = "com.picoding"
$OldName = "fish"

if (-not $NewPackage -and -not $CreateDb) {
    Write-Error "Usage: .\actions\init-project.ps1 [-CreateDb] [-NewPackage <new.package.name>] [-NewName <project-name>]"
}

$RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $RepoRoot

function New-Database {
    # DB_URL/DB_USER/DB_PASSWORD come from the environment, falling back to .env.
    $dbUrl = $env:DB_URL
    $dbUser = $env:DB_USER
    $dbPassword = $env:DB_PASSWORD

    $envFile = Join-Path $RepoRoot ".env"
    if (-not $dbUrl -and (Test-Path $envFile)) {
        foreach ($line in Get-Content $envFile) {
            if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
                $key = $Matches[1]
                $value = $Matches[2].Trim().Trim('"')
                switch ($key) {
                    "DB_URL" { if (-not $dbUrl) { $dbUrl = $value } }
                    "DB_USER" { if (-not $dbUser) { $dbUser = $value } }
                    "DB_PASSWORD" { if (-not $dbPassword) { $dbPassword = $value } }
                }
            }
        }
    }

    if (-not $dbUrl) {
        Write-Error "DB_URL is not set (checked the environment and .env) - cannot create database."
    }

    $psql = Get-Command psql -ErrorAction SilentlyContinue
    if (-not $psql) {
        Write-Error "psql not found on PATH - install the PostgreSQL client to use -CreateDb."
    }

    # DB_URL is a JDBC url: jdbc:postgresql://host[:port]/dbname[?params]
    if ($dbUrl -notmatch '^jdbc:postgresql://([^/:]+)(:(\d+))?/([A-Za-z0-9_]+)') {
        Write-Error "Could not parse DB_URL '$dbUrl' as jdbc:postgresql://host[:port]/dbname"
    }
    $dbHost = $Matches[1]
    $dbPort = if ($Matches[3]) { $Matches[3] } else { "5432" }
    $dbName = $Matches[4]
    if (-not $dbUser) { $dbUser = "postgres" }

    Write-Host "Checking database '$dbName' on ${dbHost}:${dbPort}..."
    $env:PGPASSWORD = $dbPassword

    $exists = & psql -h $dbHost -p $dbPort -U $dbUser -d postgres -tAc `
        "SELECT 1 FROM pg_database WHERE datname = '$dbName'"

    if (($exists | Out-String).Trim() -eq "1") {
        Write-Host "Database '$dbName' already exists - nothing to do."
    } else {
        Write-Host "Creating database '$dbName'..."
        & psql -h $dbHost -p $dbPort -U $dbUser -d postgres -c "CREATE DATABASE `"$dbName`""
        Write-Host "Database '$dbName' created."
    }
}

if (-not $NewPackage) {
    New-Database
    return
}

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

# Rename the project directory itself to match. Best-effort: this can transiently
# fail right after heavy file churn (antivirus/indexer briefly holding a file open),
# or fail outright if a shell/IDE has the directory open as its current directory -
# so it's retried a few times, and a lasting failure is reported without aborting
# the script.
$ParentDir = Split-Path $RepoRoot -Parent
$CurrentDirName = Split-Path $RepoRoot -Leaf
if ($CurrentDirName -ne $NewName) {
    $NewRepoRoot = Join-Path $ParentDir $NewName
    if (Test-Path $NewRepoRoot) {
        Write-Host "Warning: '$NewRepoRoot' already exists - leaving the project directory name as-is."
    } else {
        Set-Location $ParentDir
        $moved = $false
        for ($attempt = 1; $attempt -le 3; $attempt++) {
            try {
                Rename-Item -Path $RepoRoot -NewName $NewName -ErrorAction Stop
                $moved = $true
                break
            } catch {
                Start-Sleep -Seconds 1
            }
        }
        if ($moved) {
            $RepoRoot = $NewRepoRoot
            Set-Location $RepoRoot
            Write-Host "Renamed project directory: $CurrentDirName -> $NewName"
        } else {
            Set-Location (Join-Path $ParentDir $CurrentDirName)
            Write-Host "Warning: couldn't rename the project directory to '$NewName' - it's likely still open in this shell/IDE. Close whatever has it open, then run:"
            Write-Host "    cd ..; Rename-Item $CurrentDirName $NewName"
        }
    }
}

Write-Host "Done."
Write-Host ""

if ($CreateDb) {
    New-Database
    Write-Host ""
}

Write-Host "Next steps:"
Write-Host "  - skim README.md - the prose (title, description) still talks about the old project"
Write-Host "  - update .env / .env.example: admin credentials, JWT secret, DB name"
Write-Host "  - git status / git diff to review everything that changed"
Write-Host "  - delete actions\init-project.sh and actions\init-project.ps1 once you're happy"
