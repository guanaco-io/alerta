#!/usr/bin/env bash

# stop on error
set -e

# log error and exit script
function fail {
  echo
  echo "ERROR -- $1"
  exit -1
}

function step {
  echo
  echo $1
  echo "---"
}

# check arguments
if [ $# != 1 ]; then
  echo "Usage: $0 <release version>"
  fail "Error: no <release version> specified"
fi

cd $(dirname $0)
BASEDIR=$(pwd)
VERSION=$1

step "Update versions in build.sbt to $VERSION"

sed -E -i.bak 's/(version[[:space:]]*\:\=[[:space:]]*)\"(.*)\"/\1"'${VERSION}'"/g' build.sbt
