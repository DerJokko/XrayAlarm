package jokko.xrayalarm.detection;

import jokko.xrayalarm.config.XrayConfig;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;

public class OreBreakListener {

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) return;
            Block block = state.getBlock();

            if (block.getRegistryEntry().isIn(BlockTags.ORES)) {
                OreTracker.handleOreBreak((ServerPlayerEntity) player, block);
            }
        });
    }
}
