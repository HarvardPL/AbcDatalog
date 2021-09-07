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
* Maven (v3.6.3 is known to work); necessary only for compilation

# Usage

## Pre-built JAR

A pre-built JAR can be found on the
[Releases](https://github.com/HarvardPL/AbcDatalog/releases) section of GitHub.

## Compilation

AbcDatalog can be compiled using Maven. From this directory, run `mvn package`
to build the archive `target/AbcDatalog-[X.Y.Z]-jar-with-dependencies.jar`
(where `[X.Y.Z]` is the version number).

## Launching the GUI

The AbcDatalog JAR is executable; launch the AbcDatalog graphical user
interface with the command:

```
java -jar [AbcDatalog.jar]
```

where `[AbcDatalog.jar]` is the name of the JAR on your system.

## Programming with the API

Please see the [AbcDatalog website](https://abcdatalog.seas.harvard.edu/) for
information on how to interface with AbcDatalog from your Java program.

# People

The primary contributors to AbcDatalog are:

	* Aaron Bembenek
	* Stephen Chong
	* Marco Gaboardi

Please email bembenek@g.harvard.edu with questions, comments, and bug reports.

Thanks to João Gonçalves for helping transition AbcDatalog to GitHub!

# Acknowledgements

AbcDatalog has been developed as part of the Privacy Tools for Sharing Research
Data project at Harvard University and is supported by the National Science
Foundation under Grant Nos. 1237235 and 1054172.
