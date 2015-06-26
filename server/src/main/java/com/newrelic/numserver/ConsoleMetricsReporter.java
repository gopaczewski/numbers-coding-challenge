package com.newrelic.numserver;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Reports application metrics to the console.
 */
public class ConsoleMetricsReporter implements MetricsReporter {

    // todo: DI
    private final ScheduledExecutorService executorService;
    private final Duration reportingFrequency;

    private final AtomicInteger uniqueNumbersDeltaCount = new AtomicInteger(0);
    private final AtomicInteger duplicateNumbersDeltaCount = new AtomicInteger(0);
    private final AtomicInteger totalUniqueNumbersCount = new AtomicInteger(0);

    public ConsoleMetricsReporter(Duration reportingFrequency) {
        this.reportingFrequency = reportingFrequency;
        this.executorService = Executors.newScheduledThreadPool(1);
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
    public void start() {
        executorService.scheduleWithFixedDelay(reportTask, reportingFrequency.toMillis(), reportingFrequency.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        executorService.shutdownNow();
        // one last write of metrics in the caller's thread
        reportTask.run();
    }
}
