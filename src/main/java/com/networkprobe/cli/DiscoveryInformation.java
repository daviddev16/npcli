package com.networkprobe.cli;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public final class DiscoveryInformation {

    public static final DiscoveryInformation FAILED = new DiscoveryInformation("?", -1);

    public static final String   LAST_DISCOVERY_ADDRESS     = "last.discovery.address";
    public static final String   LAST_DISCOVERY_TIMESTAMP   = "last.discovery.timestamp";

    private final String address;
    private final long timestamp;
    private int attempts;

    public DiscoveryInformation(String address, long timestamp) {
        this.address = address;
        this.timestamp = timestamp;
        this.attempts = 0;
    }

    public DiscoveryInformation(JSONObject jsonObject) {
        this.address = jsonObject.getString(LAST_DISCOVERY_ADDRESS);
        this.timestamp = jsonObject.getLong(LAST_DISCOVERY_TIMESTAMP);
        this.attempts = 0;
    }

    public InetAddress getAsInetAddress() throws UnknownHostException {
        return InetAddress.getByName(address);
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public int getAttempts() {
        return attempts;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj instanceof DiscoveryInformation)
            return ((DiscoveryInformation)obj).getAddress().equals(address);

        return false;
    }

    public String toJsonString() {
        return new JSONObject()
                .put(LAST_DISCOVERY_ADDRESS, address)
                .put(LAST_DISCOVERY_TIMESTAMP, timestamp)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address);
    }

}
