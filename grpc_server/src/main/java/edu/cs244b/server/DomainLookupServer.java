package edu.cs244b.server;

import edu.cs244b.common.CommonUtils;
import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.HostName;
import edu.cs244b.common.NullOrEmpty;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;


public class DomainLookupServer {
    private static final Logger logger = LoggerFactory.getLogger(DomainLookupServer.class);

    private final int port;
    private final Server server;

    public DomainLookupServer(int port) throws IOException {
        this(port, ServerUtils.getDefaultLookupFile());
    }

    /** Create a DomainLookup server listening on {@code port} using {@code lookupFile} database. */
    public DomainLookupServer(int port, URL lookupFile) throws IOException {
        this(ServerBuilder.forPort(port), port, ServerUtils.parseDomainMappings(lookupFile));
    }

    /** Create a DomainLookup server using serverBuilder as a base and lookup entries as data. */
    public DomainLookupServer(ServerBuilder<?> serverBuilder, int port, Collection<DomainLookupMapping.Entry> lookupEntries) {
        this.port = port;
        server = serverBuilder.addService(new DomainLookupService(lookupEntries)).build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    DomainLookupServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws Exception {
        //TODO: Decide a port and make it a constant
        DomainLookupServer server = new DomainLookupServer(8980);
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Implementation of DomainLookupService.
     * <p>See domain_lookup.proto for details of the methods.
     */
    private static class DomainLookupService extends DomainLookupServiceGrpc.DomainLookupServiceImplBase {
        private final ConcurrentMap<String, Integer> ipByHostName = new ConcurrentHashMap<String, Integer>();

        DomainLookupService(final Collection<DomainLookupMapping.Entry> lookupEntries) {
            for (final DomainLookupMapping.Entry entry : lookupEntries) {
                ipByHostName.put(entry.getHostName(), CommonUtils.ipToInt(entry.getIpAddress()));
            }
        }

        @Override
        public void getDomain(final HostName hostName, StreamObserver<DNSRecord> responseObserver) {
            responseObserver.onNext(resolveHostname(hostName));
            responseObserver.onCompleted();
        }

        private DNSRecord resolveHostname(final HostName hostName) {
            /* TODO:
             * Get the best match mapping
             * Resolve if it is pointing to some other trusted server
             */
            final DNSRecord.Builder builder = DNSRecord.newBuilder();
            if (NullOrEmpty.isFalse(hostName.getName()) && ipByHostName.containsKey(hostName.getName())) {
                builder.addIpAddresses(CommonUtils.intToIp(ipByHostName.get(hostName.getName())));
            }
            return builder.build();
        }

    }
}

