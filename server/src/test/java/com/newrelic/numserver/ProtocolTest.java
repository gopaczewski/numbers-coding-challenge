package com.newrelic.numserver;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static junit.framework.TestCase.assertEquals;

public class ProtocolTest {

    private Protocol protocol;

    @Before
    public void setUp() {
        protocol = new Protocol();
    }

    @Test
    public void acceptInput_validNumbers() {
        assertEquals(responseForValidNumber(123456789), protocol.acceptInput("123456789"));
        assertEquals(responseForValidNumber(6789), protocol.acceptInput("000006789"));
        assertEquals(responseForValidNumber(0), protocol.acceptInput("000000000"));
    }

    @Test
    public void acceptInput_invalidCommand() {
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput("asdfasdf"));
    }

    @Test
    public void acceptInput_empty() {
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput(""));
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput("\n"));
    }

    @Test
    public void acceptInput_malformedNumber() {
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput("1234"));
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput("1234A6789"));
        assertEquals(Protocol.ClientInputResponse.CLOSE_CONNECTION, protocol.acceptInput("0xFFFFFFF"));
    }

    @Test
    public void acceptInput_terminateCommand() {
        assertEquals(Protocol.ClientInputResponse.TERMINATE, protocol.acceptInput("terminate"));
    }

    /*
     * Helpers
     */
    private static Protocol.ClientInputResponse responseForValidNumber(int number) {
        return new Protocol.ClientInputResponse(Protocol.ClientInputAction.INSERT_NUMBER, Optional.of(number));
    }
}
