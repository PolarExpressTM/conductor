package org.eu.polarexpress.conductor.discord.stream;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.net.http.WebSocket;
import java.util.function.Consumer;

@Getter
public class BaseMediaConnection {
    private long interval;
    private final MediaUdp mediaUdp;
    private final String guildId;
    private final String channelId;
    private final String botId;
    private WebSocket webSocket;
    private final Consumer<MediaUdp> callback;
    private final VoiceConnectionStatus status;
    private String server;
    private String token;
    private String sessionId;
    private String selfIp;
    private int selfPort;
    private String address;
    private int port;
    private int ssrc;
    private int videoSsrc;
    private int rtxSsrc;
    private String[] modes;
    private byte[] secretKey;
    private final StreamOptions streamOptions;

    @Builder
    public BaseMediaConnection(String guildId,
                               String botId,
                               String channelId,
                               StreamOptions streamOptions,
                               Consumer<MediaUdp> callback) {
        status = VoiceConnectionStatus.builder().build();
        this.streamOptions = streamOptions == null ? StreamOptions.builder()
                .width(1080)
                .height(720)
                .fps(30)
                .bitrateKbps(1000)
                .maxBitrateKbps(2500)
                .videoCodec(SupportedVideoCodec.H264)
                .readAtNativeFps(true)
                .rtcpSenderReportEnabled(true)
                .build() : streamOptions;
        mediaUdp = new MediaUdp(this);
        this.guildId = guildId;
        this.channelId = channelId;
        this.botId = botId;
        this.callback = callback;
    }

    @Builder
    @Getter
    @Setter
    public static class VoiceConnectionStatus {
        private boolean hasSession;
        private boolean hasToken;
        private boolean started;
        private boolean resuming;
    }

    @Builder
    @Getter
    @Setter
    public static class StreamOptions {
        private int width;
        private int height;
        private int fps;
        private int bitrateKbps;
        private int maxBitrateKbps;
        private boolean hardwareAcceleration;
        private SupportedVideoCodec videoCodec;
        private boolean readAtNativeFps;
        private boolean rtcpSenderReportEnabled;
    }

    public enum SupportedVideoCodec {
        H264,
        H265,
        VP8,
        VP9,
        AV1
    }
}
