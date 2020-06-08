package edu.cs244b.server;

import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.DomainLookupServiceGrpc.DomainLookupServiceBlockingStub;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;

import java.security.cert.X509Certificate;


public class Peer {
    private final String name;
    private String ip_address;
    private Integer port;

    private DomainLookupServiceBlockingStub stub;

    public Peer(String name, String ip_address, int port) {
        this.name = name;
        this.ip_address = ip_address;
        this.port = port;
    }

    Peer(String name, DomainLookupServiceBlockingStub stub) {
        this.name = name;
        this.stub = stub;
    }

    public String getName() {
        return name;
    }

    public String getIPAddress() {
        return ip_address;
    }

    public Integer getPort() {
        return port;
    }

    private static SslContext getClientSSLContext(final String certBaseDirectory, X509Certificate serverCert) throws Exception {
        return GrpcSslContexts.forClient()
                // trustManager - used for verifying server the server's certificate
                .trustManager(serverCert)
                // keyManager - cert chain & key for client's certificate
                .keyManager(CertificateReader.getCertificateChain(certBaseDirectory), CertificateReader.getKey(certBaseDirectory))
                .build();
    }

    DomainLookupServiceBlockingStub getStub(final String certBaseDirectory, final String peerName) throws Exception {
        if (stub == null) {
            X509Certificate serverCert = CertificateReader.getServerCertificateAuthority(certBaseDirectory, peerName);
            NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress(ip_address, port);

            if (serverCert != null) {
                channelBuilder
                        .negotiationType(NegotiationType.TLS)
                        .sslContext(getClientSSLContext(certBaseDirectory, serverCert));
            } else {
                channelBuilder
                        .usePlaintext();
            }

            stub = DomainLookupServiceGrpc.newBlockingStub(channelBuilder.build());
        }

        return stub;
    }
}
