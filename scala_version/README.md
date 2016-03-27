# BasCat Scala Version

I rewrote the Go program (also in this repo) in Scala, to get a sense
of the difference.  The implementation is more resource-hungry (due to
JVM overhead and also higher-level constructs like Streams), but 
substantially fewer lines of code.

One big win for Scala was the output filtering (where certain 
combinations of tokens are replaced with simpler tokens).  In 
the Go codebase, a the `output_filtered` function works as a 
state machine.  The Scala `printLine` function is much more 
declarative.  I would much rather maintain the Scala.

The other win, in my mind, was centralizing the error handling
for unexpected EOF in one place.  In contrast, the Go version
has to check for EOF after every read operation.

