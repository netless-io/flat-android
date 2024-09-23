#!/bin/sh

mkdir $HOME/flat
# decrpyt keystore
gpg --quiet --batch --yes --decrypt --passphrase="$KEYSTORE_SECRET_PASSPHRASE" \
--output $GITHUB_WORKSPACE/flat.keystore $GITHUB_WORKSPACE/.github/flat.keystore.gpg
# decrpyt gradle properties
gpg --quiet --batch --yes --decrypt --passphrase="$GRADLE_SECRET_PASSPHRASE" \
--output $HOME/flat/ci-gradle.properties $GITHUB_WORKSPACE/.github/ci-gradle.properties.gpg
# decrpyt google-services.json
gpg --quiet --batch --yes --decrypt --passphrase="$GRADLE_SECRET_PASSPHRASE" \
--output $GITHUB_WORKSPACE/app/google-services.json $GITHUB_WORKSPACE/.github/google-services.json.gpg

mkdir -p ~/.gradle
cp $HOME/flat/ci-gradle.properties ~/.gradle/gradle.properties
