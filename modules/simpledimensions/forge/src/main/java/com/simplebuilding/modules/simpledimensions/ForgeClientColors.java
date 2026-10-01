package com.simplebuilding.modules.simpledimensions;


import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

/**
 * Client-only: tints the nether-style portal texture with the per-portal hue
 * stored in each {@link SkyPortalBlockEntity}, using the data-driven tint system.
 */
@Mod.EventBusSubscriber(modid = "simpledimension", value = Dist.CLIENT)
public final class ForgeClientColors {

    private static final int DEFAULT_COLOR = 0xFF66D9FF;

    private ForgeClientColors() {
    }

    @SubscribeEvent
    public static void onRegisterBlockTints(RegisterColorHandlersEvent.Block event) {
        event.register(List.of(new PortalTintSource()), DimensionRegistry.PORTAL,DimensionRegistry.LEGACY);
    }

    private static final class PortalTintSource implements BlockTintSource {
        @Override
        public int color(BlockState state) {
            return DEFAULT_COLOR;
        }

        @Override
        public int colorInWorld(BlockState state, BlockAndTintGetter getter, BlockPos pos) {
            if (getter != null && pos != null) {
                BlockEntity be = getter.getBlockEntity(pos);
                if (be instanceof SkyPortalBlockEntity portal) {
                    return 0xFF000000 | (portal.getColor() & 0xFFFFFF);
                }
            }
            return DEFAULT_COLOR;
        }
    }
}
