package edu.cs244b.mappings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ManagerTest {

    JSONMappingStore jsonStore(String rawJSON) throws IOException {
        File temp = File.createTempFile("tmp-", ".json");

        BufferedWriter bw = new BufferedWriter(new FileWriter(temp));
        bw.write(rawJSON);
        bw.close();

        return new JSONMappingStore(temp.toURL());
    }

    @Test
    public void handlesNoMappingDB() throws IOException {
        Manager manager = new Manager(jsonStore("{\"entries\": []}"));

        assertNull(manager.lookupHostname("google.com"));
    }

    @Test
    public void returnsNullIfNoMapping() throws IOException {
        Manager manager = new Manager(jsonStore(
    "{" +
            "   \"entries\": [" +
            "        { \"hostName\": \"ads.facebook.com\", \"ipAddress\": \"1.2.3.4\" }" +
            "   ]" +
            "}"
        ));

        assertNull(manager.lookupHostname("google.com"));
        assertNull(manager.lookupHostname("facebook.com"));
        assertNull(manager.lookupHostname("usa.gov"));
    }

    @Test
    public void returnsDirectMapping() throws IOException {
        Manager manager = new Manager(jsonStore(
    "{" +
            "   \"entries\": [" +
            "        { \"hostName\": \"facebook.com\", \"ipAddress\": \"1.2.3.4\" }" +
            "   ]" +
            "}"
        ));

        LookupResult result = manager.lookupHostname("facebook.com");
        assertNotNull(result);
        assertEquals(LookupResult.MappingType.DIRECT, result.type);
        assertEquals("1.2.3.4", result.value);
    }

    @Test
    public void returnsIndirectMapping() throws IOException {
        Manager manager = new Manager(jsonStore(
    "{" +
            "   \"entries\": [" +
            "        { \"hostName\": \"facebook.com\", \"resource\": \"bigtech\" }" +
            "   ]" +
            "}"
        ));

        LookupResult result = manager.lookupHostname("facebook.com");
        assertNotNull(result);
        assertEquals(LookupResult.MappingType.INDIRECT, result.type);
        assertEquals("bigtech", result.value);
    }

    @Test
    public void returnsMostSpecificMapping() throws IOException {
        Manager manager = new Manager(jsonStore(
    "{" +
            "   \"entries\": [" +
            "        { \"hostName\": \"com\", \"resource\": \"icann\" }," +
            "        { \"hostName\": \"facebook.com\", \"resource\": \"bigtech\" }" +
            "   ]" +
            "}"
        ));

        LookupResult result = manager.lookupHostname("facebook.com");
        assertNotNull(result);
        assertEquals("bigtech", result.value);
    }

    @Test
    public void returnsDefaultMappingIfNoExactMatch() throws IOException {
        Manager manager = new Manager(jsonStore(
                "{" +
                        "   \"entries\": [" +
                        "        { \"hostName\": \"com\", \"resource\": \"icann\" }," +
                        "        { \"hostName\": \"facebook.com\", \"resource\": \"bigtech\" }" +
                        "   ]" +
                        "}"
        ));

        LookupResult result = manager.lookupHostname("google.com");
        assertNotNull(result);
        assertEquals("icann", result.value);
    }
}