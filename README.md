# BasCat

`bascat` is a program to print out tokenized gwbasic .BAS files in ASCII.
There are actually a fair amount of .BAS files out there in the default tokenized
format, but you'd need a working GWBASIC/BASICA to see the source.

This is a CSharp/DotNet Core version.  I have implemented the program in many languages before, 
but this is among the simplest of them.  In most of the previous versions,
I parse the input into a Token stream, and then clean up and print the stream.
At the time, I was thinking about the ability to run statistics on the tokens
and similar uses, but at this point I consider that overkill. This version
just prints the tokens directly.

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

I've written BasCat in multiple languages as a learning exercise.

 - Go [Go.Bascat repo](https://github.com/rwtodd/Go.Bascat)
 - Python [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Common Lisp [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - Scala [Scala.BasCat repo](https://github.com/rwtodd/Scala.BasCat)
 - Older Scala version (in a private repo)
 - Java [SmallProgs 18 repo](https://github.com/rwtodd/small\_programs\_2018)
 - JavaScript  (in a private repo)
 - Kotlin [Kotlin.BasCat repo](https://github.com/rwtodd/Kotlin.BasCat)

