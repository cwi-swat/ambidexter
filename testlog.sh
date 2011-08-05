#!/bin/bash

# $1 grammar file
# $2 .. $9 optional cmd line arguments

LOGFILE="$1.$2$3$4$5$6$7$8$9.log"


rm $LOGFILE &> /dev/null

date > $LOGFILE

/usr/bin/time --format="time: %e\nuser: %U\nkernel: %S" java -Xmx1600m -cp bin:lib nl.cwi.sen1.AmbiDexter.Main $2 $3 $4 $5 $6 $7 $8 $9 $1 >> $LOGFILE 2>&1 &

cur="0"
max="0"
pid="$!"

while [ -n "$cur" ]
do
  sleep 0.1
  if test $cur -gt $max
  then
    #echo `ps -C $2 -o etime=`" $cur" >> "$1.time" 
    max=$cur
  fi
  cur=$(ps -p $pid -o rss=)
  #echo "$cur -gt $max"
done

echo "maxmem: ${max## }" >> $LOGFILE

