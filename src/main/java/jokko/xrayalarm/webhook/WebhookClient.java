package jokko.xrayalarm.webhook;

import jokko.xrayalarm.Xrayalarm;
import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.config.XrayConfig.OreConfig;
import jokko.xrayalarm.detection.OreTracker.OreBreakEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent; 

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookClient {

    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    private static final Map<String, ChatFormatting> COLOR_MAP = Map.ofEntries(
        Map.entry("red", ChatFormatting.RED),
        Map.entry("white", ChatFormatting.WHITE),
        Map.entry("gold", ChatFormatting.GOLD),
        Map.entry("green", ChatFormatting.GREEN),
        Map.entry("gray", ChatFormatting.GRAY),
        Map.entry("aqua", ChatFormatting.AQUA),
        Map.entry("blue", ChatFormatting.BLUE),
        Map.entry("yellow", ChatFormatting.YELLOW),
        Map.entry("bold", ChatFormatting.BOLD) // etc.
    );

    public static void sendAlert(ServerPlayer player, List<OreBreakEvent> events, OreConfig cfg) {
        String msg = cfg.alertMessage()
                .replace("{player}", player.getName().getString())
                .replace("{count}", String.valueOf(events.size()))
                .replace("{block}", cfg.blockId())
                .replace("{time}", String.valueOf(cfg.timeWindowMinutes()));

        int x, y, z;
        if (events != null && !events.isEmpty()) {
            OreBreakEvent last = events.get(events.size() - 1);
            x = last.x();
            y = last.y();
            z = last.z();
        } else {
            x = player.blockPosition().getX();
            y = player.blockPosition().getY();
            z = player.blockPosition().getZ();
        }
        msg = msg.replace("{x}", String.valueOf(x))
                 .replace("{y}", String.valueOf(y))
                 .replace("{z}", String.valueOf(z));

        if (XrayConfig.useChat) {
            // Get server reference and broadcast message
            try {
                net.minecraft.server.MinecraftServer srv = player.level().getServer();
                if (srv != null) {
                    // Send in-game chat (colored)
                    Component comp = buildComponentFromTemplate(msg);
                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        p.sendSystemMessage(comp);
                    }
                }
            } catch (Exception e) {
                // Silently fail if server is not available
            }
        }

        // Send webhook (plain text)
        if (XrayConfig.useWebhook && !XrayConfig.webhookUrl.isEmpty()) {
            try {
                String pingPrefix = "";
                if (XrayConfig.usePingRole && XrayConfig.pingRole != null && !XrayConfig.pingRole.isEmpty()) {
                    // Discord role mention format: <@&ROLE_ID>
                    pingPrefix = "<@&" + XrayConfig.pingRole + "> ";
                }
                String playerName = player.getName().getString();
                String plain = stripColorTags(msg);
                // Bold the player name for Discord
                if (playerName != null && !playerName.isEmpty()) {
                    plain = plain.replace(playerName, "**" + playerName + "**");
                }
                plain = pingPrefix + plain;
                plain = plain.replace("\"", "\\\"");
                String json = "{\"content\": \"" + plain + "\"}";

                Xrayalarm.LOGGER.info("[XrayAlarm] Preparing webhook alert. url='{}' usePingRole={} pingRole='{}'", XrayConfig.webhookUrl, XrayConfig.usePingRole, XrayConfig.pingRole);
                Xrayalarm.LOGGER.debug("[XrayAlarm] Alert JSON: {}", json);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(XrayConfig.webhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenAccept(response -> {
                            int status = response.statusCode();
                            if (status >= 200 && status < 300) {
                                Xrayalarm.LOGGER.info("[XrayAlarm] Webhook alert sent (status {})", status);
                            } else {
                                Xrayalarm.LOGGER.warn("[XrayAlarm] Webhook alert returned status {}. Body: {}", status, response.body());
                            }
                        })
                        .exceptionally(e -> {
                            Xrayalarm.LOGGER.error("[XrayAlarm] Webhook Fehler beim Senden (alert)", e);
                            return null;
                        });
            } catch (Exception ex) {
                Xrayalarm.LOGGER.error("[XrayAlarm] Exception while preparing/sending webhook alert: {}", ex.toString());
            }
        }
    }

    public static void logPlayerLeave(ServerPlayer player) {
        try {
            String msg = XrayConfig.logoutMessage;

            String dimension = player.level().dimension().toString();
            int slash = dimension.lastIndexOf('/');
            if (slash >= 0) {
                String part = dimension.substring(slash + 1).trim();
                if (part.endsWith("]")) part = part.substring(0, part.length() - 1);
                dimension = part;
            }
            String world = dimension;

            msg = msg.replace("{player}", player.getName().getString())
                     .replace("{x}", String.valueOf(player.blockPosition().getX()))
                     .replace("{y}", String.valueOf(player.blockPosition().getY()))
                     .replace("{z}", String.valueOf(player.blockPosition().getZ()))
                     .replace("{world}", world)
                     .replace("{dimension}", dimension);

            String plain = stripColorTags(msg);
            Xrayalarm.LOGGER.info("[XrayAlarm] " + plain);
        } catch (Exception e) {
            // ignore logging failures
        }
    }

    public static void sendCoordinatesOnLeave(ServerPlayer player) {
        String msg = XrayConfig.logoutMessage;

        // Get a readable dimension/world id (e.g. "minecraft:the_nether") from the ResourceKey.toString()
        String dimension = player.level().dimension().toString();
        int slash = dimension.lastIndexOf('/');
        if (slash >= 0) {
            String part = dimension.substring(slash + 1).trim();
            if (part.endsWith("]")) part = part.substring(0, part.length() - 1);
            dimension = part;
        }
        String world = dimension; // keep same for now

        msg = msg.replace("{player}", player.getName().getString())
                 .replace("{x}", String.valueOf(player.blockPosition().getX()))
                 .replace("{y}", String.valueOf(player.blockPosition().getY()))
                 .replace("{z}", String.valueOf(player.blockPosition().getZ()))
                 .replace("{world}", world)
                 .replace("{dimension}", dimension);

        if (XrayConfig.useChat) {
            try {
                net.minecraft.server.MinecraftServer srv = player.level().getServer();
                if (srv != null) {
                    Component comp = buildComponentFromTemplate(msg);
                    for (ServerPlayer p : srv.getPlayerList().getPlayers()) {
                        p.sendSystemMessage(comp);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        }

        if (XrayConfig.useWebhook && !XrayConfig.webhookUrl.isEmpty()) {
            String playerName = player.getName().getString();
            String plain = stripColorTags(msg);
            if (playerName != null && !playerName.isEmpty()) {
                plain = plain.replace(playerName, "**" + playerName + "**");
            }
            plain = plain.replace("\"", "\\\"");
            String json = "{\"content\": \"" + plain + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(XrayConfig.webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            // Log and inspect response for logout webhook
            Xrayalarm.LOGGER.debug("[XrayAlarm] Sending logout webhook to {}: {}", XrayConfig.webhookUrl, plain);
            CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        int status = response.statusCode();
                        if (status >= 200 && status < 300) {
                            Xrayalarm.LOGGER.info("[XrayAlarm] Webhook logout sent (status {})", status);
                        } else {
                            Xrayalarm.LOGGER.warn("[XrayAlarm] Webhook logout returned status {}. Body: {}", status, response.body());
                        }
                    })
                    .exceptionally(e -> { Xrayalarm.LOGGER.error("[XrayAlarm] Webhook Fehler beim Senden (logout)", e); return null; });
        }
    }

    // Build a Component from text with <color>...</color> tags
    private static MutableComponent buildComponentFromTemplate(String template) {
        Pattern pattern = Pattern.compile("(?s)(?:<(\\w+)>(.*?)</\\1>)|([^<]+)");
        Matcher m = pattern.matcher(template);
        MutableComponent result = Component.literal("");
        while (m.find()) {
            if (m.group(1) != null) {
                String tag = m.group(1).toLowerCase();
                String text = m.group(2);
                ChatFormatting fmt = COLOR_MAP.getOrDefault(tag, ChatFormatting.WHITE);
                result.append(Component.literal(text).withStyle(fmt));
            } else {
                result.append(Component.literal(m.group(3)));
            }
        }
        return result;
    }

    // Remove color tags for webhooks
    private static String stripColorTags(String s) {
        return s.replaceAll("</?\\w+>", "");
    }
}
