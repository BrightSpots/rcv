#!/bin/bash
# Cross-platform SHA generation
# Usage: ./sha.sh <filename> <OS: Windows, MacOS, or Linux> <sha version: 1, 256, or 512>

set -e

filename=$1
os=$2
a=$3


if [ $os == 'Windows' ]; then
    echo $(certutil -hashfile $filename SHA$a | sed -n 2p)
elif [ $os == 'Linux' ]; then
    echo $(sha${a}sum $filename | cut -f1 -d" ")
else
    echo $(shasum -a $a $filename | cut -f1 -d" ")
fi
