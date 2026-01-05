package jokko.xrayalarm.detection;

import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.webhook.WebhookClient;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class PlayerLogoutListener {

    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            try {
                ServerPlayer player = handler.getPlayer();
                if (player == null) return;
                // Always log disconnects via XrayAlarm logger
                WebhookClient.logPlayerLeave(player);
                // Optionally send coordinates to chat/webhook
                if (XrayConfig.coordinatesOnLeave) {
                    WebhookClient.sendCoordinatesOnLeave(player);
                }
            } catch (Exception e) {
                // ignore
            }
        });
    }
}
