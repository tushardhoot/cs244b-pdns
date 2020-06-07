package edu.cs244b.common;

import sun.net.util.IPAddressUtil;

import java.io.File;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CommonUtils {

    public static int ipToInt(final String ipAddress) {
        return ByteBuffer.wrap(IPAddressUtil.textToNumericFormatV4(ipAddress)).getInt();
    }

    public static String intToIp(final Integer ip) throws UnknownHostException {
        final byte[] b = new byte[4];
        b[0] = (byte) ((ip & 0xFF000000) >>> 24);
        b[1] = (byte) ((ip & 0x00FF0000) >>> 16);
        b[2] = (byte) ((ip & 0x0000FF00) >>> 8);
        b[3] = (byte) (ip & 0x000000FF);
        return InetAddress.getByAddress(null, b).getHostAddress();
    }

    public static List<String> rDNSForm(String hostname) {
        // Arrays.asList creates an immutable List, so we need to copy it over.
        List<String> parts = new LinkedList<>(Arrays.asList(hostname.split("\\.")));
        Collections.reverse(parts);
        return parts;
    }

    public static URL getUrl(final String fileName) {
        URL url;
        try {
            url = new File(fileName).toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("Malformed Url: %s", fileName));
        }
        return url;
    }

    public static boolean rare(final double rarePercent) {
        return (Math.random() * 100 < rarePercent);
    }
}
