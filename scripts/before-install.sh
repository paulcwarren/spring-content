#!/usr/bin/env bash
echo Maven profile: ${MAVEN_PROFILE}
echo Build type: ${BUILD_TYPE}
echo Travis tag: ${TRAVIS_TAG}

#if [ -n "$TRAVIS_TAG" ]; then
#    export BUILD_TYPE=release
#else
#    export BUILD_TYPE=snapshot
#fi