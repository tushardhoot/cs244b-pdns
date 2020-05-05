package edu.cs244b.mappings;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import edu.cs244b.common.NullOrEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JSONMappingStore implements Manager.MappingStore {

    private final URL path;

    public JSONMappingStore(URL path) {
        this.path = path;
    }

    public Set<LookupResult> loadMappings() {
        InputStream inputStream;
        try {
            inputStream = path.openStream();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not open file path %s", path.toString()));
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Gson gson = new Gson();
        JSONMappings jsonMappings = gson.fromJson(reader, JSONMappings.class);

        return jsonMappings.entries.stream().map(JSONMappingStore::validResultOrThrow).collect(Collectors.toSet());
    }

    private static LookupResult validResultOrThrow(Entry entry) {
        if (NullOrEmpty.isTrue(entry.hostName)) {
            throw new RuntimeException("mapping with no hostname");
        }

        if (NullOrEmpty.isTrue(entry.ipAddress) && NullOrEmpty.isTrue(entry.resource)) {
            throw new RuntimeException(String.format("no ipAddress or resource defined for hostname %s", entry.hostName));
        }

        if (NullOrEmpty.isFalse(entry.ipAddress) && NullOrEmpty.isFalse(entry.resource)) {
            throw new RuntimeException(String.format("both ipAddress and resource defined for hostname %s", entry.hostName));
        }

        LookupResult result;
        if (NullOrEmpty.isFalse(entry.ipAddress)) {
            result = new LookupResult(LookupResult.MappingType.DIRECT, entry.hostName, entry.ipAddress);
        } else {
            result = new LookupResult(LookupResult.MappingType.INDIRECT, entry.hostName, entry.resource);
        }
        return result;
    }

    private static class Entry {
        private String hostName;
        private String ipAddress;
        private String resource;
    }

    private static class JSONMappings {
        private List<Entry> entries;
    }
}
