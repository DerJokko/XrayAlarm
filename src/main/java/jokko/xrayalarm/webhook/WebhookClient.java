package jokko.xrayalarm.webhook;

import jokko.xrayalarm.Xrayalarm;
import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.config.XrayConfig.OreConfig;
import jokko.xrayalarm.detection.OreTracker.OreBreakEvent;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class WebhookClient {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void sendAlert(ServerPlayer player, List<OreBreakEvent> events, OreConfig cfg) {
        String msg = cfg.alertMessage()
                .replace("{player}", player.getName().getString())
                .replace("{count}", String.valueOf(events.size()))
                .replace("{block}", cfg.blockId())
                .replace("{time}", String.valueOf(cfg.timeWindowMinutes()));

        if (XrayConfig.useChat) {
            // Get server reference and broadcast message
            try {
                net.minecraft.server.MinecraftServer srv = player.level().getServer();
                if (srv != null) {
                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
                    }
                }
            } catch (Exception e) {
                // Silently fail if server is not available
            }
        }

        if (XrayConfig.useWebhook && !XrayConfig.webhookUrl.isEmpty()) {
            String json = "{\"content\": \"" + msg.replace("\"", "\\\"") + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(XrayConfig.webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(e -> { Xrayalarm.LOGGER.error("Webhook Fehler", e); return null; });
        }
    }
}
