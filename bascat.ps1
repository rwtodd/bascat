# Just an example script--the one I actually use to run this beast when on Windows. It won't
# work for you without modification.
$BASE="C:\Users\richa\jars"
java -cp "$BASE\shared\scala-library-2.12.7.jar;$BASE\bascat\bascat_2.12-1.0.jar" org.rwtodd.bascat.BasCat @ARGS

