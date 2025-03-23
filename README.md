# bascat

bascat is a program to print out tokenized gwbasic .BAS files in ASCII.  There are 
actually a fair amount of .BAS files out there in the default tokenized format, 
but you'd need a working GWBASIC/BASICA to see the source.

This is mostly a toy program that I'm using to help get familiar with
Go, but it might be useful to someone.  

## Multiple Languages

The original version I did was in Go (in this branch). I did several other verisions in
other languages (scala, java, kotlin, fsharp etc.), and most of them are on other
branches of this repository.

## Unprotect Feature

It was possible to save your file encrypted in GW-BASIC, and I found the decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/) project. So,
I implemented that decryption scheme... and in some basic tests running GW-BASIC 3.23 in DOSBOX it does work.


## Licesnse

I put this implementation under the MIT license 

## Referece

The documentation I used for the tokenized file format was
here: [http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

