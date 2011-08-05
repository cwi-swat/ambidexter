#!/bin/sh

# $1 file
# $2 .. $9 optional cmd line arguments

java -Xss16m -Xmx1500m -cp bin:lib/aterm-java.jar:lib/shared-objects.jar:lib/jjtraveler.jar nl.cwi.sen1.AmbiDexter.Main $2 $3 $4 $5 $6 $7 $8 $9 $1
