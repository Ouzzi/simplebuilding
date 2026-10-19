package com.simplebuilding.modules.simplehammers;

import com.simplehammers.SimpleHammers;
import com.simplehammers.engine.SledgehammerProgress;
import com.simplehammers.engine.SledgehammerUpgrades;
import com.simplehammers.engine.SledgehammerUsageEvent;
import com.simplehammers.registry.HammerRegistry;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleHammers.MOD_ID)
public final class HammersForge {
    public HammersForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplehammers.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleHammers.init(FMLPaths.CONFIGDIR.get(), net.minecraftforge.fml.ModList::isLoaded);
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.ITEM)) HammerRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) HammerRegistry.registerTab();
        });
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (!event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;
            if (com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleHammers.MOD_ID)) {
                for (Map.Entry<Item, List<ItemStack>> entry : HammerRegistry.vanillaPlacements().entrySet()) insertAfter(event, entry.getKey(), entry.getValue());
            }
            if (SimpleHammers.on()) event.getEntries().put(com.simplehammers.guide.HammersGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        net.minecraftforge.event.level.BlockEvent.BreakEvent.BUS.addListener(event -> {
            if (event.getPlayer() instanceof Player player && !player.level().isClientSide()) {
                Level level = player.level();
                SledgehammerUsageEvent.handleBeforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
            }
            return false;
        });
        net.minecraftforge.event.TickEvent.ServerTickEvent.Post.BUS.addListener(event -> SledgehammerProgress.tick(event.server()));
        net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> {
            if (event.getEntity() instanceof ServerPlayer player) SledgehammerUpgrades.onDisconnect(player);
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simplehammers.guide.HammersGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event, Item anchorItem, List<ItemStack> stacks) {
        var entries = event.getEntries();
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new ItemStack(anchorItem);
        boolean anchored = entries.contains(anchor);
        var previous = anchor;
        for (var stack : stacks) {
            if (entries.contains(stack)) continue;
            if (anchored) {
                entries.putAfter(previous, stack, visibility);
                previous = stack;
            } else {
                entries.put(stack, visibility);
            }
        }
    }
}
