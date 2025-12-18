package jokko.xrayalarm.detection;

import jokko.xrayalarm.config.XrayConfig;
import jokko.xrayalarm.Xrayalarm;
import jokko.xrayalarm.webhook.WebhookClient;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.*;

public class OreTracker {

    private static final Map<UUID, Map<String, List<OreBreakEvent>>> playerHistory = new HashMap<>();

    public static void handleOreBreak(ServerPlayer player, Block block) {
        Xrayalarm.LOGGER.info("[OreTracker] Handling ore break for player {} and block {}",
            player.getName().getString(),
            block.toString()
        );
        
        if (!XrayConfig.enabled) {
            Xrayalarm.LOGGER.info("[OreTracker] XRayAlarm is disabled, ignoring ore break");
            return;
        }

        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        Xrayalarm.LOGGER.info("[OreTracker] Block ID resolved to: {}", blockId);
        
        XrayConfig.OreConfig cfg = XrayConfig.trackedBlocks.get(blockId);
        if (cfg == null) {
            Xrayalarm.LOGGER.info("[OreTracker] Block {} is not in tracked blocks list. Available: {}", blockId, XrayConfig.trackedBlocks.keySet());
        }

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

        int remaining = cfg.alertThreshold() - events.size();
        Xrayalarm.LOGGER.info("[OreTracker] {} broke {} - Count: {}/{} ({} more to threshold)",
            player.getName().getString(),
            blockId,
            events.size(),
            cfg.alertThreshold(),
            remaining > 0 ? remaining : 0
        );

        if (events.size() >= cfg.alertThreshold()) {
            Xrayalarm.LOGGER.warn("[OreTracker] ALERT! {} exceeded threshold for {} with {} breaks!", 
                player.getName().getString(), 
                blockId, 
                events.size()
            );
            WebhookClient.sendAlert(player, events, cfg);
            events.clear();
        }
    }

    public record OreBreakEvent(long timestamp, ServerPlayer player, String blockId) {}
}
