#!/usr/bin/env bash

# This script is used to partially automate the release process. You still need
# to update the version number in `pom.xml`.

set -e

script_dir=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$script_dir"

git checkout master
# Make sure code builds
mvn package
# Make sure all files have licenses
mvn license:update-file-header
# Make sure code is formatted consistently
mvn com.spotify.fmt:fmt-maven-plugin:format
# Generate Javadocs
mvn javadoc:javadoc
git stash

# Update the Javadocs on the website
git checkout gh-pages
rm -rf apidocs
cp -r target/site/apidocs .
git add apidocs
git commit -m "Update Javadocs."
git push

git checkout master
git stash pop
