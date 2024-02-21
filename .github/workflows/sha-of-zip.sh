#!/bin/bash
# Unzips the given zip file, then generates a checksum of the zip file that ignores
# the timestamps by extracting it and SHA'ing a file of all SHAs. This is not recursive:-
# zips within the zip will not be extracted, and therefore the timestamps of the "inner" zip
# will be a part of the hash.
# Usage: ./sha-of-zip.sh <zipFilepath> <OS: Windows, MacOS, or Linux> <sha version: 1, 256, or 512>
set -e

zipFilepath=$1
os=$2
a=$3

echo parentpath
parentPath=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

# Make a temporary directory to extract zip, and a temporary file to hold SHAs
echo temp1
tempDirectory=$(mktemp -d)
echo temp2
tempAllChecksumsFile=$(mktemp)
echo temp3
touch $tempAllChecksumsFile

# Extract the zip
echo unzipy
unzip -q $zipFilepath -d $tempDirectory 2>/dev/null

# Get a checksum for each file in the zip
echo tempcd
cd $tempDirectory
echo foreach
for filename in $(find * -type f | sort); do
  echo hcheck
  checksum=$($parentPath/sha.sh $filename $os $a)
  echo sum
  echo $checksum >> $tempAllChecksumsFile
done

# Echo the checksum of the checksums
echo sumitall
echo $($parentPath/sha.sh $tempAllChecksumsFile $os $a)
