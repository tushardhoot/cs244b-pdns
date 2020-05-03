package edu.cs244b.common;

import sun.net.util.IPAddressUtil;

import java.nio.ByteBuffer;

public class CommonUtils {

    public static int ipToInt(final String ipAddress) {
        return ByteBuffer.wrap(IPAddressUtil.textToNumericFormatV4(ipAddress)).getInt();
    }

    public static String intToIp(final Integer ip) {
        return new String(ByteBuffer.allocate(4).putInt(ip).array());
    }
}
