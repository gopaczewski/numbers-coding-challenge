package com.newrelic.numserver;

/**
 * TODO
 */
public interface Database {

    /**
     * Attempts to insert a new number into the database.
     *
     * @return true if the number has not yet been seen (inserted), false if the number is a duplicate
     */
    boolean tryInsert(int number);
}
