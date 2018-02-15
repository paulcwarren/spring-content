if [ ! -z "$TRAVIS_TAG" ]
then
    echo "On a tag -> set pom.xml <version> to $TRAVIS_TAG and BUILD_TYPE to release"
    export BUILD_TYPE=release
    mvn org.codehaus.mojo:versions-maven-plugin:2.1:set -DnewVersion=$TRAVIS_TAG
    #1>/dev/null 2>/dev/null
else
    echo "Not on a tag -> keep snapshot version in pom.xml"
fi

echo "Importing GPG keys"
openssl aes-256-cbc -K $encrypted_2d46c2ddc73e_key -iv $encrypted_2d46c2ddc73e_iv -in codesigning.asc.enc -out codesigning.asc -d
gpg --fast-import codesigning.asc

mvn -B -U -P ci deploy --settings settings.xml -DskipTests=true scm-publish:publish-scm
