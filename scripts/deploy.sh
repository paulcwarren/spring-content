if [ ! -z "$TRAVIS_TAG" ]
then
    export BUILD_TYPE=release/$TRAVIS_TAG
    mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=$TRAVIS_TAG
fi

echo BUILD_TYPE: $BUILD_TYPE

echo "Importing GPG keys"
openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

mvn -B -U -P ci,docs deploy scm-publish:publish-scm -DskipTests=true --settings settings.xml
