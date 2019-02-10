@echo off
mvn clean gpg:sign deploy -DaltDeploymentRepository=ossrh-staging::default::https://oss.sonatype.org/service/local/staging/deploy/maven2