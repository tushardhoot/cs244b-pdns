package edu.cs244b.server;

import com.google.common.annotations.VisibleForTesting;
import edu.cs244b.common.CommonUtils;
import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DNSRecordP2P;
import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.Message;
import edu.cs244b.common.P2PMessage;
import edu.cs244b.mappings.JSONMappingStore;
import edu.cs244b.mappings.LookupResult;
import edu.cs244b.mappings.MappingStore;
import io.grpc.*;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class DomainLookupServer {
    private static final Logger logger = LoggerFactory.getLogger(DomainLookupServer.class);

    private final int port;
    private final Server server;
    private final DomainLookupService domainLookupService;

    public DomainLookupServer(int port) throws Exception {
        this(port, new JSONMappingStore(ServerUtils.defaultJSONLookupDB()));
    }

    /** Create a DomainLookup server using serverBuilder as a base and lookup entries as data. */
    public DomainLookupServer(int port, MappingStore mappingStore) throws Exception {
        this.port = port;
        final ServerOperationalConfig serverOpConfig = ServerUtils.getServerOpConfig();
        this.domainLookupService = new DomainLookupService(mappingStore, serverOpConfig);
        /*server = ServerBuilder
                .forPort(port)
                .intercept(new TimeoutInterceptor())
                .build();*/
        /*
         * openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -keyout key.pem -out cert.pem
         * generate the private key and ssl certificate
         * server certificate: /var/cs244b.p2p.dns/ssl_certificates/server
         * client certificate: /var/cs244b.p2p.dns/ssl_certificates/client
         * trusted contacts certificate: /var/cs244b.p2p.dns/ssl_certificates/trusted_contacts/<contact_id>
         */
        server = NettyServerBuilder.forPort(port)
                .sslContext(ServerUtils.getServerSSLContext(serverOpConfig))
                .addService(domainLookupService)
                .intercept(new TimeoutInterceptor())
                .build();
    }

    /** Start serving requests. */
    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            logger.error("*** shutting down gRPC server since JVM is shutting down");
            try {
                DomainLookupServer.this.stop();
            } catch (InterruptedException ex) {
                logger.error("Exception while stopping server", ex);
            }
            logger.error("*** server shut down");
        }));
    }

    /** Stop serving requests and shutdown resources. */
    public void stop() throws InterruptedException {
        if (domainLookupService != null) {
            //TODO - also store state at every 5-10 mins as backup in case of a failure
            domainLookupService.storeState();
        }

        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Implementation of DomainLookupService.
     * <p>See domain_lookup.proto for details of the methods.
     */
    static class DomainLookupService extends DomainLookupServiceGrpc.DomainLookupServiceImplBase {
        private final MappingStore mappingStore;
        private final Map<String, Peer> peers;

        private final long dnsExpiryTime;
        private final int maxAllowedHops;
        private final int maxAllowedHostNameLength;
        private final String sslCertBaseDirectory;

        private final DNSCache dnsCache;

        DomainLookupService(final MappingStore mappingStore, final ServerOperationalConfig serverOpConfig) {
            this.mappingStore = mappingStore;
            this.mappingStore.setup();
            this.peers = ServerUtils.loadPeers();

            dnsExpiryTime = serverOpConfig.getDnsExpiryDays() * DateTimeConstants.MILLIS_PER_DAY;
            maxAllowedHops = Math.min(serverOpConfig.getMaxHopCount(), ServerUtils.MAX_HOP_ALLOWED);
            maxAllowedHostNameLength = serverOpConfig.getPermissibleHostNameLength();
            sslCertBaseDirectory = serverOpConfig.getSslCertBaseLocation();
            dnsCache = new DNSCache(serverOpConfig.getDnsCacheCapacity(), logger);
            dnsCache.load(serverOpConfig.getDnsStateFileLocation() + ServerUtils.DNS_STATE_SUFFIX);
        }

        @VisibleForTesting
        DomainLookupService(final MappingStore mappingStore,
                            final Map<String, Peer> peers,
                            final ServerOperationalConfig serverOpConfig,
                            final String dnsStateFilePath) {
            this.mappingStore = mappingStore;
            this.mappingStore.setup();
            this.peers = peers;
            dnsExpiryTime = serverOpConfig.getDnsExpiryDays() * DateTimeConstants.MILLIS_PER_DAY;
            maxAllowedHops = serverOpConfig.getMaxHopCount();
            maxAllowedHostNameLength = serverOpConfig.getPermissibleHostNameLength();
            sslCertBaseDirectory = serverOpConfig.getSslCertBaseLocation();
            dnsCache = new DNSCache(serverOpConfig.getDnsCacheCapacity(), logger);
            dnsCache.load(dnsStateFilePath);
        }

        @Override
        public void getDomain(final Message message, final StreamObserver<DNSRecord> responseObserver) {
            logger.info("Incoming request from client for host name: {}", message.getHostName());
            final Pair<Status, DNSRecordP2P> dnsInfo = resolveDNSInfo(message, maxAllowedHops);
            if (dnsInfo.getKey() != Status.OK) {
                responseObserver.onError(new StatusException(dnsInfo.getKey()));
                return;
            }

            final String resolvedIp = (dnsInfo.getValue().getDnsRecord().getIpAddressesCount() > 0) ?
                    dnsInfo.getValue().getDnsRecord().getIpAddresses(0) : "";
            logger.info("DNS resolved for host name {}: {}", message.getHostName(), resolvedIp);
            responseObserver.onNext(dnsInfo.getValue().getDnsRecord());
            responseObserver.onCompleted();
        }

        @Override
        public void getDomainP2P(final P2PMessage pMessage, final StreamObserver<DNSRecordP2P> responseObserver) {
            if (pMessage.getMessage() == null || pMessage.getHopCount() < ServerUtils.ZERO) {
                responseObserver.onError(new StatusException(Status.INVALID_ARGUMENT));
                return;
            }

            logger.info("Incoming P2P request for host name: {}", pMessage.getMessage().getHostName());
            final long currentTimeMillis = System.currentTimeMillis();
            final Pair<Status, DNSRecordP2P> dnsInfo = resolveDNSInfo(pMessage.getMessage(), Math.min(pMessage.getHopCount(), maxAllowedHops));
            if (dnsInfo.getKey() != Status.OK) {
                responseObserver.onError(new StatusException(dnsInfo.getKey()));
                return;
            }
            if (ServerUtils.isDNSRecordValid(dnsInfo.getValue()) && dnsInfo.getValue().getExpiryTime() < currentTimeMillis) {
                responseObserver.onError(new StatusException(Status.INTERNAL));
                return;
            }

            final String resolvedIp = (dnsInfo.getValue().getDnsRecord().getIpAddressesCount() > 0) ?
                    dnsInfo.getValue().getDnsRecord().getIpAddresses(0) : "";
            logger.info("P2P resolved DNS for host name {}: {}", pMessage.getMessage().getHostName(), resolvedIp);
            responseObserver.onNext(dnsInfo.getValue());
            responseObserver.onCompleted();
        }

        private Pair<Status, DNSRecordP2P> resolveDNSInfo(final Message message, final int remainingHops) {
            if (!ServerUtils.isHostNameValid(message.getHostName(), maxAllowedHostNameLength)) {
                return Pair.of(Status.INVALID_ARGUMENT, DNSRecordP2P.newBuilder().build());
            }

            // check local dns cache for resolution - return if found
            final DNSCache.DNSInfo dnsInfo = dnsCache.get(message.getHostName());
            final DNSRecordP2P dnsRecord = buildP2PResponse(message.getHostName(), dnsInfo);
            if (dnsRecord != null) {
                return Pair.of(Status.OK, dnsRecord);
            }

            final LookupResult lookupResult = mappingStore.lookup(message.getHostName());
            if (lookupResult == null) {
                return Pair.of(Status.NOT_FOUND, DNSRecordP2P.newBuilder().build());
            }

            Status status = Status.OK;
            DNSRecordP2P dnsRecordP2P = null;
            if (lookupResult.getType() == LookupResult.MappingType.INDIRECT) {
                if (remainingHops > ServerUtils.ZERO) {
                    try {
                        dnsRecordP2P = indirectResolution(message.getHostName(), lookupResult, remainingHops);
                    } catch (StatusRuntimeException sre) {
                        switch (sre.getStatus().getCode()) {
                            case NOT_FOUND:
                                status = Status.NOT_FOUND;
                                break;
                            default:
                                logger.error("StatusRuntimeException error", sre);
                                status = Status.INTERNAL;
                        }
                    } catch (SSLException ssle) {
                        status = Status.UNAUTHENTICATED;
                    } catch (Exception ex) {
                        logger.error("resolveDNSInfo error", ex);
                        status = Status.INTERNAL;
                    }
                } else {
                    // instead of error, send an empty dns record to uniquely identify this case
                    dnsRecordP2P = DNSRecordP2P.newBuilder().build();
                    logger.info("Max hops breached for host name: {}", message.getHostName());
                }
            } else {
                dnsRecordP2P = buildP2PResponse(message.getHostName(), lookupResult);
            }

            dnsCache.registerDnsRecord(dnsRecordP2P);
            return Pair.of(status, dnsRecordP2P);
        }

        private DNSRecordP2P indirectResolution(
                final String hostname,
                final LookupResult lookupResult,
                final int remainingHops) throws Exception {
            final String peerName = lookupResult.getValue();
            if (!peers.containsKey(peerName)) {
                throw new StatusException(Status.INTERNAL);
            }

            final Peer peer = peers.get(peerName);
            return peer.getStub(sslCertBaseDirectory)
                    .getDomainP2P(P2PMessage.newBuilder()
                            .setMessage(Message.newBuilder()
                                    .setHostName(hostname).build())
                            .setHopCount(remainingHops - 1)
                            .build());
        }

        private DNSRecordP2P buildP2PResponse(final String hostname, final LookupResult result) {
            final DNSRecordP2P.Builder builder = DNSRecordP2P.newBuilder();
            if (result != null) {
                final DNSRecord dnsRecord = DNSRecord.newBuilder()
                        .setHostName(hostname)
                        .addIpAddresses(result.getValue())
                        .build();
                builder.setDnsRecord(dnsRecord)
                        .setExpiryTime(System.currentTimeMillis() + dnsExpiryTime);
            }
            return builder.build();
        }

        private DNSRecordP2P buildP2PResponse(final String hostName, final DNSCache.DNSInfo dnsInfo) {
            if (dnsInfo == null) {
                return null;
            }

            try {
                return DNSRecordP2P.newBuilder()
                        .setDnsRecord(DNSRecord.newBuilder()
                                .setHostName(hostName)
                                .addIpAddresses(CommonUtils.intToIp(dnsInfo.ip))
                                .build())
                        .setExpiryTime(dnsInfo.expiryTime)
                        .build();
            } catch (UnknownHostException ex) {
                logger.error("Error while resolving IP address", ex);
                return null;
            }
        }

        private void storeState() {
            // TODO
        }
    }
}

