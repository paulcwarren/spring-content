#!/usr/bin/env bash
echo ${MAVEN_PROFILE}

openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

if [ -n "$TRAVIS_TAG" ]; then
    export MAVEN_PROFILE=release
else
    export MAVEN_PROFILE=master
fi