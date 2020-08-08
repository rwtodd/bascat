# RT-BASCAT

`bascat` is a program to print out tokenized gwbasic .BAS files in
ASCII.  There are actually a fair amount of .BAS files out there in
the default tokenized format, but you'd need a working GWBASIC/BASICA
to see the source.

I have written many versions of this utility in various languages. This
repo is a common lisp version (asdf:load-system "rt-bascat").

## Unprotect Feature

It was possible to save your file encrypted in GW-BASIC, and I found
the decryption algorithm in the
[PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/) project. So, I
implemented that decryption scheme... however I do not have any
encrypted BAS files to test it on, so I don't know if it works.

## Referece

The documentation I used for the tokenized file format was here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

## Building .EXE

In sbcl...

    (require 'asdf)
    (asdf:make "rt-bascat")

For ECL...

    (load "build_for_ecl.lisp")

## Multiple Languages

I've written BasCat in multiple languages as a learning exercise.
Most of them are in this very repository, each in its own branch.
Check them out!

