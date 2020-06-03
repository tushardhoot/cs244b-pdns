package edu.cs244b.mappings;

public class LookupResult {

    public enum MappingType {
        DIRECT,
        INDIRECT
    }

    MappingType type;
    String hostname;
    String value;

    public LookupResult(MappingType type, String hostname, String value) {
        this.type = type;
        this.hostname = hostname.toLowerCase();
        this.value = value;
    }

    public MappingType getType() {
        return type;
    }

    public String getHostname() {
        return hostname;
    }

    public String getValue() {
        return value;
    }
}
