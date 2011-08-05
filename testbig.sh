#!/bin/sh

# $1 .. $9  first arguments to PAN

FILES="grammars/grammar1*.norm.y"
for f in $FILES
do
echo $f
./testlog.sh $f $1 $2 $3 $4 $5 $6 $7 $8 $9
done
