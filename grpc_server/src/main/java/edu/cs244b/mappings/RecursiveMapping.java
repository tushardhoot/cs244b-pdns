package edu.cs244b.mappings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecursiveMapping {
    Map<String, RecursiveMapping> subMappings;

    LookupResult defaultValue;

    RecursiveMapping() {
        subMappings = new HashMap<>();
    }

    RecursiveMapping(LookupResult defaultValue) {
        subMappings = new HashMap<>();
        this.defaultValue = defaultValue;
    }

    public void pushMapping(List<String> remainder, LookupResult value) {
        if (remainder.isEmpty()) {
            throw new RuntimeException("Pushed an empty mapping");
        }

        String part = remainder.remove(0);
        if (remainder.isEmpty()) {
            if (subMappings.containsKey(part)) {
                throw new RuntimeException(String.format("Duplicate mappings for %s", part));
            }

            subMappings.put(part, new RecursiveMapping(value));
        } else {
            subMappings.putIfAbsent(part, new RecursiveMapping());
            subMappings.get(part).pushMapping(remainder, value);
        }
    }

    public LookupResult lookup(List<String> remainder) {
        if (remainder.isEmpty()) {
            return defaultValue;
        }

        String nextPart = remainder.remove(0);
        LookupResult result = null;
        if (subMappings.containsKey(nextPart)) {
            result = subMappings.get(nextPart).lookup(remainder);
        }

        if (result == null) {
            // Couldn't find a match downstream.
            return defaultValue;
        }

        return result;
    }
}
