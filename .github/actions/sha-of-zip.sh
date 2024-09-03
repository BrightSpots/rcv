#!/bin/bash
# Unzips the given zip file, then generates a checksum of the zip file that ignores
# the timestamps by extracting it and SHA'ing a file of all SHAs. This is not recursive:
# zips within the zip will not be extracted, and therefore the timestamps of the "inner" zip
# will be a part of the hash.
# Usage: ./sha-of-zip.sh <zipFilepath> <OS: Windows, MacOS, or Linux> <sha version: 1, 256, or 512>
set -e

zipFilepath=$1
os=$2
sha_a=$3

parentPath=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

# Make a temporary directory to extract zip, and a temporary file to hold SHAs
tempDirectory=$(mktemp -d)
tempAllChecksumsFile=$(mktemp)
tempChecksumDebugInfoFile=$(mktemp)
touch $tempAllChecksumsFile
touch $tempChecksumDebugInfoFile

# Extract the zip
unzip -q $zipFilepath -d $tempDirectory 2>/dev/null

# Get a checksum for each file in the zip
cd $tempDirectory
for filename in $(find * -type f | sort); do
  checksum=$($parentPath/../workflows/sha.sh $filename $os $sha_a)
  echo $checksum >> $tempAllChecksumsFile
  echo $filename = $checksum >> $tempChecksumDebugInfoFile
done

# Echo the checksum of the checksums
echo $($parentPath/../workflows/sha.sh $tempAllChecksumsFile $os $sha_a)

# For easier debugging, print the file-by-file hash
echo "##########"
echo "This checksum was created by a SHA-$sha_a of the following file, after removing the filenames and sorting by their SHAs:"
cat $tempChecksumDebugInfoFile | sort
