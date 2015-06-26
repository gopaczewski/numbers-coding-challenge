package com.newrelic.numserver;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    private static final Logger log = LoggerFactory.getLogger(SingleFileDatabase.class);

    @VisibleForTesting static final byte[] EOL = System.getProperty("line.separator").getBytes(Charsets.UTF_8);

    private static final int QUEUE_SIZE = 1024 * 1024;

    private final Path dbFile;

    // cache of existing numbers, large enough to hold numbers with 9 digits
    private final BitSet bs = new BitSet(1000000000);
    private final Lock bsLock = new ReentrantLock();

    private final BlockingQueue<Integer> writeQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    private final ByteBuffer bb = ByteBuffer.allocate((4 + EOL.length) * QUEUE_SIZE);
    private final Collection<Integer> boxCar = new ByteBufferBackedCollection(bb);

    private SeekableByteChannel fileChannel;

    public SingleFileDatabase(Path dbFile) throws IOException {
        this.dbFile = dbFile;
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
        ((FileChannel) fileChannel).force(false);
        fileChannel.close();
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            if (writeQueue.drainTo(boxCar) == 0) {
                // when write queue is empty block here to prevent busy loop
                Integer number = writeQueue.poll(200, TimeUnit.MILLISECONDS);
                if (number == null) {
                    log.debug("Timeout in db writer thread waiting for data");
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
        } catch (IOException e) {
            // todo : create test for this!  should be able to re-open and re-populate data file
            log.error("Exception handled when writing to data file", e);
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

        private final ByteBuffer bb;

        private ByteBufferBackedCollection(ByteBuffer bb) {
            this.bb = bb;
        }

        @Override
        public boolean add(Integer integer) {
            bb.putInt(integer);
            bb.put(EOL);
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
