# bascat

bascat is a program to print out tokenized gwbasic .BAS files in ASCII.  There are 
actually a fair amount of .BAS files out there in the default tokenized format, 
but you'd need a working GWBASIC/BASICA to see the source.

This is mostly a toy program that I'm using to help get familiar with
Go, but it might be useful to someone.  

## Multiple Languages

The original version I did was in Go.  But, in March 2016 I 
created a Scala version and an F# version.  Of the three,
I think the F# version came out the cleanest.  It's a really
nice language!  Anyway, all three are in this repo available
for comparison.

## Go Get

You should be able to install this utility with:

    go get go.waywardcode.com/bascat


## Unprotect Feature

It was possible to save your file encrypted in GW-BASIC, and I found the decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/) project. So,
I implemented that decryption scheme... however I do not have any encrypted BAS files
to test it on, so I don't know if it works.


## Licesnse

I put this implementation under GPL V2

## Referece

The documentation I used for the tokenized file format was
here: [http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

