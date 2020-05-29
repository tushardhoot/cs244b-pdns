package edu.cs244b.mappings;

import edu.cs244b.common.CommonUtils;

import java.util.Set;

public class ConstantsMappingStore implements MappingStore {

    private final RecursiveMapping resolver;

    public ConstantsMappingStore(Set<LookupResult> mappings) {
        this.resolver = new RecursiveMapping();
        mappings.forEach(result -> resolver.pushMapping(CommonUtils.rDNSForm(result.hostname), result));
    }

    @Override
    public void setup() {}

    @Override
    public LookupResult lookup(String hostname) {
        return resolver.lookup(CommonUtils.rDNSForm(hostname));
    }
}
