package edu.cs244b.server;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.protobuf.util.JsonFormat;

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

    public static int ZERO = 0;
    public static int ONE = 1;

    /**
     * Gets the default mapping file from classpath.
     */
    public static URL defaultJSONLookupDB() {
        return DomainLookupServer.class.getResource("domain_lookup_db.json");
    }

    private static URL defaultPeerList() {
        return DomainLookupServer.class.getResource("peers.json");
    }

    /**
     * Parses the JSON input file containing the list of hostName to ipAddress mappings.
     */
    public static Map<String, Peer> loadPeers() {
        InputStream inputStream;
        final URL path = defaultPeerList();
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

    public static ServerOperationalConfig getServerOpConfig() {
        InputStream inputStream;
        final URL path = getServerOperationalConfig();
        try {
            inputStream = path.openStream();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Could not open file path %s", path.toString()));
        }

        final ServerOperationalConfig.Builder builder = ServerOperationalConfig.newBuilder();
        try {
            final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            JsonFormat.parser().ignoringUnknownFields().merge(reader, builder);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Could not parse and merge server op config %s", path.toString()));
        }
        return builder.build();
    }

    private static URL getServerOperationalConfig() {
        return DomainLookupServer.class.getResource("server.op.config");
    }

    static class Peers {
        List<Peer> hosts;
    }
}
