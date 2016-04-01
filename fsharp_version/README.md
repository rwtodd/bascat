# BasCat F# Version

I converted the Scala version of the program (also in this repo) to
F#, to get a sense of the difference.  This implementaion came out
slightly cleaner-looking to my eyes, but overall is very similar.

The main differences are:

  * Scala has no built-in `unfold`, so the line filtering
    is a custom recursive function.  (Yes, I could have
    just implemented unfold myself).
  * While I made a `case class` in Scala for the Tokens,
    it seemed more natural in F# to just make a tuple.  I could
    have done this in Scala as well, but at the time the class
    seemed more appropriate.

