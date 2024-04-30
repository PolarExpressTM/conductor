package org.eu.polarexpress.conductor.discord.stream;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BasePacket {
    private int ssrc;
    private final int payloadType;
    private int mtu;
    private int sequence;
    private long timestamp;
    private long totalBytes;
    private int totalPackets;
    private int prevTotalPackets;
    private long lastPacketTime;
    private long srInterval;
    private final MediaUdp mediaUdp;
    private final boolean extensionEnabled;

    public BasePacket(MediaUdp connection, int payloadType, boolean extensionEnabled) {
        this.mediaUdp = connection;
        this.payloadType = payloadType;
        this.extensionEnabled = extensionEnabled;
        this.mtu = 1200;
        this.srInterval = 512;
    }

    public void sendFrame(byte[] frame) {
        lastPacketTime = System.currentTimeMillis();
    }
}
