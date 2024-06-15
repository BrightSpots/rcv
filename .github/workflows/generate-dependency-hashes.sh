#!/bin/bash
# Generates a CSV of checksums for all Maven dependencies in the global cache
# including their actual SHA 256, and where to verify that online.
# Usage: ./generate-dependency-hashes.sh <OS: Windows, MacOS, or Linux>
set -e

os=$1

parentPath=$( cd "$(dirname "${BASH_SOURCE[0]}")" ; pwd -P )

echo "Filename, SHA-1 Checksum, SHA-256 Checksum, Maven Dependency URL, Direct URL to SHA-1, Direct URL to SHA-256"
cd ~/.gradle/caches/modules-2/files-2.1
for filename in $(find * -type f); do
    # filename is of format, with dot-separated org:
    # <org>/<dependency-name>/<version>/<sha-1>/<dependency-name>-<version>.<ext>
    # friendly URL is of format, with dot-separated org:
    # https://mvnrepository.com/artifact/<org>/<dependency-name>/<version>
    # direct-to-SHA URL is of format, with slash-separated org:
    # https://repo1.maven.org/maven2/<org>/<dependency-name>/<version>/<dependency-name>-<version>.<ext>.sha256
    dotSeparatedOrg=$(echo $filename | cut -f1 -d/)
    dependencyName=$(echo $filename | cut -f2 -d/)
    version=$(echo $filename | cut -f3 -d/)
    ext=${filename#*.}
    slashSeparatedOrg=$(echo $dotSeparatedOrg | tr "." "/")
    friendlyUrl="https://mvnrepository.com/artifact/$dotSeparatedOrg/$dependencyName/$version"
    directUrl="https://repo1.maven.org/maven2/$slashSeparatedOrg/$dependencyName/$version/$dependencyName-$version.$ext"
    directUrlToSha1="$directUrl.sha1"
    directUrlToSha256="$directUrl.sha256"
    sha1=$($parentPath/sha.sh $filename $os 1)
    sha256=$($parentPath/sha.sh $filename $os 256)
    echo "$filename,$sha1,$sha256,$friendlyUrl,$directUrlToSha1,$directUrlToSha256"
done
