package com.simplebuilding.modules.simpledimensions;


import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import java.util.List;

/**
 * Client-only: tints the nether-style portal texture with the per-portal hue
 * stored in each {@link SkyPortalBlockEntity}, using the data-driven tint system.
 */
@EventBusSubscriber(modid = "simpledimension", value = Dist.CLIENT)
public final class NeoForgeClientColors {

    private static final int DEFAULT_COLOR = 0xFF66D9FF;

    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){
        net.neoforged.fml.ModList.get().getModContainerById("simpledimension").orElseThrow().registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,(container,parent)->com.simplebuilding.modules.simpledimensions.client.DimensionConfigScreen.create(parent));
    }
    private NeoForgeClientColors() {
    }

    @SubscribeEvent
    public static void onRegisterBlockTints(RegisterColorHandlersEvent.BlockTintSources event) {
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
