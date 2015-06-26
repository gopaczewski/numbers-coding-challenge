package com.newrelic.numserver;

import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SingleFileDatabaseTest {

    File dbFile;
    SingleFileDatabase database;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        dbFile = File.createTempFile("numbers", ".tmp");
        database = new SingleFileDatabase(Paths.get(dbFile.getAbsolutePath()));
        database.startAsync();
    }

    @After
    public void tearDown() throws Exception {
        dbFile.delete();
    }

    @Test
    public void tryInsert() throws Exception {
        assertTrue(database.tryInsert(7777));
        assertTrue(database.tryInsert(0));
        assertTrue(database.tryInsert(999999999));

        assertFalse(database.tryInsert(0));
        assertFalse(database.tryInsert(7777));

        byte[] expected = ByteBuffer.allocate(15)
                .putInt(7777)
                .put(SingleFileDatabase.EOL)
                .putInt(0)
                .put(SingleFileDatabase.EOL)
                .putInt(999999999)
                .put(SingleFileDatabase.EOL)
                .array();

        database.stopAsync();

        // hate putting pauses like this in tests, but this was quick and dirty
        database.awaitTerminated(2000, TimeUnit.MILLISECONDS);

        assertArrayEquals(expected, Files.toByteArray(dbFile));
        System.out.println(dbFile.getAbsolutePath());
    }

    @Test
    public void isDuplicate() throws Exception {
        assertFalse(database.isDuplicate(0));
        assertTrue(database.isDuplicate(0));
        assertFalse(database.isDuplicate(1));
        assertTrue(database.isDuplicate(1));
        assertFalse(database.isDuplicate(333));
        assertTrue(database.isDuplicate(333));
        assertFalse(database.isDuplicate(999999999));
        assertTrue(database.isDuplicate(999999999));
    }
}