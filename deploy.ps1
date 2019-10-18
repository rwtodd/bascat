# This is just the deployment script I use on my machines... where you put the binaries
# is your business.
$progName = "bascat"

dotnet publish -c Release
$tgtDir = "~\bin\_$progName"
$tgtScript = "~\bin\$progName.ps1"

if (Test-Path $tgtDir) {
  Remove-Item -Force -Recurse $tgtDir
}
if (Test-Path $tgtScript) {
  Remove-Item -Force $tgtScript
}

Copy-Item -Recurse -LiteralPath bin\Release\netcoreapp3.0\publish -Destination $tgtDir
Set-Content -Path $tgtScript -Value @"
& "`$PSScriptRoot\_$progName\$progName.exe" `@args
"@
