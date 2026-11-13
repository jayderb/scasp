package com.scasp.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Stage 3 requirement 3: InetAddress host/address information. */
public final class InetAddressDemo {
    private InetAddressDemo() {}
    public static List<String> describe(String... hosts) {
        List<String> out = new ArrayList<>();
        try {
            InetAddress local = InetAddress.getLocalHost();
            out.add("Local host: name=" + local.getHostName() + " address=" + local.getHostAddress());
        } catch (UnknownHostException e) { out.add("Local host could not be resolved: " + e.getMessage()); }
        InetAddress loop = InetAddress.getLoopbackAddress();
        out.add("Loopback: name=" + loop.getHostName() + " address=" + loop.getHostAddress() + " isLoopback=" + loop.isLoopbackAddress());
        for (String h : hosts) {
            try {
                for (InetAddress a : InetAddress.getAllByName(h))
                    out.add("Resolve " + h + ": " + a.getHostAddress() + " (" + (a.getAddress().length == 4 ? "IPv4" : "IPv6")
                            + ", siteLocal=" + a.isSiteLocalAddress() + ", loopback=" + a.isLoopbackAddress() + ")");
            } catch (UnknownHostException e) { out.add("Resolve " + h + ": unknown host (handled safely)"); }
        }
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces()))
                if (ni.isUp()) for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    out.add("Interface " + ni.getName() + ": " + a.getHostAddress());
        } catch (Exception e) { out.add("Interfaces unavailable: " + e.getMessage()); }
        return out;
    }
    public static void main(String[] args) {
        describe(args.length > 0 ? args : new String[]{"localhost", "no-such-host.invalid"}).forEach(System.out::println);
    }
}
