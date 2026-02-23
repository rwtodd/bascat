# To make the help md files in the first place:
#  New-MarkdownCommandHelp -ModuleInfo (Get-Module -Name RWTodd.GWBasic.PowerShell) -OutputFolder ./docs-en-US -WithModulePage 


# build script to put everything in the right place....

dotnet build -c Release -p:BuildForPowerShell=true -o $PSScriptRoot/RWTodd.GWBasic.PowerShell $PSScriptRoot/RWTodd.GWBasic/RWTodd.GWBasic.csproj

Measure-PlatyPSMarkdown -Path $PSScriptRoot/docs-en-US/RWTodd.GWBasic.PowerShell/*.md |
Where-Object { $_.FileType -match 'CommandHelp' } |
Import-MarkdownCommandHelp -Path { $_.FilePath } |
Export-MamlCommandHelp -OutputFolder $PSScriptRoot/maml -Force

if (!(Test-Path $PSScriptRoot/RWTodd.GWBasic.PowerShell/en-US)) { mkdir $PSScriptRoot/RWTodd.GWBasic.PowerShell/en-US }
Copy-Item $PSScriptRoot/maml/RWTodd.GWBasic.PowerShell/*.xml $PSScriptRoot/RWTodd.GWBasic.PowerShell/en-US/

# to create a nupkg... use:
# Compress-PSResource -Path ./RWTodd.GWBasic.PowerShell -DestinationPath .
# and for the c#:
# dotnet pack ./RWTodd.GWBasic.csproj -c Release -o .