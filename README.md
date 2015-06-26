# Overview

New Relic Coding Challenge

This application is comprised of a server that implements the requirements of the numbers server and client code to test the server.

# Assumptions

## Index to test for duplicate numbers will fit entirely in memory

1,000,000,000 possible values * 4 bytes = 4 GB (+ some overhead)
Assumption -> all index will fit into memory on a single server

# Server

The server requires JDK 8 and Maven to be installed both to build and run.

## Build and run tests

$ cd server/ && mvn clean test

## Run

To ensure the exec-maven plugin will run in a Java 8 VM you must set your JAVA_HOME appropriately, for e.g. insert the following into your ~/.mavenrc:

export JAVA_HOME=/Library/Java/JavaVirtualMachines/{jdk-version}/Contents/Home

Then

$ cd server/ && mvn compile exec:java
