package com.simplebuilding.modules.simpledimensions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
@Mod("simpledimension")
public final class ForgeExample {
    public ForgeExample(FMLJavaModLoadingContext context) {
        ModuleForgeTests.register(context.getModBusGroup());
  if(net.minecraftforge.fml.ModList.isLoaded("ftbquests"))com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
  net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(e->{if(e.getTabKey().equals(net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES))e.accept(com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide.book());});
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(e -> DimensionRuntime.get(e.server()).tick());
        net.minecraftforge.event.server.ServerStoppedEvent.BUS.addListener(e -> DimensionRuntime.stop(e.getServer()));
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(net.minecraftforge.eventbus.api.listener.Priority.LOWEST, e -> {
            if (e.getUseBlock().isDenied() || e.getUseItem().isDenied()) return false;
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                var result = PortalActivation.ignite(player, e.getHand(), e.getHitVec());
                if (result != InteractionResult.PASS) {
                    e.setCancellationResult(result);
                    return true;
                }
            }
            return false;
        });
        DimensionRuntime.claimIntegrationRequired = java.util.List.of("flan", "ftbchunks", "openpartiesandclaims", "griefdefender", "claimchunk")
                .stream().anyMatch(net.minecraftforge.fml.ModList::isLoaded);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey().equals(Registries.BLOCK)) DimensionRegistry.blocks();
            if (event.getRegistryKey().equals(Registries.ITEM)) DimensionRegistry.items();
            if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
                DimensionRegistry.PORTAL_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DimensionRegistry.id("sky_portal"),
                        portalType(java.util.Set.of(DimensionRegistry.PORTAL, DimensionRegistry.LEGACY)));
                DimensionRegistry.LEGACY_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, DimensionRegistry.id("light_blue_portal"),
                        portalType(java.util.Set.of(DimensionRegistry.LEGACY)));
            }
        });
    }
    private static BlockEntityType<SkyPortalBlockEntity> portalType(java.util.Set<net.minecraft.world.level.block.Block> blocks) {
        // Forge's two-argument constructor has no NeoForge/Fabric security flag.
        return new BlockEntityType<>(SkyPortalBlockEntity::new, blocks) {
            @Override public boolean onlyOpCanSetNbt() { return true; }
        };
    }
}
