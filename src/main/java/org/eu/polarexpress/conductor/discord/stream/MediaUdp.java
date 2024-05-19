package org.eu.polarexpress.conductor.discord.stream;

import lombok.Getter;
import lombok.Setter;

@Getter
public class MediaUdp {
    private long nonce;
    @Setter
    private boolean ready;

    public MediaUdp() {
    }
}
