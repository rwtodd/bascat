# JBasCat

A java port of the bascat utility.  I mainly wrote it to compare it to the scala varsion.
In general, I'd say it's as clean or cleaner than the scala version, with two exceptions:

  * I had to roll my own TakeWhile() extension to Java8 Streams.  Java9 will fix this.
  * The pattern-matching part to change the way certain sequences are printed looks much cleaner in scala.
  
