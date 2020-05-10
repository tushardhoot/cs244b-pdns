package edu.cs244b.server;

import com.google.common.annotations.VisibleForTesting;
import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DNSRecordP2P;
import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.Message;
import edu.cs244b.common.NullOrEmpty;
import edu.cs244b.common.P2PMessage;
import edu.cs244b.mappings.JSONMappingStore;
import edu.cs244b.mappings.LookupResult;
import edu.cs244b.mappings.Manager;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class DomainLookupServer {
    private static final Logger logger = LoggerFactory.getLogger(DomainLookupServer.class);

    private final int port;
    private final Server server;

    public DomainLookupServer(int port) {
        this(port, new JSONMappingStore(ServerUtils.defaultJSONLookupDB()));
    }

    /** Create a DomainLookup server using serverBuilder as a base and lookup entries as data. */
    public DomainLookupServer(int port, Manager.MappingStore mappingStore) {
        this.port = port;
        server = ServerBuilder
                .forPort(port)
                .addService(new DomainLookupService(new Manager(mappingStore)))
                .build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                DomainLookupServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
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
        int port = 8980;
        if (args.length > 0 && NullOrEmpty.isFalse(args[0])) {
            port = Integer.parseInt(args[0]);
        }

        DomainLookupServer server = new DomainLookupServer(port);
        server.start();
        server.blockUntilShutdown();
    }

    /**
     * Implementation of DomainLookupService.
     * <p>See domain_lookup.proto for details of the methods.
     */
    static class DomainLookupService extends DomainLookupServiceGrpc.DomainLookupServiceImplBase {
        private final Manager mappingManager;
        private final Map<String, Peer> peers;

        private final long dnsExpiryTime;
        private final long maxAllowedHops;

        DomainLookupService(final Manager manager) {
            this.mappingManager = manager;
            this.peers = ServerUtils.loadPeers();

            final ServerOperationalConfig serverOpConfig = ServerUtils.getServerOpConfig();
            dnsExpiryTime = serverOpConfig.getDnsExpiryDays() * DateTimeConstants.MILLIS_PER_DAY;
            maxAllowedHops = serverOpConfig.getMaxHopCount();
        }

        @VisibleForTesting
        DomainLookupService(final Manager manager,
                            final Map<String, Peer> peers,
                            final ServerOperationalConfig serverOpConfig) {
            this.mappingManager = manager;
            this.peers = peers;
            dnsExpiryTime = serverOpConfig.getDnsExpiryDays() * DateTimeConstants.MILLIS_PER_DAY;
            maxAllowedHops = serverOpConfig.getMaxHopCount();
        }

        @Override
        public void getDomain(final Message message, final StreamObserver<DNSRecord> responseObserver) {
            final Pair<Status, DNSRecordP2P> dnsInfo = resolveDNSInfo(message, ServerUtils.ZERO);
            if (dnsInfo.getKey() != Status.OK) {
                responseObserver.onError(new StatusException(dnsInfo.getKey()));
                return;
            }

            responseObserver.onNext(dnsInfo.getValue().getDnsRecord());
            responseObserver.onCompleted();
        }

        @Override
        public void getDomainP2P(final P2PMessage pMessage, final StreamObserver<DNSRecordP2P> responseObserver) {
            if (pMessage.getMessage() == null || pMessage.getHopCount() < ServerUtils.ONE) {
                responseObserver.onError(new StatusException(Status.INVALID_ARGUMENT));
                return;
            }

            if (pMessage.getHopCount() > maxAllowedHops) {
                responseObserver.onNext(DNSRecordP2P.newBuilder().build());
                responseObserver.onCompleted();
                //responseObserver.onError(new StatusException(Status.OUT_OF_RANGE));
                return;
            }

            final Pair<Status, DNSRecordP2P> dnsInfo = resolveDNSInfo(pMessage.getMessage(), pMessage.getHopCount());
            if (dnsInfo.getKey() != Status.OK) {
                responseObserver.onError(new StatusException(dnsInfo.getKey()));
                return;
            }

            responseObserver.onNext(dnsInfo.getValue());
            responseObserver.onCompleted();
        }

        private Pair<Status, DNSRecordP2P> resolveDNSInfo(final Message message, final int hopCount) {
            if (NullOrEmpty.isTrue(message.getHostName())) {
                return Pair.of(Status.INVALID_ARGUMENT, DNSRecordP2P.newBuilder().build());
            }

            final LookupResult lookupResult = mappingManager.lookupHostname(message.getHostName());
            if (lookupResult == null) {
                return Pair.of(Status.NOT_FOUND, DNSRecordP2P.newBuilder().build());
            }

            Status status = Status.OK;
            DNSRecordP2P dnsRecordP2P = null;
            if (lookupResult.getType() == LookupResult.MappingType.INDIRECT) {
                try {
                    dnsRecordP2P = indirectResolution(lookupResult, hopCount);
                } catch (StatusRuntimeException sre) {
                    switch (sre.getStatus().getCode()) {
                        case NOT_FOUND:
                            status = Status.NOT_FOUND;
                            break;
                        default:
                            status = Status.INTERNAL;
                    }
                } catch (Exception ex) {
                    status = Status.INTERNAL;
                }
            } else {
                dnsRecordP2P = buildP2PResponse(lookupResult);
            }

            return Pair.of(status, dnsRecordP2P);
        }

        private DNSRecordP2P indirectResolution(final LookupResult lookupResult,
                                                final int hopCount) throws StatusException {
            final String peerName = lookupResult.getValue();
            if (!peers.containsKey(peerName)) {
                throw new StatusException(Status.INTERNAL);
            }

            final Peer peer = peers.get(peerName);
            return peer.getStub()
                    .getDomainP2P(P2PMessage.newBuilder()
                            .setMessage(Message.newBuilder()
                                    .setHostName(lookupResult.getHostname()).build())
                            .setHopCount(hopCount + 1)
                            .build());
        }

        private DNSRecordP2P buildP2PResponse(final LookupResult result) {
            final DNSRecordP2P.Builder builder = DNSRecordP2P.newBuilder();
            if (result != null) {
                final DNSRecord dnsRecord = DNSRecord.newBuilder()
                        .setHostName(result.getHostname())
                        .addIpAddresses(result.getValue())
                        .build();
                builder.setDnsRecord(dnsRecord)
                        .setTtl(System.currentTimeMillis() + dnsExpiryTime);
            }
            return builder.build();
        }
    }
}

