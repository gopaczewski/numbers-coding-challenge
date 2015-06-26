package com.newrelic.numserver;

import com.google.common.annotations.VisibleForTesting;

import java.util.Objects;
import java.util.Optional;

/**
 * Parses and interprets commands sent to the server.
 */
public class Protocol {

    private static final int NUMBER_LENGTH = 9;

    @VisibleForTesting static final String EOL = System.getProperty("line.separator");
    @VisibleForTesting static final String TERMINATE_INPUT_CMD = "terminate";


    public ClientInputResponse acceptInput(String line) {
        if (TERMINATE_INPUT_CMD.equals(line)) {
            return new ClientInputResponse(ClientInputAction.TERMINATE);
        }
        try {
            return new ClientInputResponse(ClientInputAction.INSERT_NUMBER, Optional.of(parseNumber(line)));
        } catch (IllegalArgumentException e) {
            return new ClientInputResponse(ClientInputAction.CLOSE_CONNECTION);
        }
    }

    private int parseNumber(String line) {
        if (line.length() != NUMBER_LENGTH) {
            throw new IllegalArgumentException("Invalid input line: " + line);
        } else {
            return Integer.parseInt(line.trim());
        }
    }

    public enum ClientInputAction {
        INSERT_NUMBER, CLOSE_CONNECTION, TERMINATE
    }

    public static class ClientInputResponse {
        private final ClientInputAction cmd;
        private final Optional<Integer> number;

        private ClientInputResponse(ClientInputAction cmd) {
            this(cmd, Optional.<Integer>empty());
        }

        public ClientInputResponse(ClientInputAction cmd, Optional<Integer> number) {
            this.cmd = Objects.requireNonNull(cmd);
            this.number = Objects.requireNonNull(number);
        }

        public ClientInputAction getCmd() {
            return cmd;
        }

        public Optional<Integer> getNumber() {
            return number;
        }

        public static ClientInputResponse TERMINATE = new ClientInputResponse(ClientInputAction.TERMINATE);
        public static ClientInputResponse CLOSE_CONNECTION = new ClientInputResponse(ClientInputAction.CLOSE_CONNECTION);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientInputResponse that = (ClientInputResponse) o;

            return cmd.equals(that.cmd) && number.equals(that.number);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cmd, number);
        }

        @Override
        public String toString() {
            return "ClientInputResponse{" +
                    "cmd=" + cmd +
                    ", number=" + number +
                    '}';
        }
    }
}
