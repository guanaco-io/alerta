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

step "Update versions in POM files to $VERSION"
mvn -q -Pall -f $BASEDIR/pom.xml org.codehaus.mojo:versions-maven-plugin:2.2:set -DgenerateBackupPoms=false -DnewVersion=$VERSION