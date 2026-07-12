param(
    [ValidateSet('build', 'release')]
    [string]$Command = 'build',
    [switch]$Clean,
    [string]$OutputDir = (Join-Path $PSScriptRoot 'dist')
)

$ErrorActionPreference = 'Stop'

function Resolve-Java8Home {
    if ($env:JAVA8_HOME) {
        return $env:JAVA8_HOME
    }

    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        $version = & (Join-Path $env:JAVA_HOME 'bin\javac.exe') -version 2>&1
        if ($version -match '^javac 1\.8\.') {
            return $env:JAVA_HOME
        }
    }

    $portable = Join-Path $PSScriptRoot '.toolchains\temurin8\jdk8u492-b09'
    if (Test-Path (Join-Path $portable 'bin\javac.exe')) {
        return $portable
    }

    throw 'A full JDK 8 is required. Set JAVA8_HOME or install it under .toolchains.'
}

$javaHome = Resolve-Java8Home
$env:JAVA_HOME = $javaHome
$env:Path = "$(Join-Path $javaHome 'bin');$env:Path"

$builds = @(
    [pscustomobject]@{
        Name = 'Forge 1.7.10'
        Directory = Join-Path $PSScriptRoot 'forge-1.7.10'
        PortableGradle = Join-Path $PSScriptRoot '.toolchains\gradle-4.4.1\gradle-4.4.1\bin\gradle.bat'
        Jar = Join-Path $PSScriptRoot 'forge-1.7.10\build\libs\NestedPackFix-1.7.10-1.0.0.jar'
    },
    [pscustomobject]@{
        Name = 'Forge 1.8.9'
        Directory = Join-Path $PSScriptRoot 'forge-1.8.9'
        PortableGradle = Join-Path $PSScriptRoot '.toolchains\gradle-2.7\gradle-2.7\bin\gradle.bat'
        Jar = Join-Path $PSScriptRoot 'forge-1.8.9\build\libs\NestedPackFix-1.8.9-1.0.0.jar'
    }
)

$failures = @()

foreach ($build in $builds) {
    Push-Location $build.Directory
    try {
        $gradle = if (Test-Path $build.PortableGradle) {
            $build.PortableGradle
        }
        else {
            '.\gradlew.bat'
        }

        $tasks = @()
        if ($Clean) {
            $tasks += 'clean'
        }
        $tasks += @('test', 'build', '--no-daemon')

        & $gradle @tasks
        if ($LASTEXITCODE -ne 0) {
            $failures += "$($build.Name) exited with code $LASTEXITCODE"
        }
    }
    catch {
        $failures += "$($build.Name): $($_.Exception.Message)"
    }
    finally {
        Pop-Location
    }
}

if ($failures.Count -gt 0) {
    throw "Build failures:`n$($failures -join "`n")"
}

if ($Command -eq 'release') {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

    foreach ($build in $builds) {
        if (-not (Test-Path $build.Jar)) {
            throw "Release JAR not found: $($build.Jar)"
        }

        $target = Join-Path $OutputDir (Split-Path $build.Jar -Leaf)
        Copy-Item -LiteralPath $build.Jar -Destination $target -Force
        $hash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
        Set-Content -LiteralPath "$target.sha256" -Value "$hash  $(Split-Path $target -Leaf)" -Encoding Ascii
    }
}
