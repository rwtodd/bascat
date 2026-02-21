# To make the help md files in the first place:
#  New-MarkdownCommandHelp -ModuleInfo (Get-Module -Name RWTodd.GWBasic) -OutputFolder ./docs-en-US -WithModulePage 


# build script to put everything in the right place....

dotnet build -c Release -p:BuildForPowerShell=true -o $PSScriptRoot/RWTodd.GWBasic $PSScriptRoot/src/RWTodd.GWBasic.csproj

Measure-PlatyPSMarkdown -Path ./docs-en-US/RWTodd.GWBasic/*.md |                   
   Where-Object Filetype -match 'CommandHelp' |
   Import-MarkdownCommandHelp -Path {$_.FilePath} |
   Export-MamlCommandHelp -OutputFolder .\maml

mkdir ./RWTodd.GWBasic/en-US
copy ./maml/RWTodd.GWBasic/*.xml ./RWTodd.GWBasic/en-US/

