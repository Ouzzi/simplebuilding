package com.simplebuilding.modules.simpleoptrender;

import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * M1 fast IP ping, own technique (Fast IP Ping describes "no reverse DNS lookup for literal IP addresses"). Vanilla
 * resolves the typed host with {@code InetAddress.getByName} and later asks the result for its host name (block-list
 * check), which for a literal IP is a reverse DNS query; it also asks DNS for an SRV record of the literal. An address
 * built with {@code InetAddress.getByAddress(literal, bytes)} already carries its host name, so nothing is looked up.
 */
public final class FastIpPing {
    private FastIpPing() {}

    /** The literal as an address that knows its host name, or null when {@code host} is no IP literal (no DNS either way). */
    public static InetAddress literal(String host) {
        if (host == null) return null;
        String h = host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;
        if (!InetAddresses.isInetAddress(h)) return null;
        try {
            return InetAddress.getByAddress(h, InetAddresses.forString(h).getAddress());
        } catch (UnknownHostException | IllegalArgumentException e) {
            return null;
        }
    }
}
