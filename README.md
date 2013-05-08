victims-lib-java [![Build Status](https://travis-ci.org/victims/victims-lib-java.png)](https://travis-ci.org/victims/victims-lib-java)
================

A java library providing fingerprinting of java binaries and REST-API client classes for the Victims Project.

## Building from source
### Requrements
* java 1.6
* maven3

### Generating artifacts
Once you have cloned the repository, you can genereate the _victims-lib_ artifactions using any of the following commands.

```sh
mvn clean package
```

### Releases
If you are preparing for release and want to generate the source and javadoc jars along with the main jar, you need to set the _release_ option to _true_.
```sh
mvn clean package -Drelease=true
```

By default the artifacts are not signed. If you require gpg signed artifacts, then set the _sign_ option to _true_.
```sh
mvn clean package -Drelease=true -Dsign=true
```

### Generating reports and site contents
The following command generate default sute contents and reports.
```sh
mvn site
```

If you require _findbug_ reporting, make sure to run a goal that will require comilation. eg:
```sh
mvn clean test site
```

## Running Tests
To execute all tests:
```sh
mvn test
```
