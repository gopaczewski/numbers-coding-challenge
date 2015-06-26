package com.newrelic.numserver;

/**
 * A persistent storage for integer numbers.
 */
public interface Database {

    /**
     * Attempts to insert a new number into the database.
     *
     * This method will block and apply back-pressure only when database writes cannot keep up with input.
     *
     * @return true if the number has not yet been seen (inserted), false if the number is a duplicate
     */
    boolean tryInsert(int number) throws InterruptedException;
}
