#!/bin/bash

# Require BASH 3 or newer

REQUIRED_BASH_VERSION=3.0.0

if [[ $BASH_VERSION < $REQUIRED_BASH_VERSION ]]; then
  echo "You must use Bash version 3 or newer to run this script"
  exit
fi

# Canonicalise the source dir, allow this script to be called anywhere
DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

# DEFINE

TARGET=target/guides
MASTER=Snowdrop_User_Guide.asc

OUTPUT_FORMATS=("pdf")
OUTPUT_CMDS=("a2x --dblatex-opts \"-P latex.output.revhistory=0\" -D \$dir \$MASTER")

echo "** Building tutorial"

echo "**** Cleaning $TARGET"
rm -rf $TARGET
mkdir -p $TARGET

output_format=html
dir=$TARGET/$output_format
mkdir -p $dir
echo "**** Copying shared resources to $dir"
cp -r images $dir
cp -r icons $dir

for file in *.asc
do
   output_filename=$dir/${file//.asc/.$output_format}
   echo "**** Processing $file > ${output_filename}"
   asciidoc -a numbered -a data-uri -a icons -a toc -a toclevels=4 -o ${output_filename} $file
done

#for ((i=0; i < ${#OUTPUT_FORMATS[@]}; i++))
#do
#   output_format=${OUTPUT_FORMATS[i]}
#   dir=$TARGET/$output_format
#   output_filename=$dir/${file//.asc/.$output_format}
#   mkdir -p $dir
#   echo "**** Copying shared resources to $dir"
#   cp -r images $dir
#   cp -r icons $dir
#   echo "**** Processing $file > ${output_filename}"
#   eval ${OUTPUT_CMDS[i]}
#done
