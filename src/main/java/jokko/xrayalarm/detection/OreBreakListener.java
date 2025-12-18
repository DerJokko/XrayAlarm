package jokko.xrayalarm.detection;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import jokko.xrayalarm.Xrayalarm;

public class OreBreakListener {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;
            net.minecraft.world.level.block.Block block = state.getBlock();

            // Check if it's an ore block by name
            String blockName = block.toString().toLowerCase();
            if (blockName.contains("ore")) {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
                Xrayalarm.LOGGER.info("[{}] {} broke {} at ({}, {}, {})", 
                    timestamp, 
                    player.getName().getString(), 
                    blockName, 
                    pos.getX(), 
                    pos.getY(), 
                    pos.getZ()
                );
                OreTracker.handleOreBreak((net.minecraft.server.level.ServerPlayer) player, block);
            }
        });
    }
}
