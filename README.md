# Overview

New Relic Coding Challenge

This application is comprised of a server that implements the requirements of the numbers server and client code to test the server.

# Server

The server requires JDK 8 and Maven to be installed both to build and run.

## Build

$ cd server/ && mvn clean install

## Run

To ensure the exec-maven plugin will run in a Java 8 VM you must set your JAVA_HOME appropriately, for e.g. insert the following into your ~/.mavenrc:

export JAVA_HOME=/Library/Java/JavaVirtualMachines/{jdk-version}/Contents/Home

Then

$ cd server/ && mvn exec:java -Dexec.mainClass="com.newrelic.numserver.Server"
