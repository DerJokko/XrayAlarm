package jokko.xrayalarm.detection;

import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.webhook.WebhookClient;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public class OreTracker {

    private static final Map<UUID, Map<String, List<OreBreakEvent>>> playerHistory = new HashMap<>();

    public static void handleOreBreak(ServerPlayer player, Block block) {
        if (!XrayConfig.enabled) return;

        @SuppressWarnings("deprecation")
        String blockId = block.builtInRegistryHolder().key().toString();
        XrayConfig.OreConfig cfg = XrayConfig.trackedBlocks.get(blockId);
        if (cfg == null) return;

        UUID uuid = player.getUUID();
        playerHistory.putIfAbsent(uuid, new HashMap<>());
        Map<String, List<OreBreakEvent>> blocks = playerHistory.get(uuid);
        blocks.putIfAbsent(blockId, new ArrayList<>());
        List<OreBreakEvent> events = blocks.get(blockId);

        long now = System.currentTimeMillis();
        events.add(new OreBreakEvent(now, player, blockId));

        // Alte Einträge rauswerfen
        long cutoff = now - cfg.timeWindowMinutes() * 60L * 1000L;
        events.removeIf(e -> e.timestamp() < cutoff);

        if (events.size() >= cfg.alertThreshold()) {
            WebhookClient.sendAlert(player, events, cfg);
            events.clear();
        }
    }

    public record OreBreakEvent(long timestamp, ServerPlayer player, String blockId) {}
}
