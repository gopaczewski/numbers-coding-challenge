package com.newrelic.numserver;

/**
 * A metrics reporter caches counters for key application metrics.  An instance should be ready to start incrementing
 * counters upon construction.
 *
 * Implementations of this interface must be thread-safe.
 */
public interface MetricsReporter {

    /**
     * Start reporting metrics.
     *
     * @throws IllegalStateException if invoked more than once, a new instance must be created instead
     */
    void start();

    /**
     * Stop reporting metrics and close any associated resources.
     */
    void shutdown();

    /**
     * Increment counters for an insert attempt.
     *
     * @param isNewNumber true to indicate a new number was inserted, false for a duplicate
     */
    void recordInsert(boolean isNewNumber);
}
