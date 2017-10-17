#!/usr/bin/env bash
echo ${MAVEN_PROFILE}
echo ${BUILD_TYPE}

openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

if [ -n "$TRAVIS_TAG" ]; then
    export BUILD_TYPE=release
else
    export BUILD_TYPE=snapshot
fi