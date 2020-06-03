package edu.cs244b.mappings;

import edu.cs244b.common.CommonUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RecursiveMappingTest {

    @Test
    public void handlesNoMappingDB() {
        RecursiveMapping mapping = new RecursiveMapping();

        assertNull(mapping.lookup(CommonUtils.rDNSForm("google.com")));
    }

    @Test
    public void returnsNullIfNoMapping() {
        RecursiveMapping mapping = new RecursiveMapping();
        mapping.pushMapping(
                CommonUtils.rDNSForm("ads.facebook.com"),
                new LookupResult(LookupResult.MappingType.DIRECT, "ads.facebook.com", "1.2.3.4")
        );

        assertNull(mapping.lookup(CommonUtils.rDNSForm("google.com")));
        assertNull(mapping.lookup(CommonUtils.rDNSForm("facebook.com")));
        assertNull(mapping.lookup(CommonUtils.rDNSForm("usa.gov")));
    }

    @Test
    public void returnsDirectMapping() {
        RecursiveMapping mapping = new RecursiveMapping();
        mapping.pushMapping(
                CommonUtils.rDNSForm("facebook.com"),
                new LookupResult(LookupResult.MappingType.DIRECT, "facebook.com", "1.2.3.4")
        );

        LookupResult result = mapping.lookup(CommonUtils.rDNSForm("facebook.com"));
        assertNotNull(result);
        assertEquals(LookupResult.MappingType.DIRECT, result.type);
        assertEquals("1.2.3.4", result.value);
    }

    @Test
    public void returnsIndirectMapping() {
        RecursiveMapping mapping = new RecursiveMapping();
        mapping.pushMapping(
                CommonUtils.rDNSForm("facebook.com"),
                new LookupResult(LookupResult.MappingType.INDIRECT, "facebook.com", "bigtech")
        );

        LookupResult result = mapping.lookup(CommonUtils.rDNSForm("facebook.com"));
        assertNotNull(result);
        assertEquals(LookupResult.MappingType.INDIRECT, result.type);
        assertEquals("bigtech", result.value);
    }

    @Test
    public void returnsMostSpecificMapping() {
        RecursiveMapping mapping = new RecursiveMapping();
        mapping.pushMapping(
                CommonUtils.rDNSForm("com"),
                new LookupResult(LookupResult.MappingType.INDIRECT, "com", "icann")
        );
        mapping.pushMapping(
                CommonUtils.rDNSForm("facebook.com"),
                new LookupResult(LookupResult.MappingType.INDIRECT, "facebook.com", "bigtech")
        );

        LookupResult result = mapping.lookup(CommonUtils.rDNSForm("facebook.com"));
        assertNotNull(result);
        assertEquals("bigtech", result.value);
    }

    @Test
    public void returnsDefaultMappingIfNoExactMatch() {
        RecursiveMapping mapping = new RecursiveMapping(
                new LookupResult(LookupResult.MappingType.INDIRECT, "", "fallback")
        );
        mapping.pushMapping(
                CommonUtils.rDNSForm("com"),
                new LookupResult(LookupResult.MappingType.INDIRECT, "com", "icann")
        );
        mapping.pushMapping(
                CommonUtils.rDNSForm("facebook.com"),
                new LookupResult(LookupResult.MappingType.INDIRECT, "facebook.com", "bigtech")
        );

        LookupResult result = mapping.lookup(CommonUtils.rDNSForm("google.com"));
        assertNotNull(result);
        assertEquals("icann", result.value);

        result = mapping.lookup(CommonUtils.rDNSForm("google.org"));
        assertNotNull(result);
        assertEquals("fallback", result.value);
    }
}