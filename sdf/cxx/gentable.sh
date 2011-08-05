#!/bin/sh


if [ -n "$2" ]; then
OUT=$2.tbl
else
OUT=$1.tbl
fi

sdf2table -c -m $1 -o $OUT

