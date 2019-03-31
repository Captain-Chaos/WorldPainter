@echo off
mvn clean gpg:sign deploy -DaltDeploymentRepository=ossrh-snapshots::default::https://oss.sonatype.org/content/repositories/snapshots