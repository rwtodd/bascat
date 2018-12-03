# BasCat

`bascat` is a program to print out tokenized gwbasic .BAS files in ASCII.
There are actually a fair amount of .BAS files out there in the default tokenized
format, but you'd need a working GWBASIC/BASICA to see the source.

This is a Scala version.  I have implemented the program in many languages before, 
but this implementation is distinguished by its devotion to functional programming.  
It uses a bespoke State monad as it parses an immutable infinite stream of bytes.
Performance is acceptable, but nowhere near my C version, which is imperative and
operates on a mmap-ed array!

## Decryption

It was possible to save your file encrypted in GW-BASIC, and I found the
decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/)
project. So,
I implemented that decryption scheme... however I do not have any
encrypted BAS files
to test it on, so I don't know if it works.

## Referece

The documentation I used for the tokenized file format was
here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

## Multiple Languages

I've written BasCat in multiple languages as a learning exercise.  Here are some of them:

 - Go [Go.Bascat repo](https://github.com/rwtodd/Go.Bascat)
 - Python [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Common Lisp [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Older Scala version (in a private repo)
 - Java [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - JavaScript  (in a private repo)
 - Kotlin [Kotlin.BasCat repo](https://github.com/rwtodd/Kotlin.BasCat)

