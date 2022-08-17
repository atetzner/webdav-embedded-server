# Embedded WebDAV file server for a local folder

## Abstract
This project contains a _very_ basic WebDAV server that will serve the contents of a single local folder. The only parameters for the server are:

- The port, to which the server will bind
- One or more user credentials to authenticate
- The local folder to serve using WebDAV

Note that this server in its current state is **not for productive usage**. Instead it is intended as a quick way to set up a WebDAV server when needed, e.g. for testing your WebDAV client library against a server with a well-known and easy to modify state.

## Usage
This library is available on [GitHub Packages](https://github.com/users/atetzner/packages?repo_name=webdav-embedded-server); therefore you need to add `https://maven.pkg.github.com/atetzner/webdav-embedded-server` as an additional Gradle/Maven repository (see [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package) for more information). 

Maven:
```xml
<dependency>
    <groupId>de.bitinsomnia</groupId>
    <artifactId>webdav-embedded-server</artifactId>
    <version>0.2.0</version>
    <type>pom</type>
</dependency>
```

Gradle:
```gradle
dependencies {
    compile 'de.bitinsomnia:webdav-embedded-server:0.2.0'
}
```

For more complete examples, see here: [Gradle](https://github.com/atetzner/webdav-embedded-server/wiki/How-To-use-with-Gradle), [Maven](https://github.com/atetzner/webdav-embedded-server/wiki/How-To-use-with-Maven)

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

Further information how to use the standalone server with the "fat-jar" build of `webdav-embedded-server` can be found in the [Wiki](https://github.com/TheMagican/webdav-embedded-server/wiki#how-to-use-as-standalone-program).

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
