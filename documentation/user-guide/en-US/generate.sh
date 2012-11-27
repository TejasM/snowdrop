#!/bin/bash

# Require BASH 3 or newer

REQUIRED_BASH_VERSION=3.0.0

if [[ $BASH_VERSION < $REQUIRED_BASH_VERSION ]]; then
  echo "You must use Bash version 3 or newer to run this script"
  exit
fi
cd en-US
# Canonicalise the source dir, allow this script to be called anywhere
DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

# DEFINE

TARGET=../target/guides

echo "** Building tutorial"

echo "**** Cleaning $TARGET"
rm -rf $TARGET
mkdir -p $TARGET

output_format=xml
dir=$TARGET/$output_format
mkdir -p $dir
echo "**** Copying shared resources to $dir"
cp -r images $dir
cp -r icons $dir

for file in *.asc
do
   output_filename=${file//.asc/.$output_format}
   echo "**** Processing $file > ${output_filename}"
   asciidoc -a numbered -a toc -a toclevels=4 -a pygments -a toc-placement=manual -b docbook -d book -o ${output_filename} $file
done
