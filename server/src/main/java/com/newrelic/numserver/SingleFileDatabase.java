package com.newrelic.numserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.AbstractCollection;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;


/**
 * A Database impl that writes all numbers to a single log file.
 */
public class SingleFileDatabase extends AbstractExecutionThreadService implements Database {

    static final String EOL = System.getProperty("line.separator");
    private static final int QUEUE_SIZE = 1024 * 1024;

    private final Path dbFile;

    private final Lock bsLock = new ReentrantLock();

    // cache of existing numbers, large enough to hold numbers with 9 digits
    private final BitSet bs = new BitSet(1000000000);

    private final BlockingQueue<Integer> writeQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final ByteBuffer bb = ByteBuffer.allocate((4 + EOL.length()) * QUEUE_SIZE);
    private final Collection<Integer> boxCar = new ByteBufferBackedCollection(bb);

    private SeekableByteChannel fileChannel;

    public SingleFileDatabase(Path dbFile) throws IOException {
        this.dbFile = dbFile;
    }

    @VisibleForTesting SingleFileDatabase(SeekableByteChannel fileChannel) {
        this.dbFile = null;
        this.fileChannel = fileChannel;
    }

    @Override
    protected void startUp() throws IOException {
        if (fileChannel == null) {
            // Create the set of options for appending to the file.
            Set<OpenOption> options = new HashSet<>();
            options.add(CREATE);
            options.add(TRUNCATE_EXISTING);
            options.add(WRITE);
            fileChannel = Files.newByteChannel(dbFile, options,
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r-----")));
        }
    }

    @Override
    protected void shutDown() throws IOException {
        if (writeQueue.drainTo(boxCar) > 0) {
            flushAndReset(bb);
        }
        fileChannel.close();
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            if (writeQueue.drainTo(boxCar) == 0) {
                // when write queue is empty block here to prevent busy loop
                Integer number = writeQueue.poll(100, TimeUnit.MILLISECONDS);
                if (number == null) {
                    // timeout with no data
                    continue;
                }
                boxCar.add(number);
            }
            flushAndReset(bb);
        }
    }

    @Override
    public boolean tryInsert(int number) throws InterruptedException {
        if (isDuplicate(number)) {
            return false;
        }
        writeQueue.put(number);
        return true;
    }

    private void flushAndReset(ByteBuffer bb) {
        bb.flip();
        try {
            fileChannel.write(bb);
        } catch (IOException x) {
            System.out.println("Exception thrown: " + x);
        }
        bb.clear();
    }

    @VisibleForTesting boolean isDuplicate(int number) {
        bsLock.lock();
        try {
            if (bs.get(number)) {
                return true;
            } else {
                bs.set(number);
                return false;
            }
        } finally {
            bsLock.unlock();
        }
    }

    private static class ByteBufferBackedCollection extends AbstractCollection<Integer> {

        private static final byte[] EOL_BYTES = EOL.getBytes(Charsets.UTF_8);
        private final ByteBuffer bb;

        private ByteBufferBackedCollection(ByteBuffer bb) {
            this.bb = bb;
        }

        @Override
        public boolean add(Integer integer) {
            bb.putInt(integer);
            bb.put(EOL_BYTES);
            return true;
        }

        @Override
        public Iterator<Integer> iterator() {
            throw new UnsupportedOperationException("write through impl only");
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("write through impl only");
        }
    }
}
