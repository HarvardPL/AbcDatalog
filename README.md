# Introduction

AbcDatalog is an open-source Java implementation of Datalog, a logic
programming language. It provides ready-to-use implementations of common
Datalog evaluation algorithms, and is designed to be easily extensible with new
evaluation engines and new language features. We hope that it proves to be
useful for both research and pedagogy.

For more information, please see the
[AbcDatalog website](https://abcdatalog.seas.harvard.edu/).

# Licensing

AbcDatalog is released under a BSD License, a copy of which is included in this
directory.

AbcDatalog uses third party libraries; a list of them and their associated
licenses can be found in the [`third-party-licenses/`](third-party-licenses/)
subdirectory.

# Requirements

* Java 8+
* Maven (v3.6.3 is known to work); not necessary for running the pre-built JAR

# Setup

## Pre-Built JAR

A pre-built JAR can be found on the
[Releases](https://github.com/HarvardPL/AbcDatalog/releases) section of the GitHub repository.

## Maven Central

AbcDatalog is released on [Maven Central](https://central.sonatype.com/artifact/io.github.harvardpl/AbcDatalog)
and can be easily added as a library to another Maven project by including a
snippet like this in that project's `pom.xml` file:

```
<dependency>
    <groupId>io.github.harvardpl</groupId>
    <artifactId>AbcDatalog</artifactId>
    <version>[X.Y.Z]</version>
</dependency>
```

Replace `[X.Y.Z]` with the most recent AbcDatalog version.

## Compilation

If you desire, you can compile the source code into a JAR using Maven. From
the project root directory, run `mvn package` to build the archive
`target/AbcDatalog-[X.Y.Z]-jar-with-dependencies.jar` (where `[X.Y.Z]` is the
version number).

# Usage

Please see the [AbcDatalog website](https://abcdatalog.seas.harvard.edu/) for
information on how to use the AbcDatalog graphical user interface and how to
interface with AbcDatalog from Java programs.

# Contributing

Contributions are encouraged!
In the past, students have contributed some great improvements to the UI.
If you are interested in contributing, open a [GitHub issue](https://github.com/HarvardPL/AbcDatalog/issues) discussing the improvements you would like to make.

# People

The primary contributors to AbcDatalog are:

* Aaron Bembenek
* Stephen Chong
* Marco Gaboardi

Question, comment, bug report? Please raise a [GitHub issue](https://github.com/HarvardPL/AbcDatalog/issues).

Thanks to João Gonçalves for helping transition AbcDatalog to GitHub!

# Acknowledgements

AbcDatalog has been developed as part of the Privacy Tools for Sharing Research
Data project at Harvard University and is supported by the National Science
Foundation under Grant Nos. 1237235 and 1054172.
