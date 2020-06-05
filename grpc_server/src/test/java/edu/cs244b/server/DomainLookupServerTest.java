package edu.cs244b.server;

import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.DomainLookupServiceGrpc.DomainLookupServiceBlockingStub;
import edu.cs244b.server.DomainLookupServer.DomainLookupService;
import edu.cs244b.common.Message;
import edu.cs244b.mappings.ConstantsMappingStore;
import edu.cs244b.mappings.LookupResult;
import edu.cs244b.mappings.MappingStore;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import io.grpc.testing.GrpcCleanupRule;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class DomainLookupServerTest {

    private static final String uuid = UUID.randomUUID().toString();
    private static final String empty_dns_state_file_path = "/tmp/empty_dns_state_" + uuid;
    private static final String cert_base_dir = "/var/cs244b.p2p.dns/ssl_certificates";

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @BeforeClass
    public static void init() throws IOException {
        BufferedWriter w = new BufferedWriter(new FileWriter(empty_dns_state_file_path));
        w.write("");
        w.close();
    }

    @AfterClass
    public static void close() {
        new File(empty_dns_state_file_path).delete();
    }

    String setupServer(MappingStore mappings, Map<String, Peer> peers) throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        final ServerOperationalConfig config = ServerOperationalConfig.newBuilder()
                .setDnsExpiryDays(3).setMaxHopCount(2).setDnsCacheCapacity(50).setDnsStateFileLocation("/tmp/")
                .setMutualTlsEnabled(true).setSslCertBaseLocation(cert_base_dir)
                .setPermissibleHostNameLength(50).build();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor().addService(
                        new DomainLookupService(mappings, peers, config, "")
                ).build().start()
        );

        return serverName;
    }

    String setupServer(final MappingStore mappings,
                       final Map<String, Peer> peers,
                       final ServerOperationalConfig config) throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor().addService(
                        new DomainLookupService(mappings, peers, config, "")
                ).build().start()
        );

        return serverName;
    }

    DomainLookupServiceBlockingStub setupClient(String serverName) {
        return DomainLookupServiceGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build())
        );
    }

    @Test
    public void domainLookup_HostnameNotFound() throws Exception {
        String serverName = setupServer(
                new ConstantsMappingStore(Collections.emptySet()),
                Collections.emptyMap()
        );
        DomainLookupServiceBlockingStub stub = setupClient(serverName);

        exceptionRule.expect(StatusRuntimeException.class);
        exceptionRule.expectMessage("NOT_FOUND");

        stub.getDomain(Message.newBuilder().setHostName("facebook.com").build());
    }

    @Test
    public void domainLookup_DirectLookup() throws Exception {
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "facebook.com", "1.2.3.4")
        );

        String serverName = setupServer(
                new ConstantsMappingStore(mappings),
                Collections.emptyMap()
        );
        DomainLookupServiceBlockingStub stub = setupClient(serverName);
        DNSRecord reply = stub.getDomain(Message.newBuilder().setHostName("facebook.com").build());
        assertEquals(1, reply.getIpAddressesCount());
        assertEquals("1.2.3.4", reply.getIpAddresses(0));

        //Checking if the DNS record from cache is correct
        DNSRecord reply2 = stub.getDomain(Message.newBuilder().setHostName("facebook.com").build());
        assertEquals(1, reply2.getIpAddressesCount());
        assertEquals("1.2.3.4", reply2.getIpAddresses(0));
    }

    @Test
    public void domainLookup_IndirectLookup() throws Exception {
        // Setup peer.
        Set<LookupResult> friendServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "usa.gov", "1.2.3.4")
        );
        String friendServer = setupServer(new ConstantsMappingStore(friendServerMappings), Collections.emptyMap());

        // Setup server.
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov")
        );
        Map<String, Peer> peers = Collections.singletonMap("usgov", new Peer("usgov", setupClient(friendServer)));
        String serverName = setupServer(new ConstantsMappingStore(mappings), peers);

        // Test indirect call.
        DomainLookupServiceBlockingStub stub = setupClient(serverName);
        DNSRecord reply = stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
        assertEquals(1, reply.getIpAddressesCount());
        assertEquals("1.2.3.4", reply.getIpAddresses(0));

        //Checking the correctness of DNS record served from cache
        DNSRecord reply2 = stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
        assertEquals(1, reply2.getIpAddressesCount());
        assertEquals("1.2.3.4", reply2.getIpAddresses(0));
    }

    @Test
    public void domainLookup_IndirectLookupHostnameNotFound() throws Exception {
        // Setup peer.
        Set<LookupResult> friendServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "facebook.com", "1.2.3.4")
        );
        String friendServer = setupServer(new ConstantsMappingStore(friendServerMappings), Collections.emptyMap());

        // Setup server.
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov")
        );
        Map<String, Peer> peers = Collections.singletonMap("usgov", new Peer("usgov", setupClient(friendServer)));
        String serverName = setupServer(new ConstantsMappingStore(mappings), peers);

        // Test indirect call.
        DomainLookupServiceBlockingStub stub = setupClient(serverName);

        exceptionRule.expect(StatusRuntimeException.class);
        exceptionRule.expectMessage("NOT_FOUND");

        stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
    }

    @Test
    public void domainLookup_IndirectLookupPeerNotFound() throws Exception {
        // Setup peer.
        Set<LookupResult> friendServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "usa.gov", "1.2.3.4")
        );
        String friendServer = setupServer(new ConstantsMappingStore(friendServerMappings), Collections.emptyMap());

        // Setup server.
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov")
        );
        Map<String, Peer> peers = Collections.singletonMap("differentpeer", new Peer("differentpeer", setupClient(friendServer)));
        String serverName = setupServer(new ConstantsMappingStore(mappings), peers);

        // Test indirect call.
        DomainLookupServiceBlockingStub stub = setupClient(serverName);

        exceptionRule.expect(StatusRuntimeException.class);
        exceptionRule.expectMessage("INTERNAL");

        stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
    }

    @Test
    public void testMaxHopCountLimitPass() throws Exception {
        final ServerOperationalConfig config = ServerOperationalConfig.newBuilder()
                .setDnsExpiryDays(3).setMaxHopCount(2).setDnsCacheCapacity(50).setDnsStateFileLocation("/tmp/")
                .setMutualTlsEnabled(true).setSslCertBaseLocation(cert_base_dir)
                .setPermissibleHostNameLength(50).build();

        // Setup peer-peer
        final Set<LookupResult> friendFriendServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "usa.gov", "1.2.3.4")
        );
        final String peerPeerServer = setupServer(new ConstantsMappingStore(friendFriendServerMappings), Collections.emptyMap(), config);

        // Setup peer
        final Set<LookupResult> peerServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov2")
        );
        Map<String, Peer> peerPeers = Collections.singletonMap("usgov2", new Peer("usgov2", setupClient(peerPeerServer)));
        final String peerServer = setupServer(new ConstantsMappingStore(peerServerMappings), peerPeers, config);

        // Setup server
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov")
        );
        Map<String, Peer> peers = Collections.singletonMap("usgov", new Peer("usgov", setupClient(peerServer)));
        String serverName = setupServer(new ConstantsMappingStore(mappings), peers, config);

        // Test indirect call.
        DomainLookupServiceBlockingStub stub = setupClient(serverName);
        DNSRecord reply = stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
        assertEquals(1, reply.getIpAddressesCount());
        assertEquals("1.2.3.4", reply.getIpAddresses(0));
    }

    @Test
    public void testMaxHopCountLimitFail() throws Exception {
        final ServerOperationalConfig config = ServerOperationalConfig.newBuilder()
                .setDnsExpiryDays(3).setMaxHopCount(1).setDnsCacheCapacity(50).setDnsStateFileLocation("/tmp/")
                .setPermissibleHostNameLength(50)
                .setMutualTlsEnabled(true).setSslCertBaseLocation(cert_base_dir)
                .build();

        // Setup peer-peer
        final Set<LookupResult> friendFriendServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.DIRECT, "usa.gov", "1.2.3.4")
        );
        final String peerPeerServer = setupServer(new ConstantsMappingStore(friendFriendServerMappings), Collections.emptyMap(), config);

        // Setup peer
        final Set<LookupResult> peerServerMappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov2")
        );
        Map<String, Peer> peerPeers = Collections.singletonMap("usgov2", new Peer("usgov2", setupClient(peerPeerServer)));
        final String peerServer = setupServer(new ConstantsMappingStore(peerServerMappings), peerPeers, config);

        // Setup server
        Set<LookupResult> mappings = Collections.singleton(
                new LookupResult(LookupResult.MappingType.INDIRECT, "usa.gov", "usgov")
        );
        Map<String, Peer> peers = Collections.singletonMap("usgov", new Peer("usgov", setupClient(peerServer)));
        String serverName = setupServer(new ConstantsMappingStore(mappings), peers, config);

        // Test indirect call.
        DomainLookupServiceBlockingStub stub = setupClient(serverName);
        DNSRecord reply = stub.getDomain(Message.newBuilder().setHostName("usa.gov").build());
        assertEquals(0, reply.getIpAddressesCount());
        assertTrue(reply.getHostName().isEmpty());
    }
}
