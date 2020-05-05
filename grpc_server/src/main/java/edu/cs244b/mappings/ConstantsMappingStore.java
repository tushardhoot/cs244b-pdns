package edu.cs244b.mappings;

import java.util.Set;

public class ConstantsMappingStore implements Manager.MappingStore {

    private Set<LookupResult> constantMappings;

    public ConstantsMappingStore(Set<LookupResult> mappings) {
        this.constantMappings = mappings;
    }

    @Override
    public Set<LookupResult> loadMappings() {
        return constantMappings;
    }
}
