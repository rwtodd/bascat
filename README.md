# bascat
A c (unix) program to decode GW-BASIC/BASICA tokenized files.

`bascat` is a program to print out tokenized gwbasic .BAS files in ASCII.
There are actually a fair amount of .BAS files out there in the default tokenized
format, but you'd need a working GWBASIC/BASICA to see the source.

This is a C (UNIX) version.  I have implemented the program in many languages before.

## Building/Installing

To install it to `/usr/local`:

    make && sudo make install

To install somewhere else:

    make PREFIX=/wherever/you/like install

The makefile is very straightforward to edit to your liking if you want
something more complicated.

## Decryption

It was possible to save your file encrypted in GW-BASIC, and I found the decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/)
project. So, I implemented that decryption scheme... however I do not have any
encrypted BAS files to test it on, so I don't know if it works.

## Referece

The documentation I used for the tokenized file format was here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

## Multiple Languages

I've written BasCat in multiple languages as a learning exercise.

 - Go [Go.Bascat repo](https://github.com/rwtodd/Go.Bascat)
 - Python [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Common Lisp [rt-bascat repo](https://github.com/rwtodd/rt-bascat)
 - Scala (in a private repo)
 - Java (in a private repo)
 - JavaScript  (in a private repo)
 - Kotlin [Kotlin.BasCat repo](https://github.com/rwtodd/Kotlin.BasCat)
 - Rust [Rust.BasCat repo](https://github.com/rwtodd/Rust.BasCat)

