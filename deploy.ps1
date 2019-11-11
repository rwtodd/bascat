# This is just the deployment script I use on my machines... where you put the binaries
# is your business.
$progName = "bascat"
$modName = "org.rwtodd.bascat"

mvn package
$tgtDir = Join-Path (Resolve-Path "~") bin "_$progName"
$tgtScript = Join-Path (Resolve-Path "~") bin "$progName.ps1"

if (Test-Path $tgtDir) {
  Remove-Item -Force -Recurse $tgtDir
}
if (Test-Path $tgtScript) {
  Remove-Item -Force $tgtScript
}


New-Item -Type Directory -Path $tgtDir
Copy-Item -Path target\*.jar -Destination $tgtDir

Set-Content -Path $tgtScript -Value @"
& java -p "$tgtDir\$progName.jar" -m $modName `@args
"@
