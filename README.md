# RWTodd.GWBasic PS Module

This is a PowerShell Module for converting old tokenized .BAS files to plain text.  It provides a
single cmdlet, `Get-GWBasicContent`, which does the work.  A C# library does the heavy lifting, and
the binary module wraps it in a nice interface.

In other branches of this repository I have console programs to do the same thing in
many languages.  If you prefer, get one of those running.

## Note: Decryption

It was possible to save your file encrypted in GW-BASIC, and I found the
decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/)
project. So,
I implemented that decryption scheme... and in some basic tests running GW-BASIC 3.23 in DOSBOX it does work.

## Referece

The documentation I used for the tokenized file format was
here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).
