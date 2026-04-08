#!/bin/sh
# Maven Wrapper — downloads Maven if not present
MAVEN_VERSION=3.9.6
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
MAVEN_HOME="${HOME}/.m2/wrapper/maven-${MAVEN_VERSION}"

if [ ! -d "${MAVEN_HOME}" ]; then
  echo "Downloading Maven ${MAVEN_VERSION}..."
  mkdir -p "${MAVEN_HOME}"
  curl -sL "${MAVEN_URL}" | tar -xz --strip-components=1 -C "${MAVEN_HOME}"
fi

exec "${MAVEN_HOME}/bin/mvn" "$@"
