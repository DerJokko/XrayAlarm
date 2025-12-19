package jokko.xrayalarm.detection;

import jokko.xrayalarm.config.XrayConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.registries.BuiltInRegistries;

public class OreBreakListener {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;
            net.minecraft.world.level.block.Block block = state.getBlock();

            // Only call the tracker for blocks that are configured in tracked_blocks
            String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
            if (XrayConfig.trackedBlocks.containsKey(blockId)) {
                OreTracker.handleOreBreak((net.minecraft.server.level.ServerPlayer) player, block, pos);
            }
        });
    }
}
