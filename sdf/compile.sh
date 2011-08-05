#!/bin/sh


if [ -n "$2" ]; then
OUT=$2.norm.impl.pt
else
OUT=$1.norm.impl.pt
fi

sdf2table -c -n -m $1 -o temp.norm.pt
implodePT -i temp.norm.pt -o $OUT
