package edu.cs244b.server;

import edu.cs244b.common.DomainLookupServiceGrpc;
import edu.cs244b.common.DomainLookupServiceGrpc.DomainLookupServiceBlockingStub;
import io.grpc.ManagedChannelBuilder;

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

    DomainLookupServiceBlockingStub getStub() {
        if (stub == null) {
            ManagedChannelBuilder<?> mcb = ManagedChannelBuilder.forAddress(ip_address, port).usePlaintext();
            stub = DomainLookupServiceGrpc.newBlockingStub(mcb.build());
        }

        return stub;
    }
}
