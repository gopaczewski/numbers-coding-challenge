package com.newrelic.numclient;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

/**
 * Test load driver for numbers server.
 */
public class NumbersLoadDriver {

    private static final int NUM_THREADS = 5;
    private static final ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);

    public static void main(String args[]) {
        if (args.length < 2) {
            System.err.println("Usage: java NumbersLoadDriver <target-host> <run-time-in-seconds>");
            System.exit(1);
        }

        final String host = args[0];
        final int runForMillis = Integer.parseInt(args[1]) * 1000;

        Runnable r = () -> {
            try (Socket s = new Socket(host, 4000);
                 BufferedOutputStream bos = new BufferedOutputStream(s.getOutputStream());
                 PrintWriter writer = new PrintWriter(bos, true)) {
                Random random = new Random();
                while (! Thread.currentThread().isInterrupted()) {
                    String line = String.format("%09d\n", random.nextInt(1000000000));
                    writer.print(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        IntStream.range(0, NUM_THREADS).forEach(n -> pool.submit(r));

        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < runForMillis) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Shutting down...");
        pool.shutdownNow();
    }
}
