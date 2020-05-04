package edu.cs244b.mappings;

public class LookupResult {

    public enum MappingType {
        DIRECT,
        INDIRECT
    }

    MappingType type;
    String hostname;
    String value;

    LookupResult(MappingType type, String hostname, String value) {
        this.type = type;
        this.hostname = hostname;
        this.value = value;
    }
}
