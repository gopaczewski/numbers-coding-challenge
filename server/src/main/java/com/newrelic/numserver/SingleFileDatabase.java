package com.newrelic.numserver;

/**
 * TODO
 */
public class SingleFileDatabase implements Database {

    @Override
    public boolean tryInsert(int number) {
        return number % 2 == 0;
    }
}
