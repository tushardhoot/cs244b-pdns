package edu.cs244b.server;

import com.google.protobuf.util.JsonFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

public class ServerUtils {

    /**
     * Gets the default mapping file from classpath.
     */
    public static URL getDefaultLookupFile() {
        return DomainLookupServer.class.getResource("domain_lookup_db.json");
    }

    /**
     * Parses the JSON input file containing the list of hostName to ipAddress mappings.
     */
    public static List<DomainLookupMapping.Entry> parseDomainMappings(URL file) throws IOException {
        InputStream input = file.openStream();
        try {
            Reader reader = new InputStreamReader(input, Charset.forName("UTF-8"));
            try {
                DomainLookupMapping.Builder mapping = DomainLookupMapping.newBuilder();
                JsonFormat.parser().merge(reader, mapping);
                return mapping.getEntriesList();
            } finally {
                reader.close();
            }
        } finally {
            input.close();
        }
    }
}
