package com.newrelic.numserver;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Numbers Server.  Listens for client connections over a server socket up to a maximum number of concurrent
 * connections.  Client connections are accepted in the main thread.
 */
public class Server {

    private final String listenAddress;
    private final int listenPort;
    private final Semaphore clientPermits;

    private final ExecutorService clientAcceptPool;
    private final ExecutorService clientConnectionPool;

    private final Protocol protocol = new Protocol();
    private final MetricsReporter metricsReporter;
    private final ServiceManager serviceManager;
    private final Database database;

    private ServerSocket serverSocket;

    public Server(String listenAddress, int listenPort, int maxConcurrentClients) throws IOException {
        this.listenAddress = listenAddress;
        this.listenPort = listenPort;
        this.clientAcceptPool = Executors.newSingleThreadExecutor();
        this.clientPermits = new Semaphore(maxConcurrentClients);
        this.clientConnectionPool = Executors.newFixedThreadPool(maxConcurrentClients);

        // todo : DI via Guice
        SingleFileDatabase sfdb = new SingleFileDatabase(Paths.get("numbers.log"));
        this.database = sfdb;

        ConsoleMetricsReporter reporter = new ConsoleMetricsReporter(Duration.ofSeconds(10));
        this.metricsReporter = reporter;

        Set<Service> services = Sets.newHashSet(sfdb, reporter);
        this.serviceManager = new ServiceManager(services);
    }

    public static void main(String[] args) {
        try {
            // todo: make these cmdline args
            Server server = new Server("0.0.0.0", 4000, 5);
            server.start();
            server.waitForTermination();
        } catch (InterruptedException | IOException e) {
            // todo
            e.printStackTrace();
        }
    }

    public void start() throws InterruptedException, IOException {
        try {
            serverSocket = new ServerSocket();
            serverSocket.setSoTimeout(2000);  // 2 second timeout for accept
            serverSocket.bind(new InetSocketAddress(listenAddress, listenPort));
        } catch (IOException e) {
            // todo
            System.err.println("Failed to bind server socket: \n" + e);
            throw e;
        }

        serviceManager.startAsync();

        clientAcceptPool.execute(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (clientPermits.tryAcquire(1, 500, TimeUnit.MILLISECONDS)) {
                        Socket socket = null;
                        try {
                            socket = serverSocket.accept();
                            final Client client = new Client(socket);
                            clientConnectionPool.execute(client::acceptInput);
                        } catch (IOException e) {
                            // this occurs on socket timeout or when the socket is closed at shutdown
                            // todo: log warn
                            clientPermits.release();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private void shutdown() {
        // todo : convert these all to Services/ServiceManager
        clientAcceptPool.shutdownNow();
        clientConnectionPool.shutdownNow();

        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        serviceManager.stopAsync();
    }

    private void waitForTermination() {
        try {
            while (! clientAcceptPool.awaitTermination(1000, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private class Client {

        private final Socket socket;

        private Client(Socket socket) throws SocketException {
            this.socket = Objects.requireNonNull(socket);
            this.socket.setSoTimeout(10000); // 10s timeout for client socket read
            // todo: convert to debug
            System.out.println("Client connected.");
        }

        private void acceptInput() {
            try {
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));

                String line;
                boolean closeClient = false;
                while (!Thread.currentThread().isInterrupted() && !closeClient && (line = br.readLine()) != null) {
                    Protocol.ClientInputResponse pr = protocol.acceptInput(line);

                    if (Protocol.ClientInputResponse.TERMINATE.equals(pr)) {
                        Server.this.shutdown();
                    } else if (Protocol.ClientInputResponse.CLOSE_CONNECTION.equals(pr)) {
                        closeClient = true;
                    } else {
                        metricsReporter.recordInsert(database.tryInsert(pr.getNumber().get()));
                    }
                }
            } catch (SocketTimeoutException e) {
                // todo: log debug
            } catch (IOException e) {
                // todo
                e.printStackTrace();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                clientPermits.release();
                try {
                    socket.close();
                } catch (IOException e) {
                    // todo
                    e.printStackTrace();
                }
            }

            // TODO: convert to debug
            System.out.println("Client disconnected.");
        }
    }
}
