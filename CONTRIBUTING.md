# Contributing to Spring Content

Spring Content is released under the Apache 2.0 license. If you would like to contribute
something, or simply want to hack on the code this document should help you get started.

## Code of Conduct
This project adheres to the Contributor Covenant [code of
conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report
unacceptable behavior to the Authors.

## Using GitHub Issues
We use GitHub issues to track bugs and enhancements. If you have a general usage question
please ask it in our [Gitter room](https://gitter.im/spring-content/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge). 

If you are reporting a bug, please help to speed up problem diagnosis by providing as much
information as possible. Ideally, that would include a small sample project that reproduces the
problem.

## Reporting Security Vulnerabilities
If you think you have found a security vulnerability in Spring Content please *DO NOT*
disclose it publicly until we've had a chance to fix it. Please don't report security
vulnerabilities using GitHub issues, instead report it directly to the Authors via email.

## Sign the Contributor License Agreement
Before we accept a non-trivial patch or pull request we will need you to
[sign the Contributor License Agreement](https://cla-assistant.io/paulcwarren/spring-content).
Signing the contributor's agreement does not grant anyone commit rights to the main
repository, but it does mean that we can accept your contributions, and you will get an
author credit if we do.  Active contributors might be asked to join the core team, and
given the ability to merge pull requests.

## Code Conventions and Housekeeping
None of these is essential for a pull request, but they will all help.  They can also be
added after the original pull request but before a merge.

* Use the Spring Framework code format conventions. If you use Eclipse and you follow
  the '`Importing into eclipse`' instructions below you should get project specific
  formatting automatically. You can also import formatter settings using the
  `eclipse-code-formatter.xml` file from the `eclipse` folder. If using IntelliJ IDEA, you
  can use the [Eclipse Code Formatter Plugin](http://plugins.jetbrains.com/plugin/6546)
  to import the same file.
* Make sure all new `.java` files have a simple Javadoc class comment with at least an
  `@author` tag identifying you, and preferably at least a paragraph on what the class is
  for.
* Add the ASF license header comment to all new `.java` files (copy from existing files
  in the project)
* Add yourself as an `@author` to the `.java` files that you modify substantially (more
  than cosmetic changes).
* Add some Javadocs.
* A few unit tests would help a lot as well -- someone has to do it.  Spring Content uses the [ginkgo4j](https://github.com/paulcwarren/ginkgo4j) BDD framework.
* If no-one else is using your branch, please rebase it against the current master (or
  other target branch in the main project).
* When writing a commit message please follow [these conventions](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html),
  if you are fixing an existing issue please add `Fixes gh-XXXX` at the end of the commit
  message (where `XXXX` is the issue number).

== Working with the Code
If you don't have an IDE preference we would recommend that you use
[Spring Tools Suite](https://spring.io/tools/sts) or
[Eclipse](http://eclipse.org) when working with the code. We use the
[M2Eclipse](http://eclipse.org/m2e/) eclipse plugin for maven support. Other IDEs and tools
should also work without issue.

### Building from Source
To build the source you will need to install
[Apache Maven](https://maven.apache.org/run-maven/index.html) v3.2.3 or above and JDK 1.8.

#### Default Build
The project can be built from the root directory using the standard maven command:

```
$ BUILD_TYPE=dev ./mvnw clean install
```

### Importing into Eclipse
You can import the Spring Content code into any Eclipse-based distribution. The easiest
way to setup a new environment is to use the Eclipse Installer with the provided
`.setup` file.

#### Using the Eclipse Installer
Spring Content includes a `.setup` file which can be used with the Eclipse Installer to
provision a new environment. To use the installer:

* Download and run the latest Eclipse Installer from
  [eclipse.org/downloads/](http://www.eclipse.org/downloads/) (under "Get Eclipse").
* Switch to "Advanced Mode" using the drop down menu on the right.
* Select "`Eclipse IDE for Java Developers`" under "`Eclipse.org`" as the product to
  install and click "`next`".
* For the "`Project`" click on "`+`" to add a new setup file. Select "`Github Projects`"
  and browser for `<checkout>/eclipse/spring-content-project.setup` from your locally cloned
  copy of the source code. Click "`OK`" to add the setup file to the list.
* Double-click on "`Spring Content`" from the project list to add it to the list that will
  be provisioned then click "`Next`".
* Click show all variables and make sure that "`Checkout Location`" points to the locally
  cloned source code that you selected earlier. You might also want to pick a different
  install location here.
* Click "`Finish`" to install the software.

Once complete you should find that a local workspace has been provisioned complete with
all required Eclipse plugins. Projects will be grouped into working-sets to make the code
easier to navigate.

#### Manual Installation with M2Eclipse
If you prefer to install Eclipse yourself we recommend that you use the
[M2Eclipse](http://eclipse.org/m2e/) eclipse plugin. If you don't already have m2eclipse
installed it is available from the "Eclipse marketplace".

Spring Content includes project specific source formatting settings, in order to have these
work with m2eclipse, we provide additional Eclipse plugins that you can install:

##### Install the m2eclipse-maveneclipse plugin
* Select "`Help`" -> "`Install New Software`".
* Add `https://dl.bintray.com/philwebb/m2eclipse-maveneclipse` as a site.
* Install "Maven Integration for the maven-eclipse-plugin"

##### Install the Spring Formatter plugin
* Select "`Help`" -> "`Install New Software`".
* Add `https://dl.bintray.com/philwebb/spring-eclipse-code-formatter/` as a site.
* Install "Spring Code Formatter"

NOTE: These plugins are optional. Projects can be imported without the plugins, your code
changes just won't be automatically formatted.

With the requisite eclipse plugins installed you can select
`import existing maven projects` from the `file` menu to import the code. You will
need to import the root `spring-content` pom.

##### Importing into Eclipse without M2Eclipse
If you prefer not to use m2eclipse you can generate eclipse project metadata using the
following command:

```
$ ./mvnw eclipse:eclipse
```

The generated eclipse projects can be imported by selecting `import existing projects`
from the `file` menu.

### Importing into Other IDEs
Maven is well supported by most Java IDEs. Refer to your vendor documentation.

## Cloning the git repository on Windows
Some files in the git repository may exceed the Windows maximum file path (260
characters), depending on where you clone the repository. If you get `Filename too long`
errors, set the `core.longPaths=true` git option:

```
git clone -c core.longPaths=true https://github.com/paulcwarren/spring-content
```
