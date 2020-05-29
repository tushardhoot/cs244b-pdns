package edu.cs244b.mappings;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class DNSMappingStoreTest {
    @Test
    public void lookupsDNS() {
        DNSMappingStore store = new DNSMappingStore();
        store.setup();

        LookupResult result = store.lookup("stanford.edu");
        assertEquals(result.value, "171.67.215.200");
    }
}
