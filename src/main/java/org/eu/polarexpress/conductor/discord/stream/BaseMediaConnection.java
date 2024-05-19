package org.eu.polarexpress.conductor.discord.stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.eu.polarexpress.conductor.discord.stream.dto.HeartbeatDto;
import org.eu.polarexpress.conductor.discord.stream.dto.OpDto;
import org.eu.polarexpress.conductor.discord.stream.dto.ReadyDto;
import org.eu.polarexpress.conductor.discord.stream.dto.SessionDto;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Getter
public abstract class BaseMediaConnection {
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final Logger logger;
    private ScheduledFuture<?> interval;
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
    private final ObjectMapper mapper;

    public BaseMediaConnection(String guildId,
                               String botId,
                               String channelId,
                               StreamOptions streamOptions,
                               Consumer<MediaUdp> callback,
                               ObjectMapper mapper) {
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
        mediaUdp = new MediaUdp();
        this.guildId = guildId;
        this.channelId = channelId;
        this.botId = botId;
        this.callback = callback;
        this.mapper = mapper;
        logger = getLogger();
    }

    public void start(HttpClient client) {
        if (!status.hasSession || !status.hasToken) {
            return;
        }
        if (status.started) {
            return;
        }
        status.setStarted(true);
        try {
            var wss = String.format("wss://%s/?v=7", server);
            webSocket = client.newWebSocketBuilder().buildAsync(URI.create(wss), new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    WebSocket.Listener.super.onOpen(webSocket);
                    if (status.resuming) {
                        status.setResuming(false);
                        resume();
                    } else {
                        identify();
                    }
                }
                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        var packet = mapper.readValue((String)data, OpDto.class);
                        logger.info("Packet received: {}", packet);
                        if (packet.code() == VoiceOpCodes.READY.getOpCode()) {
                            var readyPacket = mapper.readValue((String)data, ReadyDto.class);
                            handleReady(readyPacket);
                            sendVoice();
                            setVideoStatus(false);
                        } else if (packet.code() == VoiceOpCodes.HELLO.getOpCode()) {
                            var heartbeatPacket = mapper.readValue((String)data, HeartbeatDto.class);
                            setupHeartbeat(heartbeatPacket);
                        } else if (packet.code() == VoiceOpCodes.SELECT_PROTOCOL_ACK.getOpCode()) {
                            var sessionPacket = mapper.readValue((String)data, SessionDto.class);
                            handleSession(sessionPacket);
                        } else if (packet.code() == VoiceOpCodes.SPEAKING.getOpCode()) {
                        } else if (packet.code() == VoiceOpCodes.HEARTBEAT_ACK.getOpCode()) {
                        } else if (packet.code() == VoiceOpCodes.RESUMED.getOpCode()) {
                            status.setStarted(true);
                            mediaUdp.setReady(true);
                        } else if (packet.code() >= 4000) {
                            logger.error("Error websocket: {}", data);
                        } else {
                            logger.debug("Unhandled voice event: {} - {}", packet.code(), data);
                        }
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                    return WebSocket.Listener.super.onText(webSocket, data, last);
                }
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    var started = status.started;
                    status.setStarted(false);
                    mediaUdp.setReady(false);
                    if ((statusCode == 4015 || statusCode < 4000) && started) {
                        status.setResuming(true);
                        start(client);
                    }
                    return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                }
                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    WebSocket.Listener.super.onError(webSocket, error);
                    logger.error(error.getLocalizedMessage());
                }
            }).get();
        } catch (ExecutionException | InterruptedException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    public void handleReady(ReadyDto dto) {
        ssrc = dto.data().ssrc();
        address = dto.data().ip();
        port = dto.data().port();
        modes = dto.data().modes().toArray(new String[0]);
        videoSsrc = ssrc + 1;
        rtxSsrc = ssrc + 2;
        // mediaUdp.audioPacketizer.setSsrc(ssrc);
        // mediaUdp.videoPacketizer.setSsrc(videoSsrc);
    }

    public void handleSession(SessionDto dto) {
        secretKey = dto.data().secretKey().getBytes();
        mediaUdp.setReady(true);
    }

    public void setupHeartbeat(HeartbeatDto dto) {
        if (interval != null && !interval.isCancelled()) {
            interval.cancel(true);
        }
        interval = executorService.scheduleAtFixedRate(() -> {
            sendOpCode(VoiceOpCodes.HEARTBEAT.getOpCode(), "42069");
        }, 0, dto.data().interval(), TimeUnit.MILLISECONDS);
    }

    public void sendOpCode(int code, String data) {
        try {
            webSocket.sendText(mapper.writeValueAsString(OpDto.builder().code(code).data(data).build()), true);
        } catch (JsonProcessingException exception) {
            logger.error("sendOpCode: {}", exception.getLocalizedMessage());
        }
    }

    public void identify() {
        sendOpCode(VoiceOpCodes.IDENTIFY.getOpCode(), String.format("""
                {
                server_id: %s,
                user_id: %s,
                session_id: %s,
                token: %s,
                video: true,
                streams: [
                    { type:"screen", rid:"100", quality:100 }
                ]
                }
                """,
                getServerId(),
                botId,
                sessionId,
                token)
        );
    }

    public void resume() {
        sendOpCode(VoiceOpCodes.RESUME.getOpCode(), String.format("""
                {
                server_id: %s,
                session_id: %s,
                token: %s,
                }
                """,
                getServerId(),
                sessionId,
                token)
        );
    }

    public void setProtocols(String ip, int port) {
        sendOpCode(VoiceOpCodes.SELECT_PROTOCOL.getOpCode(), "");
    }

    public void setVideoStatus(boolean video) {
        sendOpCode(VoiceOpCodes.VIDEO.getOpCode(), "");
    }

    public void setSpeaking(boolean speaking) {
        sendOpCode(VoiceOpCodes.SPEAKING.getOpCode(), "");
    }

    public void sendVoice() {
    }

    public abstract String getServerId();
    protected abstract Logger getLogger();

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
