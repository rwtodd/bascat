---
external help file: RWTodd.GWBasic.dll-Help.xml
Module Name: RWTodd.GWBasic
online version:
schema: 2.0.0
---

# ConvertFrom-GWBasic

## SYNOPSIS
Convert a GWBASIC or BASICA tokenized basic file to plain text.

## SYNTAX

```
ConvertFrom-GWBasic [-BasFile] <String> [<CommonParameters>]
```

## DESCRIPTION
Old DOS basic interpreters saved their source files in tokenized format.  This command converts from that tokenized format to plain text, which it outputs.

It was possible to save your file encrypted in GW-BASIC, and I found the decryption algorithm in the PC-BASIC project (http://sourceforge.net/p/pcbasic/wiki/Home/). So, I implemented that decryption scheme... however I do not have any encrypted BAS files to test it on, so I don't know if it works.

## EXAMPLES

### Example 1
```powershell
PS C:\> ConvertFrom-GWBasic myfile.bas > myfile.bas.txt
```

Converts `myfile.bas` to plain text, and saves that text into `myfile.bas.txt`.

### Example 2
```powershell
PS C:\> gci *.gwbas | ConvertFrom-GWBasic -Verbose
```

Converts all the .gwbas files in the directory to plain text and outputs to the console. The `-Verbose` flag outputs a line between the files indicating the file name to be parsed.

## PARAMETERS

### -BasFile
The GWBASIC/BASICA tokenized file to convert to plain text.  It is a literal path (no wildcards).  If you need wildcards, pipe items into the cmdlet instead.

```yaml
Type: String
Parameter Sets: (All)
Aliases:

Required: True
Position: 0
Default value: None
Accept pipeline input: True (ByPropertyName, ByValue)
Accept wildcard characters: False
```

### CommonParameters
This cmdlet supports the common parameters: -Debug, -ErrorAction, -ErrorVariable, -InformationAction, -InformationVariable, -OutVariable, -OutBuffer, -PipelineVariable, -Verbose, -WarningAction, and -WarningVariable. For more information, see [about_CommonParameters](http://go.microsoft.com/fwlink/?LinkID=113216).

## INPUTS

### System.String
The name of a tokenized basic file.

## OUTPUTS

### System.String
The plain text conversion of the input file.

## NOTES

## RELATED LINKS

[GW-BASIC Token List](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html)
