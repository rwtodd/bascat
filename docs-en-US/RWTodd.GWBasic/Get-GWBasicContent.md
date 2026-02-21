---
document type: cmdlet
external help file: RWTodd.GWBasic.dll-Help.xml
Locale: en-US
Module Name: RWTodd.GWBasic
ms.date: 02/21/2026
PlatyPS schema version: 2024-05-01
title: Get-GWBasicContent
---

# Get-GWBasicContent

## SYNOPSIS

Convert a GWBASIC or BASICA tokenized basic file to plain text.

## SYNTAX

### __AllParameterSets

```
Get-GWBasicContent [-Path] <string[]> [-WhatIf] [-Confirm] [<CommonParameters>]
```

## ALIASES

This cmdlet has the following aliases,
  {{Insert list of aliases}}

## DESCRIPTION

Old DOS basic interpreters saved their source files in tokenized format.
 This command converts from that tokenized format to plain text, which it outputs.

It was possible to save your file encrypted in GW-BASIC, and I found the decryption algorithm in the PC-BASIC project (http://sourceforge.net/p/pcbasic/wiki/Home/). So, I implemented that decryption scheme.

## EXAMPLES

### Example 1
```powershell
PS C:\> Get-GWBasicContent myfile.bas > myfile.bas.txt
```

Converts `myfile.bas` to plain text, and saves that text into `myfile.bas.txt`.

### Example 2
```powershell
PS C:\> gci *.gwbas | Get-GWBasicContent > myfiles.bas.txt
```

Converts all the gwbas files to plain text, and saves that text into `myfiles.bas.txt`.

## PARAMETERS

### -Confirm

Prompts you for confirmation before running the cmdlet.

```yaml
Type: System.Management.Automation.SwitchParameter
DefaultValue: ''
SupportsWildcards: false
Aliases:
- cf
ParameterSets:
- Name: (All)
  Position: Named
  IsRequired: false
  ValueFromPipeline: false
  ValueFromPipelineByPropertyName: false
  ValueFromRemainingArguments: false
DontShow: false
AcceptedValues: []
HelpMessage: ''
```

### -Path

The GWBASIC/BASICA tokenized file to convert to plain text.
 Wildcards are allowed.

```yaml
Type: System.String[]
DefaultValue: ''
SupportsWildcards: false
Aliases:
- BasFile
ParameterSets:
- Name: (All)
  Position: 0
  IsRequired: true
  ValueFromPipeline: true
  ValueFromPipelineByPropertyName: true
  ValueFromRemainingArguments: false
DontShow: false
AcceptedValues: []
HelpMessage: ''
```

### -WhatIf

Runs the command in a mode that only reports what would happen without performing the actions.

```yaml
Type: System.Management.Automation.SwitchParameter
DefaultValue: ''
SupportsWildcards: false
Aliases:
- wi
ParameterSets:
- Name: (All)
  Position: Named
  IsRequired: false
  ValueFromPipeline: false
  ValueFromPipelineByPropertyName: false
  ValueFromRemainingArguments: false
DontShow: false
AcceptedValues: []
HelpMessage: ''
```

### CommonParameters

This cmdlet supports the common parameters: -Debug, -ErrorAction, -ErrorVariable,
-InformationAction, -InformationVariable, -OutBuffer, -OutVariable, -PipelineVariable,
-ProgressAction, -Verbose, -WarningAction, and -WarningVariable. For more information, see
[about_CommonParameters](https://go.microsoft.com/fwlink/?LinkID=113216).

## INPUTS

### System.String

The name of a tokenized basic file.

### System.String[]

{{ Fill in the Description }}

## OUTPUTS

### System.String

The plain text conversion of the input file.

## NOTES




## RELATED LINKS

- [GW-BASIC Token List](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html)
