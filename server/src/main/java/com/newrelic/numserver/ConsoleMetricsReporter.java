package com.newrelic.numserver;

import com.google.common.util.concurrent.AbstractScheduledService;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reports application metrics to the console.
 */
public class ConsoleMetricsReporter extends AbstractScheduledService implements MetricsReporter {

    private final Duration reportingFrequency;

    private final AtomicInteger uniqueNumbersDeltaCount = new AtomicInteger(0);
    private final AtomicInteger duplicateNumbersDeltaCount = new AtomicInteger(0);
    private final AtomicInteger totalUniqueNumbersCount = new AtomicInteger(0);

    public ConsoleMetricsReporter(Duration reportingFrequency) {
        this.reportingFrequency = reportingFrequency;
    }

    @Override
    protected void runOneIteration() throws Exception {
        int uniqueDelta = uniqueNumbersDeltaCount.getAndSet(0);
        int duplicatesDelta = duplicateNumbersDeltaCount.getAndSet(0);
        int totalUnique = totalUniqueNumbersCount.addAndGet(uniqueDelta);
        System.out.println(String.format("Received %d unique numbers, %d duplicates. Unique total: %d",
                uniqueDelta, duplicatesDelta, totalUnique));
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, reportingFrequency.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void recordInsert(boolean isNewNumber) {
        if (isNewNumber) {
            uniqueNumbersDeltaCount.incrementAndGet();
        } else {
            duplicateNumbersDeltaCount.incrementAndGet();
        }
    }

    private final Runnable reportTask = () -> {
        int uniqueDelta = uniqueNumbersDeltaCount.getAndSet(0);
        int duplicatesDelta = duplicateNumbersDeltaCount.getAndSet(0);
        int totalUnique = totalUniqueNumbersCount.addAndGet(uniqueDelta);
        System.out.println(String.format("Received %d unique numbers, %d duplicates. Unique total: %d",
                uniqueDelta, duplicatesDelta, totalUnique));
    };

    @Override
    protected void shutDown() throws Exception {
        // one last write of metrics in the caller's thread
        reportTask.run();
    }
}
