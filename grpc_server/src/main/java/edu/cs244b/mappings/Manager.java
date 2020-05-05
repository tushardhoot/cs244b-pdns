package edu.cs244b.mappings;

import edu.cs244b.common.CommonUtils;
import java.util.*;

public class Manager {
    RecursiveMapping mappings;

    public interface MappingStore {
        Set<LookupResult> loadMappings();
    }

    public Manager(MappingStore store) {
        mappings = new RecursiveMapping();
        store.loadMappings().forEach(result -> mappings.pushMapping(CommonUtils.rDNSForm(result.hostname), result));
    }

    public LookupResult lookupHostname(String hostname) {
        return mappings.lookup(CommonUtils.rDNSForm(hostname));
    }
}