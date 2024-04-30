package org.eu.polarexpress.conductor.discord.stream;

public class MediaUdp {
    private final BaseMediaConnection mediaConnection;
    private long nonce;

    public MediaUdp(BaseMediaConnection mediaConnection) {
        this.mediaConnection = mediaConnection;

    }
}
