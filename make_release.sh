#!/usr/bin/env bash

# This script is used to partially automate the release process. You still need
# to update the version number in `pom.xml`.

set -e

script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
cd "$script_dir"

git checkout master
mvn clean
# Make sure all files have licenses
mvn license:update-file-header
# Make sure code is formatted consistently
mvn com.spotify.fmt:fmt-maven-plugin:format
# Make sure code builds
mvn package
# Generate Javadocs
mvn javadoc:javadoc -Prelease
if [ -n "$(git status --porcelain)" ]; then
    echo "Directory not clean"
    exit 1
fi

# Update the Javadocs on the website
git checkout gh-pages
rm -rf apidocs
cp -r target/site/apidocs .
git add apidocs
git commit -m "Update Javadocs."
git push

git checkout master

# To upload the new release to Maven Central, run:
#
# mvn clean deploy -Prelease
#
# (This assumes you have a settings.xml file with the GPG passphrase and a
# Sonatype token.) Once the package is validated, you can publish it using the
# interface on <https://central.sonatype.org/>.
