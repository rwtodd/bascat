# Run the debug binary, and then compare the output
# to known good output.
# The run of fc.exe should leave $LASTEXITCODE set appropriately.

& "${PSSCRIPTROOT}\..\x64\Debug\BasCat\BasCat.exe" "${PSSCRIPTROOT}\..\Tests\NEPTUNE.gwbas" > "$env:TEMP\bctest.txt"
& fc.exe "${PSSCRIPTROOT}\..\Tests\NEPTUNE.txt"  "$env:TEMP\bctest.txt"
