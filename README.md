victims-lib-java [![Build Status](https://travis-ci.org/victims/victims-lib-java.png)](https://travis-ci.org/victims/victims-lib-java)
================
A java library providing fingerprinting and service interaction for the Victims Project.

### GPG Keys
Download: [pgp.mit.edu](http://pgp.mit.edu:11371/pks/lookup?search=0xEEE72232&op=index)

Fingerprint:
```
47DB 2877 89B2 1722 B6D9 5DDE 5326 8101 3701 7186
```
## Using in your project
### Maven
Update your _pom.xml_ dependencies.
```xml
<dependency>
  <groupId>com.redhat.victims</groupId>
  <artifactId>victims-lib</artifactId>
  <version>1.0</version>
</dependency>
```
### Gradle
Update your _build.gradle_ dependencies.
```groovy
dependencies {
    compile group: 'com.redhat.victims', name: 'victims-lib', version: '1.0'
}
```
## Building from source
### Requrements
* java 1.6
* maven3

### Generating artifacts
Once you have cloned the repository, you can genereate the _victims-lib_ artifactions using any of the following commands.
```sh
mvn clean package
```
By default the artifacts are not signed. If you require gpg signed artifacts,
```sh
mvn clean package -Drelease=true gpg:sign -Dgpg.keyname=EEE72232
```
## Running Tests
To execute all tests:
```sh
mvn test
```
To run only offlinetests:
```sh
mvn test -Dtest=OfflineTests
```
