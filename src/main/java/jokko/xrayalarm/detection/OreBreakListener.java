package jokko.xrayalarm.detection;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

public class OreBreakListener {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) return;
            net.minecraft.world.level.block.Block block = state.getBlock();

            // Check if it's an ore block by name
            String blockName = block.toString().toLowerCase();
            if (blockName.contains("ore")) {
                OreTracker.handleOreBreak((net.minecraft.server.level.ServerPlayer) player, block);
            }
        });
    }
}
