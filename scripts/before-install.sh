#!/usr/bin/env bash
if [ "$TRAVIS_BRANCH" = 'release' ] && [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
    openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in cd/signingkey.asc.enc -out cd/signingkey.asc -d
    gpg --fast-import cd/signingkey.asc
fi