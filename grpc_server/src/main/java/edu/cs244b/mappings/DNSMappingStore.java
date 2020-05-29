package edu.cs244b.mappings;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DNSMappingStore implements MappingStore {

    @Override
    public void setup() {}

    @Override
    public LookupResult lookup(String hostname) {
        try {
            InetAddress addr = InetAddress.getByName(hostname);
            return new LookupResult(LookupResult.MappingType.DIRECT, hostname, addr.getHostAddress());
        } catch (UnknownHostException uhe) {
            return null;
        }
    }

}
