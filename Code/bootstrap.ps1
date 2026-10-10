<#
.SYNOPSIS
  SepareWorldItems v3 bootstrap: prepares the Java versions, builds every platform and collects
  jars, API and default configs in Code/dist/SepareWorldItems-<version>/.

.DESCRIPTION
  1. Java: needs Azul Zulu JDK 8, 17, 21 and 25. Uses an existing install (C:\Program Files\Zulu\zulu-<n>,
     JAVA_HOME, PATH) or downloads a portable JDK into Code/.jdks/ (nothing is installed system-wide).
  2. Builds: plugin (3 jars), Fabric, Legacy Fabric 1.12.2, NeoForge, Forge, API.
  3. Collects everything into Code/dist/ and writes BUILD-REPORT.txt with what worked and what is missing.

.EXAMPLE
  ./bootstrap.ps1                      # everything
  ./bootstrap.ps1 -Only bukkit,fabric  # some platforms
  ./bootstrap.ps1 -SelfTest            # also run the in-game self tests (slow)
  ./bootstrap.ps1 -NoDownload          # never download JDKs, only use installed ones
#>
[CmdletBinding()]
param(
    [ValidateSet('bukkit', 'fabric', 'legacy', 'neoforge', 'forge')]
    [string[]] $Only = @('bukkit', 'fabric', 'legacy', 'neoforge', 'forge'),
    [switch] $SelfTest,
    [switch] $NoDownload
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$Code = $PSScriptRoot
$JdkDir = Join-Path $Code '.jdks'
$IsWin = [System.Environment]::OSVersion.Platform -eq 'Win32NT'
$Exe = if ($IsWin) { '.exe' } else { '' }
$Gradlew = if ($IsWin) { 'gradlew.bat' } else { 'gradlew' }
$Version = ((Get-Content (Join-Path $Code 'gradle.properties')) | Where-Object { $_ -match '^version=' }) -replace '^version=', ''
$Dist = Join-Path $Code "dist/SepareWorldItems-$Version"
$Report = [System.Collections.Generic.List[string]]::new()

function Say($msg, $color = 'Gray') { Write-Host $msg -ForegroundColor $color }

# ---------------------------------------------------------------- Java

function Get-JavaMajor([string] $jdkHome) {
    $java = Join-Path $jdkHome "bin/java$Exe"
    if (-not (Test-Path $java)) { return $null }
    $out = & $java -version 2>&1 | Out-String
    if ($out -match 'version "1\.(\d+)') { return [int]$Matches[1] }
    if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    return $null
}

function Find-Jdk([int] $major) {
    $candidates = @(
        (Join-Path $JdkDir "zulu-$major"),
        "C:\Program Files\Zulu\zulu-$major",
        "C:\Program Files\Java\jdk-$major",
        "C:\Program Files\Eclipse Adoptium\jdk-$major",
        "/usr/lib/jvm/zulu-$major", "/usr/lib/jvm/java-$major-openjdk-amd64",
        "/Library/Java/JavaVirtualMachines/zulu-$major.jdk/Contents/Home"
    )
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c) -and (Get-JavaMajor $c) -eq $major) { return (Resolve-Path $c).Path }
    }
    # Adoptium/Oracle style folders with full version names (jdk-25.0.1+9...)
    foreach ($root in @("C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Zulu", "/usr/lib/jvm")) {
        if (Test-Path $root) {
            foreach ($d in Get-ChildItem $root -Directory -ErrorAction SilentlyContinue) {
                if ((Get-JavaMajor $d.FullName) -eq $major) { return $d.FullName }
            }
        }
    }
    return $null
}

