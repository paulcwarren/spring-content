#!/usr/bin/env bash
if [ -n "$TRAVIS_TAG" ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
    gpg --fast-import codesigning.asc
fi