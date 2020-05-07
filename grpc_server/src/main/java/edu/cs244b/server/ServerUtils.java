package edu.cs244b.server;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ServerUtils {

    /**
     * Gets the default mapping file from classpath.
     */
    public static URL defaultJSONLookupDB() {
        return DomainLookupServer.class.getResource("domain_lookup_db.json");
    }

    public static URL defaultPeerList() {
        return DomainLookupServer.class.getResource("peers.json");
    }

    /**
     * Parses the JSON input file containing the list of hostName to ipAddress mappings.
     */
    public static Map<String, Peer> loadPeers(URL path) throws IOException {
        InputStream inputStream;
        try {
            inputStream = path.openStream();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not open file path %s", path.toString()));
        }

        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Gson gson = new Gson();
        Peers peers = gson.fromJson(reader, Peers.class);

        return peers.hosts.stream().collect(Collectors.toMap(p -> p.getName(), Function.identity()));
    }

    class Peers {
        List<Peer> hosts;
    }
}
