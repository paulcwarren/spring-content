#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'release' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in signingkey.asc.enc -out signingkey.asc -d
    gpg --fast-import signingkey.asc
fi