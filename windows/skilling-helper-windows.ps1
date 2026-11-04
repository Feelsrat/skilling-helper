# Skilling Helper for Windows: installs, updates and starts the RuneLite dev client with all the plugins built in.
#
# First time, in PowerShell (Start menu > type "PowerShell" > Enter), paste:
#   irm https://github.com/Feelsrat/skilling-helper/releases/latest/download/skilling-helper-windows.ps1 | iex
# (or right-click the downloaded skilling-helper-windows.ps1 > "Run with PowerShell").
#
# That puts a "Skilling Helper" icon on the Desktop and in the Start menu. Every time you start it, it first gets
# the newest plugins (and a newer copy of this script) from the latest GitHub release, then opens RuneLite.
# If there's no internet it just starts what it already has.
#
# Everything lives in %LOCALAPPDATA%\SkillingHelper: jre\ (a private Java 21), client.jar, version.txt, launcher.log.
# Your RuneLite settings and Jagex login are the normal ones in %USERPROFILE%\.runelite.
#
# Options (after -File ...ps1): -NoLaunch (only update), -Reinstall (download Java and the client again).
# This only downloads files and starts Java; it never clicks or types anything in the game.

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # Invoke-WebRequest is very slow with its progress bar on
try { [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12 } catch { }

$Releases = 'https://github.com/Feelsrat/skilling-helper/releases/latest/download/'
$ScriptAsset = 'skilling-helper-windows.ps1'
$JreApi = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse'
$MinJarSize = 10MB

$Dir = Join-Path $env:LOCALAPPDATA 'SkillingHelper'
$Installed = Join-Path $Dir $ScriptAsset
$Jre = Join-Path $Dir 'jre'
$Javaw = Join-Path $Jre 'bin\javaw.exe'
$Jar = Join-Path $Dir 'client.jar'
$VersionFile = Join-Path $Dir 'version.txt'
$Log = Join-Path $Dir 'launcher.log'
$RuneLiteHome = Join-Path $env:USERPROFILE '.runelite'
$Login = Join-Path $RuneLiteHome 'credentials.properties'

$NoLaunch = $args -contains '-NoLaunch'
$Reinstall = $args -contains '-Reinstall'
$Updated = $args -contains '-Updated'

New-Item -ItemType Directory -Force -Path $Dir | Out-Null

function Say([string]$msg) {
	Write-Host $msg
	try { Add-Content -Path $Log -Value ((Get-Date -Format 'yyyy-MM-dd HH:mm:ss ') + $msg) } catch { }
}

function Show-Box([string]$msg, [string]$buttons = 'OK', [string]$icon = 'Information') {
	try {
		Add-Type -AssemblyName System.Windows.Forms
		return [string][System.Windows.Forms.MessageBox]::Show($msg, 'Skilling Helper', $buttons, $icon)
	} catch {
		Write-Host $msg
		return 'OK'
	}
}

function Fail([string]$msg) {
	Say $msg
	Show-Box $msg 'OK' 'Error' | Out-Null
	exit 1
}

function Get-File([string]$url, [string]$out) {
	# ?t= stops any cache in between from handing back an older release
	$sep = '?'
	if ($url.Contains('?')) { $sep = '&' }
	Invoke-WebRequest -UseBasicParsing -Uri ($url + $sep + 't=' + [DateTime]::UtcNow.Ticks) -OutFile $out `
		-Headers @{ 'User-Agent' = 'skilling-helper-windows' } -TimeoutSec 120
}

function Get-Text([string]$url) {
	$tmp = Join-Path $Dir 'download.tmp'
	try {
		Get-File $url $tmp
		return (Get-Content -Raw -Path $tmp).Trim()
	} finally {
		Remove-Item -Force -ErrorAction SilentlyContinue $tmp
	}
}

# --- Install: copy this script to %LOCALAPPDATA% and make the icons --------------------------------------

function Install-Self {
	# Run from a file (Run with PowerShell) or straight from the web (irm | iex): either way keep a copy in $Dir
	$from = $PSCommandPath
	if ($from -and (Test-Path $from)) {
		if ((Resolve-Path $from).Path -ne $Installed) { Copy-Item -Force $from $Installed }
	} else {
		Get-File ($Releases + $ScriptAsset) $Installed
	}
	try { Unblock-File -Path $Installed } catch { }
}

function New-Shortcut([string]$path) {
	$shell = New-Object -ComObject WScript.Shell
	$lnk = $shell.CreateShortcut($path)
	$lnk.TargetPath = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
	$lnk.Arguments = '-NoProfile -ExecutionPolicy Bypass -WindowStyle Minimized -File "' + $Installed + '"'
	$lnk.WorkingDirectory = $Dir
	$lnk.Description = 'Update Skilling Helper and start RuneLite'
	$icon = Join-Path $env:LOCALAPPDATA 'RuneLite\RuneLite.exe'
	if (-not (Test-Path $icon)) { $icon = $Javaw }
	$lnk.IconLocation = $icon + ',0'
	$lnk.Save()
}

function Install-Shortcuts {
	$desktop = [Environment]::GetFolderPath('Desktop')
	$menu = Join-Path ([Environment]::GetFolderPath('Programs')) 'Skilling Helper.lnk'
	$fresh = -not (Test-Path $menu)
	foreach ($p in @((Join-Path $desktop 'Skilling Helper.lnk'), $menu)) {
		try { New-Shortcut $p } catch { Say "Couldn't make the shortcut $p : $($_.Exception.Message)" }
	}
	if ($fresh) { Say 'Added a "Skilling Helper" icon to the Desktop and the Start menu.' }
}

# --- Self-update -------------------------------------------------------------------------------------------

function Update-Self {
	if ($Updated) { return }
	$new = $Installed + '.new'
	try {
		Get-File ($Releases + $ScriptAsset) $new
		$body = Get-Content -Raw -Path $new
		$old = ''
		if (Test-Path $Installed) { $old = Get-Content -Raw -Path $Installed }
		if ($body -and $body.Contains('$ScriptAsset = ') -and $body -ne $old) {
			Move-Item -Force $new $Installed
			try { Unblock-File -Path $Installed } catch { }
			Say 'Got a newer launcher; restarting it.'
			$pass = @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $Installed, '-Updated') + $args
			& (Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe') @pass
			exit $LASTEXITCODE
		}
	} catch {
		Say "Couldn't check for a newer launcher: $($_.Exception.Message)"
	} finally {
		Remove-Item -Force -ErrorAction SilentlyContinue $new
	}
}

# --- Java --------------------------------------------------------------------------------------------------

function Install-Java {
	if ((Test-Path $Javaw) -and -not $Reinstall) { return }
	Say 'Downloading Java 21 (about 45 MB, only the first time)...'
	$info = Invoke-RestMethod -UseBasicParsing -Uri $JreApi -Headers @{ 'User-Agent' = 'skilling-helper-windows' }
	$pkg = @($info)[0].binary.package
	$zip = Join-Path $Dir 'jre.zip'
	$tmp = Join-Path $Dir 'jre.tmp'
	try {
		Get-File $pkg.link $zip
		$sum = (Get-FileHash -Algorithm SHA256 -Path $zip).Hash.ToLower()
		if ($sum -ne $pkg.checksum.ToLower()) { throw 'the Java download is corrupt; start again' }
		Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $tmp
		Expand-Archive -Path $zip -DestinationPath $tmp
		$inner = Get-ChildItem -Directory -Path $tmp | Select-Object -First 1
		if (-not $inner -or -not (Test-Path (Join-Path $inner.FullName 'bin\javaw.exe'))) { throw 'the Java download has no javaw.exe' }
		Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $Jre
		Move-Item $inner.FullName $Jre
		Say ('Installed ' + $pkg.name)
	} finally {
		Remove-Item -Force -ErrorAction SilentlyContinue $zip
		Remove-Item -Recurse -Force -ErrorAction SilentlyContinue $tmp
	}
}

# --- Plugins (the client jar with every plugin built in) ---------------------------------------------------

function Update-Client {
	$latest = Get-Text ($Releases + 'android-version.txt')
	if ($latest -notmatch '^v[0-9.]+$') { throw "unexpected version '$latest'" }
	$have = ''
	if (Test-Path $VersionFile) { $have = (Get-Content -Raw -Path $VersionFile).Trim() }
	if ((Test-Path $Jar) -and $have -eq $latest -and -not $Reinstall) {
		Say "Plugins are up to date ($latest)."
		return
	}
	Say "Downloading Skilling Helper $latest..."
	$new = $Jar + '.new'
	try {
		Get-File ($Releases + 'android-client.jar') $new
		if ((Get-Item $new).Length -lt $MinJarSize) { throw 'the download is too small' }
		try {
			Move-Item -Force $new $Jar
		} catch {
			throw 'RuneLite seems to be open already (client.jar is in use); close it to update'
		}
		Set-Content -Path $VersionFile -Value $latest
		if ($have) { Say "Updated $have -> $latest." } else { Say "Installed $latest." }
	} finally {
		Remove-Item -Force -ErrorAction SilentlyContinue $new
	}
}

# --- Jagex login -------------------------------------------------------------------------------------------

function Find-Login {
	if (Test-Path $Login) { return $true }
	# The Steam Deck "phone kit" zip carries the login too: home/.runelite/credentials.properties
	$downloads = Join-Path $env:USERPROFILE 'Downloads'
	$kits = @(Get-ChildItem -ErrorAction SilentlyContinue -Path $downloads -Filter 'skilling-helper-phone*.zip' | Sort-Object LastWriteTime -Descending)
	foreach ($kit in $kits) {
		try {
			Add-Type -AssemblyName System.IO.Compression.FileSystem
			$z = [System.IO.Compression.ZipFile]::OpenRead($kit.FullName)
			try {
				$entry = $z.Entries | Where-Object { $_.FullName -eq 'home/.runelite/credentials.properties' } | Select-Object -First 1
				if ($entry) {
					New-Item -ItemType Directory -Force -Path $RuneLiteHome | Out-Null
					[System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $Login, $true)
					Say ('Took your Jagex login from ' + $kit.Name + '.')
					return $true
				}
			} finally {
				$z.Dispose()
			}
		} catch {
			Say ("Couldn't read " + $kit.Name + ": $($_.Exception.Message)")
		}
	}
	# Folders unzipped by hand
	foreach ($d in @(Get-ChildItem -Directory -ErrorAction SilentlyContinue -Path $downloads -Filter 'skilling-helper-phone*')) {
		$f = Join-Path $d.FullName 'home\.runelite\credentials.properties'
		if (Test-Path $f) {
			New-Item -ItemType Directory -Force -Path $RuneLiteHome | Out-Null
			Copy-Item -Force $f $Login
			Say ('Took your Jagex login from ' + $d.Name + '.')
			return $true
		}
	}
	return $false
}

function Confirm-Login {
	if (Find-Login) { return }
	$msg = "No saved Jagex login yet, so RuneLite can only show the old-style login screen.`n`n" +
		"To save it (once):`n" +
		"1. Start menu > RuneLite (configure). In Client arguments add --insecure-write-credentials, then Save.`n" +
		"2. Start RuneLite from the Jagex Launcher once and close it.`n" +
		"   (Or put skilling-helper-phone.zip from the Steam Deck in your Downloads folder.)`n" +
		"3. Start Skilling Helper again.`n`n" +
		"See github.com/runelite/runelite/wiki/Using-Jagex-Accounts.`n`nStart RuneLite anyway?"
	if ((Show-Box $msg 'YesNo' 'Warning') -ne 'Yes') { exit 0 }
}

# --- Main --------------------------------------------------------------------------------------------------

Say '--- Skilling Helper launcher'
$fromElsewhere = -not (Test-Path $Installed) -or ($PSCommandPath -and (Test-Path $PSCommandPath) -and (Resolve-Path $PSCommandPath).Path -ne $Installed)
if ($fromElsewhere) {
	try { Install-Self } catch { Say "Couldn't copy the launcher to $Dir : $($_.Exception.Message)" }
}
if (Test-Path $Installed) { Update-Self @args }

try {
	Install-Java
} catch {
	if (-not (Test-Path $Javaw)) { Fail "Couldn't download Java: $($_.Exception.Message)`n`nCheck the internet connection and start again." }
	Say "Couldn't reinstall Java, keeping the old one: $($_.Exception.Message)"
}

try {
	Update-Client
} catch {
	if (-not (Test-Path $Jar)) { Fail "Couldn't download Skilling Helper: $($_.Exception.Message)`n`nCheck the internet connection and start again." }
	Say "Couldn't update, starting the version you have: $($_.Exception.Message)"
}

if (Test-Path $Installed) { try { Install-Shortcuts } catch { Say "Couldn't make the icons: $($_.Exception.Message)" } }

if ($NoLaunch) { exit 0 }

Confirm-Login
Say 'Starting RuneLite.'
Start-Process -FilePath $Javaw -WorkingDirectory $Dir -ArgumentList @('-ea', '-Xmx1g', "`"-XX:ErrorFile=$Dir\crash-%p.log`"", '-jar', "`"$Jar`"", '--developer-mode')
