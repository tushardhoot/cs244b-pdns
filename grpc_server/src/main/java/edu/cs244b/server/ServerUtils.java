package edu.cs244b.server;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.util.JsonFormat;
import edu.cs244b.common.CommonUtils;
import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DNSRecordP2P;
import edu.cs244b.common.NullOrEmpty;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerUtils {

    public static final int ZERO = 0;
    public static final int MAX_HOP_ALLOWED = 32;
    public static final String DNS_STATE_SUFFIX = "/state/dns_state";

    /**
     * Gets the default mapping file from classpath.
     */
    public static URL defaultJSONLookupDB(final String domainMapping) {
        return CommonUtils.getUrl(domainMapping);
    }

    public static URL defaultPeerList(final String peers) {
        return CommonUtils.getUrl(peers);
    }

    /**
     * Parses the JSON input file containing the list of hostName to ipAddress mappings.
     */
    public static Map<String, Peer> loadPeers(final String trustedPeers) {
        InputStream inputStream;
        final URL path = defaultPeerList(trustedPeers);
        try {
            inputStream = path.openStream();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not open file path %s", path.toString()));
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Gson gson = new Gson();
        Peers peers = gson.fromJson(reader, Peers.class);

        return peers.hosts.stream().collect(Collectors.toMap(Peer::getName, Function.identity()));
    }

    public static ServerOperationalConfig getServerOpConfig(final String serverConfig) {
        InputStream inputStream;
        final URL path = getServerOperationalConfig(serverConfig);
        try {
            inputStream = path.openStream();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not open file path %s", path.toString()));
        }

        final ServerOperationalConfig.Builder builder = ServerOperationalConfig.newBuilder();
        try {
            final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            JsonFormat.parser().ignoringUnknownFields().merge(reader, builder);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not parse and merge server op config %s", path.toString()));
        }
        return builder.build();
    }

    private static URL getServerOperationalConfig(final String serverConfig) {
        return CommonUtils.getUrl(serverConfig);
    }

    public static boolean isHostNameValid(final String hostName, final int permissableLength) {
        return !NullOrEmpty.isTrue(hostName) && hostName.length() <= permissableLength;
    }

    public static boolean isDNSRecordValid(final DNSRecordP2P dnsRecordP2P) {
        if (dnsRecordP2P != null) {
            final DNSRecord dnsRecord = dnsRecordP2P.getDnsRecord();
            return dnsRecord != null
                    && NullOrEmpty.isFalse(dnsRecord.getHostName())
                    && dnsRecord.getIpAddressesCount() > 0;
        }
        return false;
    }

    public static SslContext getClientSSLContext(final String certBaseDirectory, final String peerName) throws Exception {
        return GrpcSslContexts.forClient()
                // trustManager - used for verifying server the server's certificate
                .trustManager(CertificateReader.getServerCertificateAuthority(certBaseDirectory, peerName))
                // keyManager - cert chain & key for client's certificate
                .keyManager(CertificateReader.getCertificateChain(certBaseDirectory), CertificateReader.getKey(certBaseDirectory))
                .build();
    }

    public static SslContext getServerSSLContext(final ServerOperationalConfig serverOpConfig) throws Exception {
        String certBaseDir = serverOpConfig.getSslCertBaseLocation();
        SslContextBuilder builder = GrpcSslContexts.forServer(CertificateReader.getCertificateChain(certBaseDir), CertificateReader.getKey(certBaseDir));
        if (serverOpConfig.getMutualTlsEnabled()) {
            builder.trustManager(CertificateReader.getClientCertificateAuthorities(certBaseDir));
            builder.clientAuth(ClientAuth.REQUIRE);
        }
        return builder.build();
    }

    static class Peers {
        List<Peer> hosts;
    }
}
