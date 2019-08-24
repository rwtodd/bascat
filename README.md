# BASCAT

# BasCat

`bascat` is a program to print out tokenized gwbasic .BAS files in ASCII.
There are actually a fair amount of .BAS files out there in the default tokenized
format, but you'd need a working GWBASIC/BASICA to see the source.

## Unprotect Feature

It was possible to save your file encrypted in GW-BASIC, and I found the decryption
algorithm in the [PC-BASIC](http://sourceforge.net/p/pcbasic/wiki/Home/)
project. So, I implemented that decryption scheme... however I do not have any
encrypted BAS files to test it on, so I don't know if it works.

## Referece

The documentation I used for the tokenized file format was
here:
[http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html](http://chebucto.ns.ca/~af380/GW-BASIC-tokens.html).

## Multiple Languages

I've written BasCat in multiple languages as a learning exercise.  Here are some, as of
this writing:

 - Scala [Scala.BasCat repo](https://github.com/rwtodd/Scala.BasCat)
 - Kotlin [Kotlin.BasCat repo](https://github.com/rwtodd/Kotlin.BasCat)
 - Go [Go.Bascat repo](https://github.com/rwtodd/Go.Bascat)
 - Python [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Common Lisp [rt-bascat repo](https://github.com/rwtodd/rt-bascat)
 - Java [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - CSharp [Csharp.BasCat repo](https://github.com/rwtodd/CSharp.BasCat)
 - Rust [Rust.Bascat repo](https://github.com/rwtodd/Rust.Bascat)
 - JavaScript  (in a private repo)

