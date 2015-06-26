# Overview

This application is comprised of a server that implements the requirements of the numbers server and client code to test the server.

# Server

The server requires JDK 8 and Maven to be installed both to build and run.

## Build and run tests

$ cd server/ && mvn clean test

## Run

To ensure the exec-maven plugin will run in a Java 8 VM you must set your JAVA_HOME appropriately.
For e.g. on OSX insert the following into your ~/.mavenrc:

export JAVA_HOME=/Library/Java/JavaVirtualMachines/{jdk-version}/Contents/Home

Then

$ cd server/ && mvn compile exec:exec

# Client

The client is a simple load driver that spews random integers at the server.

## Run

mvn compile exec:java