function Install-Jdk([int] $major) {
    $os = if ($IsWin) { 'windows' } elseif ($IsMacOS) { 'macos' } else { 'linux' }
    $arch = if ([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture -eq 'Arm64') { 'aarch64' } else { 'x64' }
    $type = if ($IsWin) { 'zip' } else { 'tar.gz' }
    $api = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=$major&os=$os&arch=$arch&archive_type=$type" +
           "&java_package_type=jdk&javafx_bundled=false&crac_supported=false&latest=true&release_status=ga&availability_types=CA&page_size=1"
    $pkg = (Invoke-RestMethod -Uri $api -UseBasicParsing)[0]
    if (-not $pkg) { throw "Azul has no JDK $major for $os/$arch" }
    Say "   downloading $($pkg.name) ..." 'DarkGray'
    New-Item -ItemType Directory -Force $JdkDir | Out-Null
    $archive = Join-Path $JdkDir $pkg.name
    Invoke-WebRequest -Uri $pkg.download_url -OutFile $archive -UseBasicParsing
    $tmp = Join-Path $JdkDir "tmp-$major"
    if (Test-Path $tmp) { Remove-Item -Recurse -Force $tmp }
    New-Item -ItemType Directory -Force $tmp | Out-Null
    if ($IsWin) { Expand-Archive -Path $archive -DestinationPath $tmp -Force } else { tar -xzf $archive -C $tmp }
    $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
    $homeDir = $inner.FullName
    if (Test-Path (Join-Path $homeDir 'Contents/Home')) { $homeDir = Join-Path $homeDir 'Contents/Home' }  # macOS bundle
    $target = Join-Path $JdkDir "zulu-$major"
    if (Test-Path $target) { Remove-Item -Recurse -Force $target }
    Move-Item $homeDir $target
    Remove-Item -Recurse -Force $tmp
    Remove-Item -Force $archive
    return $target
}

Say "`n== SepareWorldItems $Version bootstrap ==" 'Cyan'
Say "`n[1/3] Java"
$Jdks = @{}
foreach ($major in 8, 17, 21, 25) {
    $jdk = Find-Jdk $major
    if (-not $jdk -and -not $NoDownload) {
        try { $jdk = Install-Jdk $major } catch { Say "   JDK $major download failed: $_" 'Red' }
    }
    if ($jdk) {
        $Jdks[$major] = $jdk
        Say ("   JDK {0,-2} {1}" -f $major, $jdk) 'Green'
    } else {
        Say "   JDK $major missing" 'Yellow'
        $Report.Add("MISSING  JDK $major")
    }
}
if (-not $Jdks[25]) {
    Say "`nGradle needs JDK 25+ to run. Install it or run without -NoDownload." 'Red'
    exit 1
}
$env:JAVA_HOME = $Jdks[25]
$ToolchainPaths = ($Jdks.Values | Sort-Object) -join ','
$GradleArgs = @('--console=plain', "-Porg.gradle.java.installations.paths=$ToolchainPaths", '-Porg.gradle.java.installations.auto-download=false')

# ---------------------------------------------------------------- Builds

function Invoke-Gradle([string] $name, [string] $dir, [string[]] $tasks) {
    Say "   $name ..." 'DarkGray'
    $log = Join-Path $Code "build/bootstrap-$($name -replace '[^a-zA-Z0-9]', '-').log"
    New-Item -ItemType Directory -Force (Split-Path $log) | Out-Null
    Push-Location $dir
    try {
        & (Join-Path $dir $Gradlew) @tasks @GradleArgs *> $log
        $ok = $LASTEXITCODE -eq 0
    } finally { Pop-Location }
    if ($ok) {
        Say "   OK       $name" 'Green'; $Report.Add("OK       $name")
    } else {
        $err = (Select-String -Path $log -Pattern 'error:|What went wrong' -Context 0, 2 | Select-Object -First 1 | Out-String).Trim()
        Say "   FAILED   $name  (log: $log)" 'Red'; $Report.Add("FAILED   $name  -> $log`n         $err")
    }
    return $ok
}

Say "`n[2/3] Build"

Invoke-Gradle 'api + common' $Code @(':api:build', ':api:javadocJar', ':api:sourcesJar', ':common:test') | Out-Null
if ($Only -contains 'bukkit') { Invoke-Gradle 'plugin (java8 / java17 / java21)' $Code @(':bukkit:build') | Out-Null }
if ($Only -contains 'fabric') { Invoke-Gradle 'fabric' $Code @('build', '-x', ':bukkit:build') | Out-Null }
if ($Only -contains 'legacy')   { Invoke-Gradle 'fabric 1.12.2 (Legacy Fabric)' (Join-Path $Code 'fabric-legacy') @('build') | Out-Null }
if ($Only -contains 'neoforge') { Invoke-Gradle 'neoforge' (Join-Path $Code 'neoforge') @('build') | Out-Null }
if ($Only -contains 'forge')    { Invoke-Gradle 'forge' (Join-Path $Code 'forge') @('build') | Out-Null }

if ($SelfTest) {
    Say "`n   self tests (dev servers) ..." 'DarkGray'
    $tests = @()
    if ($Only -contains 'fabric')   { foreach ($d in Get-ChildItem (Join-Path $Code 'fabric/versions') -Directory) { $tests += , @("fabric $($d.Name)", $Code, ":fabric:$($d.Name):runServer") } }
    if ($Only -contains 'legacy')   { $tests += , @('fabric 1.12.2', (Join-Path $Code 'fabric-legacy'), 'runServer') }
    if ($Only -contains 'neoforge') { foreach ($d in Get-ChildItem (Join-Path $Code 'neoforge/versions') -Directory) { $tests += , @("neoforge $($d.Name)", (Join-Path $Code 'neoforge'), ":$($d.Name):runServer") } }
    if ($Only -contains 'forge')    { foreach ($d in Get-ChildItem (Join-Path $Code 'forge/versions') -Directory) { $tests += , @("forge $($d.Name)", (Join-Path $Code 'forge'), ":$($d.Name):runServer") } }
    foreach ($t in $tests) {
        $log = Join-Path $Code "build/selftest-$($t[0] -replace '[^a-zA-Z0-9.]', '-').log"
        Push-Location $t[1]
        try { & (Join-Path $t[1] $Gradlew) $t[2] '-PswiSelftest' @GradleArgs *> $log } finally { Pop-Location }
        $line = (Select-String -Path $log -Pattern '\[SelfTest\] (PASSED|FAILED).*' | Select-Object -Last 1).Matches.Value
        if (-not $line) { $line = 'did not run (see log)' }
        $status = if ($line -like '*PASSED*') { 'OK      ' } else { 'FAILED  ' }
        Say "   $status selftest $($t[0]): $line" $(if ($status -like 'OK*') { 'Green' } else { 'Red' })
        $Report.Add("$status selftest $($t[0]): $line")
    }
}

# ---------------------------------------------------------------- Collect

Say "`n[3/3] Collect -> $Dist"
if (Test-Path $Dist) { Remove-Item -Recurse -Force $Dist }
foreach ($p in 'plugin', 'fabric', 'neoforge', 'forge', 'api', 'configs/plugin', 'configs/mods') {
    New-Item -ItemType Directory -Force (Join-Path $Dist $p) | Out-Null
}
function Copy-Jars($from, $to) {
    $src = Join-Path $Code $from
    # Exactly this version (ignores older -SNAPSHOT or previous builds still in build/libs).
    $exact = "-$([regex]::Escape($Version))(-java\d+)?\.jar$"
    if (Test-Path $src) { Get-ChildItem $src -Filter '*.jar' | Where-Object { $_.Name -match $exact } | Copy-Item -Destination (Join-Path $Dist $to) }
}
Copy-Jars 'build/libs/bukkit' 'plugin'
Copy-Jars 'build/libs/fabric' 'fabric'
Copy-Jars 'build/libs/neoforge' 'neoforge'
Copy-Jars 'build/libs/forge' 'forge'
Get-ChildItem (Join-Path $Code 'api/build/libs') -Filter '*.jar' -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -match "^separeworlditems-api-$([regex]::Escape($Version))(-sources|-javadoc)?\.jar$" } | Copy-Item -Destination (Join-Path $Dist 'api')
Copy-Item (Join-Path $Code 'bukkit/src/main/resources/config.yml') (Join-Path $Dist 'configs/plugin/config.yml')
Copy-Item (Join-Path $Code 'fabric/src/main/resources/separeworlditems/config.yml') (Join-Path $Dist 'configs/mods/config.yml')

