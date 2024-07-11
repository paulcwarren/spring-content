[![Build](https://github.com/paulcwarren/spring-content/workflows/Branch%20Build/badge.svg?branch=1.2.x)](https://github.com/paulcwarren/spring-content/actions?query=workflow%3A%22Branch+Build%22)
[![codecov](https://codecov.io/gh/paulcwarren/spring-content/branch/1.1.x/graph/badge.svg?token=Q7uPi3zXTB)](https://codecov.io/gh/paulcwarren/spring-content)
[![Join the chat at https://gitter.im/spring-content/Lobby](https://badges.gitter.im/spring-content/Lobby.svg)](https://gitter.im/spring-content/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# Spring Content

Cloud-Native Content Services for Spring.

Spring Content provides modules for managing content in JPA, MongoDB's GridFS, S3 or Filesystem storage.  When combined with Spring Data/REST it allows content to be associated with Spring Data Entities.  The Solr module provides an integration with Apache Solr for fulltext indexing and search capabilities.  The Renditions module provides a pluggable renditions framework and several out-of-the-box renderers that can render stored content in several different formats.   

## Getting help
Having trouble with Spring Content? We'd like to help!

* Check the [reference documentation](https://paulcwarren.github.io/spring-content/).
* Learn the Spring basics -- Spring Content builds on many other Spring projects, check
  the [spring.io](https://spring.io) web-site for a wealth of reference documentation. If
  you are just starting out with Spring, try one of the [spring.io guides](https://spring.io/guides).
* If you are upgrading, read the [release notes](https://github.com/paulcwarren/spring-content/releases).
  for upgrade instructions and "new and noteworthy" features.
* Ask a question - we monitor our [Gitter room](https://gitter.im/spring-content/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) for questions.
* Report bugs with Spring Content at [github.com/paulcwarren/spring-content/issues](https://github.com/paulcwarren/spring-content/issues).

## Reporting Issues
Spring Content uses GitHub's integrated issue tracking system to record bugs and feature
requests. If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please [search the issue tracker](https://github.com/paulcwarren/spring-content/issues)
  to see if someone has already reported the problem.
* If the issue doesn't already exist, [create a new issue](https://github.com/paulcwarren/spring-content/issues/new).
* Please provide as much information as possible with the issue report, we like to know
  the version of Spring Content that you are using, as well as your Operating System and
  JVM version.
* If you need to paste code, or include a stack trace use Markdown `` ``` `` escapes
  before and after your text.
* If possible try to create a test-case or project that replicates the issue.

## Building from Source
You don't need to build from source to use Spring Content (binaries in
https://repo.maven.apache.org/maven2/), but if you want to try out the latest and
greatest, Spring Content can be easily built with the
[maven wrapper](https://github.com/takari/maven-wrapper). You also need JDK 1.8.

```
$ AWS_REGION=us-west-1 ./mvnw [-P tests] clean install
```

where the optional `-P tests` invokes the maven build with integration (*IT.java) tests as well as units (default).

If you want to build with the regular `mvn` command, you will need
[Maven v3.2.1 or above](https://maven.apache.org/run-maven/index.html).

_Also see [CONTRIBUTING.md](CONTRIBUTING.md) if you wish to submit pull requests,
and in particular please fill out the [Contributor's License Agreement](https://cla-assistant.io/paulcwarren/spring-content) before your first change, however trivial._

#### Building reference documentation

The reference documentation can be included in the build by specifying the `docs` profile.

```
$ AWS_REGION=us-west-1 ./mvnw -P docs clean install 
```

TIP: The generated documentation is available from `spring-content/target/generated-docs/refs/dev/`

## Guides
The [https://paulcwarren.github.io/spring-content/](https://paulcwarren.github.io/spring-content/) site contains several guides that show how to use Spring
Content step-by-step:

* [Getting Started with Spring Content](https://paulcwarren.github.io/spring-content/spring-content-fs-docs/) is a
  very basic guide that shows you how to create a simple application, backed by a filesystem-based content store.
* [Getting Started with Spring Content REST](https://paulcwarren.github.io/spring-content/spring-content-rest-docs/) is a guide that shows you how to create a REST Content Service backed by a filesystem-based content store
  can be configured.

## License
Spring Content is Open Source software released under the
[Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgements

* Spring Content uses YourKit.  YourKit supports open source projects with its full-featured Java Profiler.
  YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
  and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
  innovative and intelligent tools for profiling Java and .NET applications.
  
  [![](https://www.yourkit.com/images/yklogo.png)](https://www.yourkit.com/java/profiler/index.jsp) 
