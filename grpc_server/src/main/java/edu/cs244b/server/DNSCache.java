package edu.cs244b.server;

import com.google.common.annotations.VisibleForTesting;
import edu.cs244b.common.CommonUtils;
import edu.cs244b.common.DNSRecord;
import edu.cs244b.common.DNSRecordP2P;
import org.joda.time.DateTimeConstants;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class DNSCache {
    private Logger logger;

    private final LinkedHashMap<String, DNSInfo> cache;
    private final int capacity;

    private static final double DNS_CACHE_CLEANUP_EXPIRED_ENTRIES_RARE_PCT = 0.01;

    public DNSCache(final int capacity, final Logger logger) {
        this.logger = logger;
        this.capacity = capacity;
        this.cache = new LinkedHashMap<>(1<<10, 0.75F, true);
    }

    public synchronized void load(final String inputFile) {
        //TODO
    }

    public synchronized int store(final String outFile) {
        //TODO
        return 0;
    }

    public synchronized DNSInfo get(final String hostName) {
        // since this is a access-order linked hash map, get pushes this entry to the end of the map
        final DNSInfo dnsInfo = cache.get(hostName);
        if (dnsInfo != null && dnsInfo.expiryTime >= System.currentTimeMillis()) {
            return dnsInfo;
        }

        cache.remove(hostName);
        return null;
    }

    public void registerDnsRecord(final DNSRecordP2P dnsRecordP2P) {
        if (ServerUtils.isDNSRecordValid(dnsRecordP2P) &&
                dnsRecordP2P.getExpiryTime() > System.currentTimeMillis()) {   /* ttl is valid */
            final DNSRecord dnsRecord = dnsRecordP2P.getDnsRecord();
            put(dnsRecord.getHostName(), dnsRecord.getIpAddresses(0), dnsRecordP2P.getExpiryTime());
        }
    }

    @VisibleForTesting
    synchronized void put(final String hostName, final String ipAddress, final long ttl) {
        cache.put(hostName, new DNSInfo(ipAddress, ttl));
        if (cache.size() <= capacity) {
            return;
        }

        // need cleanup
        cleanup();
    }

    private synchronized void cleanup() {
        if (cache.size() == 0) {
            return;
        }

        /* A nice way to do cleanup:
         * Whenever cleanup limit is breached, iterate through all the entries once in ten thousand times
         * Iterating over all entries to clear expired entries which have piled up with time
         * Remove all those entries which have expired
         * Remove those which are about to expire (as iterating the loop can be an expensive operation every few mins)
         * If no entries are expired when iteration is over, remove the least used entry
         */
        try {
            if (CommonUtils.rare(DNS_CACHE_CLEANUP_EXPIRED_ENTRIES_RARE_PCT)) {  //  1-of-10000
                Iterator<Map.Entry<String, DNSInfo>> iterator = cache.entrySet().iterator();
                // expire all those who fall in the next 10 min window
                final long expiryTimeToConsider = System.currentTimeMillis() + (10 * DateTimeConstants.MILLIS_PER_MINUTE);
                while (iterator.hasNext()) {
                    final Map.Entry<String, DNSInfo> entry = iterator.next();
                    if (candidateToBeRemoved(entry.getValue(), expiryTimeToConsider)) {
                        iterator.remove();
                    }
                }
            }

            // return if the cleanup based on ttl worked
            if (cache.size() <= capacity) {
                return;
            }

            //Remove the least used entry from cache if all entries are valid
            final Map.Entry<String, DNSInfo> it = cache.entrySet().iterator().next();
            cache.remove(it.getKey());
        } catch (Exception ex) {
            logger.error("exception while removing entry from cache", ex);
        }
    }

    private boolean candidateToBeRemoved(final DNSInfo dnsInfo, final long expiryTimeToConsider) {
        return dnsInfo == null || dnsInfo.expiryTime <= expiryTimeToConsider;
    }

    @VisibleForTesting
    LinkedHashMap<String, DNSInfo> getCache() {
        return cache;
    }

    static class DNSInfo {
        public final int ip;
        public final long expiryTime;

        public DNSInfo(final String ipAddress, final long ttl) {
            this.ip = CommonUtils.ipToInt(ipAddress);
            this.expiryTime = ttl;
        }
    }
}
