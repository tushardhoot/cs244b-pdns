package edu.cs244b.common;

import sun.net.util.IPAddressUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CommonUtils {

    public static int ipToInt(final String ipAddress) {
        return ByteBuffer.wrap(IPAddressUtil.textToNumericFormatV4(ipAddress)).getInt();
    }

    public static String intToIp(final Integer ip) {
        return new String(ByteBuffer.allocate(4).putInt(ip).array());
    }

    public static List<String> rDNSForm(String hostname) {
        // Arrays.asList creates an immutable List, so we need to copy it over.
        List<String> parts = new LinkedList<>(Arrays.asList(hostname.split("\\.")));
        Collections.reverse(parts);
        return parts;
    }
}
