# BasCat Scala Version

I rewrote the Go program (also in this repo) in Scala, to get a sense
of the difference.  I really thought the Scala one would be much 
smaller, but the difference wasn't very big.

One big win for Scala was the output filtering (where certain 
combinations of tokens are replaced with simpler tokens).  In 
the Go codebase, a the `output_filtered` function works as a 
state machine.  The Scala `printLine` function is much more 
declarative.  I would much rather maintain the Scala.

