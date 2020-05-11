package edu.cs244b.server;

import edu.cs244b.common.CommonUtils;
import org.joda.time.DateTimeConstants;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Set;

@RunWith(JUnit4.class)
public class DNSCacheTest {
    private static final Logger logger = LoggerFactory.getLogger(DNSCacheTest.class);

    @Test
    public void testBasic() throws UnknownHostException {
        long expiryTime = System.currentTimeMillis() + (5 * DateTimeConstants.MILLIS_PER_MINUTE);
        final DNSCache dnsCache = new DNSCache(5, logger);
        dnsCache.put("facebook.com", "1.1.1.1", expiryTime);
        dnsCache.put("amazon.com", "1.1.1.2", expiryTime);
        dnsCache.put("google.com", "1.1.1.3", expiryTime);
        dnsCache.put("walmart.com", "1.1.1.4", expiryTime);
        dnsCache.put("usa.gov", "1.1.1.5", expiryTime);

        Set<String> hostnames = dnsCache.getCache().keySet();
        Assert.assertEquals("[facebook.com, amazon.com, google.com, walmart.com, usa.gov]", hostnames.toString());

        final DNSCache.DNSInfo amazonDNSInfo = dnsCache.get("amazon.com");
        // Test DNS correctness
        Assert.assertEquals("1.1.1.2", CommonUtils.intToIp(amazonDNSInfo.ip));
        // Test access-order
        Assert.assertEquals("[facebook.com, google.com, walmart.com, usa.gov, amazon.com]", hostnames.toString());

        // Test cleanup
        expiryTime += 15 * DateTimeConstants.MILLIS_PER_MINUTE;
        dnsCache.put("nasa.gov", "1.1.1.6", expiryTime);
        dnsCache.put("covid19india.org", "1.1.1.7", expiryTime);
        hostnames = dnsCache.getCache().keySet();
        Assert.assertEquals("[nasa.gov, covid19india.org]", hostnames.toString());

        // Test cleanup again
        expiryTime += 15 * DateTimeConstants.MILLIS_PER_MINUTE;
        dnsCache.put("linkedin.com", "1.1.1.8", expiryTime);
        dnsCache.put("twitter.com", "1.1.1.8", expiryTime);
        dnsCache.put("leetcode.com", "1.1.1.8", expiryTime);
        dnsCache.put("github.com", "1.1.1.9", expiryTime);
        dnsCache.put("stanford.edu", "1.1.1.10", expiryTime);
        dnsCache.put("quora.com", "1.1.1.11", expiryTime);
        hostnames = dnsCache.getCache().keySet();
        Assert.assertEquals("[twitter.com, leetcode.com, github.com, stanford.edu, quora.com]", hostnames.toString());
    }
}