$expected = @{
    'plugin'   = @('java8', 'java17', 'java21')
    'fabric'   = (Get-ChildItem (Join-Path $Code 'fabric/versions') -Directory).Name + '1.12.2'
    'neoforge' = (Get-ChildItem (Join-Path $Code 'neoforge/versions') -Directory).Name
    'forge'    = (Get-ChildItem (Join-Path $Code 'forge/versions') -Directory).Name
}
$Report.Add('')
$Report.Add('Jars:')
foreach ($platform in 'plugin', 'fabric', 'neoforge', 'forge') {
    $have = (Get-ChildItem (Join-Path $Dist $platform) -Filter '*.jar' -ErrorAction SilentlyContinue).Name
    foreach ($e in $expected[$platform]) {
        $jar = $have | Where-Object { $_ -match "-$([regex]::Escape($e))[-.]" } | Select-Object -First 1
        if ($jar) { $Report.Add("  OK       $platform/$jar") } else { $Report.Add("  MISSING  $platform $e") }
    }
}

$header = @(
    "SepareWorldItems $Version - build report ($(Get-Date -Format 'yyyy-MM-dd HH:mm'))",
    '',
    'plugin/    Spigot/Paper/Purpur/Folia: -java8 = MC 1.8.8-1.16.5, -java17 = 1.17-1.20.4, -java21 = 1.20.5-26.x',
    'fabric/    Fabric (needs Fabric API; 1.12.2 needs Legacy Fabric API)',
    'neoforge/  NeoForge',
    'forge/     MinecraftForge',
    'api/       separeworlditems-api (compile against it, do not install it on a server)',
    'configs/   plugin/config.yml -> plugins/SepareWorldItems/   mods/config.yml -> config/separeworlditems/',
    ''
)
($header + $Report) | Set-Content -Encoding UTF8 (Join-Path $Dist 'BUILD-REPORT.txt')

$missing = $Report | Where-Object { $_ -match 'MISSING|FAILED' }
Say ''
if ($missing) {
    Say "Done with problems ($($missing.Count)):" 'Yellow'
    $missing | ForEach-Object { Say "  $_" 'Yellow' }
} else {
    Say 'Done: everything built.' 'Green'
}
Say "Output: $Dist" 'Cyan'
