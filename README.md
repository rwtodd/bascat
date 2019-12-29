# BASCAT

# BasCat

`bascat` is a program to print out tokenized gwbasic .BAS files in ASCII.
There are actually a fair amount of .BAS files out there in the default tokenized
format, but you'd need a working GWBASIC/BASICA to see the source. Not anymore.

## Unprotect Feature

It was possible to save your file encrypted in GW-BASIC, and I found the decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/)
project. So, I implemented that decryption scheme... however I do not have any
encrypted BAS files to test it on, so I don't know if it works.

## Referece

The documentation I used for the tokenized file format was
here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

The floating-point format used in the files is described here:
[https://en.wikipedia.org/wiki/Microsoft_Binary_Format](https://en.wikipedia.org/wiki/Microsoft_Binary_Format)

## Multiple Languages

I've written BasCat in multiple languages as a learning exercise.  
Most of them are in this repo, in different branches (e.g., win32-version,
scala-version, etc.)

## License

MIT License... see LICENSE file in the repo
