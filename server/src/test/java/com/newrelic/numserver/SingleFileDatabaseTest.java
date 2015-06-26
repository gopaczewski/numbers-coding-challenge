package com.newrelic.numserver;

import com.google.common.base.Charsets;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

/**
 * TODO
 */
public class SingleFileDatabaseTest {

    @Mock SeekableByteChannel sbc;
    @Captor ArgumentCaptor<ByteBuffer> captor;

    private SingleFileDatabase database;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        database = new SingleFileDatabase(sbc);
    }

    @Test
    public void isDuplicate() throws Exception {
        assertFalse(database.isDuplicate(0));
        assertTrue(database.isDuplicate(0));
        assertFalse(database.isDuplicate(1));
        assertTrue(database.isDuplicate(1));
    }
}