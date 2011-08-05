#!/bin/sh

# test derivation generation with and without NU test
# $1 file
# $2 filter file
# $3 k

# first output all ambiguous sentential forms of filtered grammar
java -Xss16m -Xmx1500m -cp bin:lib/aterm-java.jar:lib/shared-objects.jar:lib/jjtraveler.jar nl.cwi.sen1.AmbiDexter.Main -q -pg -k $3 -ogs -rf $2 $1

if [ "$?" -eq "0" ]
then
# then output all ambiguous sentential forms of original grammar, only with trees filtered
java -Xss16m -Xmx1500m -cp bin:lib/aterm-java.jar:lib/shared-objects.jar:lib/jjtraveler.jar nl.cwi.sen1.AmbiDexter.Main -q -pg -k $3 -ogs $1

if [ "$?" -eq "0" ]
then
orgamb="$1.null.$3.amb"
filamb="$1.$2.$3.amb"

sort -u $orgamb > $orgamb.sort
sort -u $filamb > $filamb.sort

diff $orgamb.sort $filamb.sort

if [ "$?" -eq "0" ]
then
echo "No differences found ?:^)"
else
echo "diff -y $orgamb.sort $filamb.sort"
exit 1
fi

fi

fi

echo