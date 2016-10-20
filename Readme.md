# Embedded WebDAV file server for a local folder
[![Travis CI build status](https://travis-ci.org/TheMagican/webdav-embedded-server.svg?branch=master)](https://travis-ci.org/TheMagican/webdav-embedded-server/)

## Abstract
This project contains a _very_ basic WebDAV server that will serve the contents of a single local folder. The only parameters for the server are:

- The port, to which the server will bind
- One or more user credentials to authenticate
- The local folder to serve using WebDAV

Note that this server in its current state is **not for productive usage**. Instead it is intended as a quick way to set up a WebDAV server when needed, e.g. for testing your WebDAV client library against a server with a well-known and easy to modify state.

## Usage
This library is available on JCenter.

Maven:
```xml
<dependency>
    <groupId>de.bitinsomnia</groupId>
    <artifactId>webdav-embedded-server</artifactId>
    <version>0.1</version>
    <type>pom</type>
</dependency>
```

Gradle:
```gradle
dependencies {
    compile 'de.bitinsomnia:webdav-embedded-server:0.1'
}
```   

### In other projects
The intended usage for this server is to be used in other projects as an easy to use embedded WebDAV server, especially for automated tests. This server enables you to

1. create a local folder with all your (test) data in it
2. set up the WebDAV server with only some lines of code
3. run your tests against the WebDAV server
4. check the results of the test by directly accessing the local folder

**Example code:**
```java
File rootFolder = Files.createTempDirectory("webdav-test").toFile();
MiltonWebDAVFileServer server = new MiltonWebDAVFileServer(rootFolder);
server.setPort(4711); // optional, defaults to 8080
server.getUserCredentials().put("user", "secret"); // optional, defaults to no authentication
server.start();

// Execute your WebDAV code here

server.stop();

// Asserts on the contents of rootFolder
```

### Standalone
The class ``de.bitinsomnia.webdav.server.MiltonStandaloneServer`` contains a `main` method to start the server from the command line. It accepts several command line arguments:
```
Usage: MiltonStandaloneServer [options] FOLDER_TO_SERVE
  Options:
    -c, --credentials
       Optional credentials to authenticate at the server. Can be given multiple
       times. If none are given, authentication is disabled. Use the form USER:PASSWORD
       Default: []
    -h, --help
       Show help and exit
       Default: false
    -p, --port
       Port for the server
       Default: 8080
```

**Example cmd-line arguments:** `-c user1:secret -c user2:password -p 4711 /path/to/data`

## Logging
This project uses SLF4J / Logback for logging. To reduce the amount of logging (especially when using in standalone mode), place a [`logback.xml`](http://logback.qos.ch/manual/configuration.html) in the classpath.

## Credits
This server is based on:
- [Jetty](https://eclipse.org/jetty/)
- [Milton DAV library](http://milton.io/)
- [JCommander](http://jcommander.org/)

## License
Copyright (C) 2010 the original author or authors.
See the [NOTICE.md](./NOTICE.md) file distributed with this work for additional
information regarding copyright ownership.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
