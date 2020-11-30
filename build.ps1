# build script to put everything in the right place....

dotnet build -c Release -o $PSScriptRoot\RWTodd.GWBasic $PSScriptRoot\src\RWTodd.GWBasic.csproj
New-ExternalHelp -Path $PSScriptRoot\Docs-en-US -OutputPath $PSScriptRoot\RWTodd.GWBasic\en-US\ -Force
