victims-lib-java [![Build Status](https://travis-ci.org/victims/victims-lib-java.png)](https://travis-ci.org/victims/victims-lib-java)
================
A java library providing fingerprinting and service interaction for the Victims Project.

### GPG Keys
Download: [pgp.mit.edu](http://pgp.mit.edu:11371/pks/lookup?search=0xEEE72232&op=index)

Fingerprint:
```
47DB 2877 89B2 1722 B6D9 5DDE 5326 8101 3701 7186
```
### Artifacts
* *Snapshots*: https://oss.sonatype.org/content/repositories/snapshots/com/redhat/victims/victims-lib/
* *Releases*: http://central.maven.org/maven2/com/redhat/victims/victims-lib/

## Using in your project
### Maven
Update your _pom.xml_ dependencies.
```xml
<dependency>
  <groupId>com.redhat.victims</groupId>
  <artifactId>victims-lib</artifactId>
  <version>1.1</version>
</dependency>
```
### Gradle
Update your _build.gradle_ dependencies.
```groovy
dependencies {
    compile group: 'com.redhat.victims', name: 'victims-lib', version: '1.1'
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
## Using Service Mocking
You might want to use a dummy sservice to test your implementation. This is available using the package _com.redhat.victims.mock_. This is avaiable in the test jar. You can use this by adding the following dependency.
```xml
<dependency>
  <groupId>com.redhat.victims</groupId>
  <artifactId>victims-lib</artifactId>
  <type>test-jar</type>
  <version>1.1</version>
  <scope>test</scope>
</dependency>
```
To write a test case using this, you will need to provide 2 files containing the expected json responses. If _null_ is used, the mock server will respond with "[]". You can use this in a _junit_ test case as shown below.
```java
  @BeforeClass
  public static void setUp() throws IOException, VictimsException {
  	File updateResponse = new File(TEST_RESPONSE);
  	MockEnvironment.setUp(updateResponse, null);
  }
  
  @AfterClass
  public static void tearDown() {
  	MockEnvironment.tearDown();
  }
```
Beyond starting a mock service and cleaning up the cache after you, this will set the following properties for you:
* _victims.service.uri_="http://localhost:1337/"
* _victims.db.force_="true"
* _victims.cache_="victims.test.cache"

## Releases
This is an abridged version of the guide available at https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide
#### SNAPSHOT Release
```sh
mvn clean deploy
```
#### Staging
```sh
mvn clean deploy
mvn release:clean
mvn release:prepare
mvn release:perform
```
#### Promoting to central
1. Login at https://oss.sonatype.org/
2. [Go to staging repositories](https://oss.sonatype.org/index.html#stagingRepositories)
3. Select the staging repository
4. Click on the _Close_ button
5. Once closed, click on the _Release_ button.
