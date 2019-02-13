#!/bin/bash -eux

BUILD_TYPE=dev mvn -P docs -pl "spring-content-$1" generate-resources
