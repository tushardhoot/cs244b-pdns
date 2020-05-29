package edu.cs244b.mappings;

public interface MappingStore {
    void setup();
    LookupResult lookup(String hostname);
}