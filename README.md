# Introduction

AbcDatalog is an open-source, Java-based implementation of the logic programming
language Datalog. It provides ready-to-use implementations of common Datalog
evaluation algorithms, including efficient multi-threaded versions of bottom-up
and magic set transformation techniques. Additionally, it is designed to be
easily extensible with new evaluation engines and new language features. We hope
that it proves to be useful for both research and pedagogy.

AbcDatalog is released under a BSD License, a copy of which is included in this
directory.


# Requirements

AbcDatalog must be compiled with Java 8. In addition, the included build script
requires Ant 1.9.0+.


# Compilation

AbcDatalog can be compiled using the included build script. From this directory,
run `ant compile`. An executable JAR that launches the graphical user interface
can be built by running `ant build-gui`.


# People

The primary contributors to AbcDatalog are:

	* Aaron Bembenek
	* Stephen Chong
	* Marco Gaboardi

Please email bembenek@g.harvard.edu with questions, comments, and bug reports.

# Acknowledgements

AbcDatalog has been developed as part of the Privacy Tools for Sharing Research Data project at Harvard University and is supported by the National Science Foundation under Grant Nos. 1237235 and 1054172.
