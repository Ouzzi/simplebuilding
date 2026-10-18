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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleHammers.MOD_ID)
public final class HammersNeoForge {
    public HammersNeoForge(IEventBus bus, Dist dist) {
        SimpleHammers.init(FMLPaths.CONFIGDIR.get(), id -> net.neoforged.fml.ModList.get().isLoaded(id));
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.ITEM)) HammerRegistry.registerItems();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) HammerRegistry.registerTab();
        });
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) return;
            if (com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleHammers.MOD_ID)) {
                for (Map.Entry<Item, List<ItemStack>> entry : HammerRegistry.vanillaPlacements().entrySet()) insertAfter(event, entry.getKey(), entry.getValue());
            }
            if (SimpleHammers.on()) event.accept(com.simplehammers.guide.HammersGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (BreakBlockEvent event) -> {
            if (event.isCanceled() || !(event.getPlayer() instanceof Player player)) return;
            Level level = player.level();
            if (level.isClientSide()) return;
            SledgehammerUsageEvent.handleBeforeBlockBreak(level, player, event.getPos(), event.getState(), level.getBlockEntity(event.getPos()));
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> SledgehammerProgress.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) SledgehammerUpgrades.onDisconnect(player);
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simplehammers.guide.HammersGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(BuildCreativeModeTabContentsEvent event, Item anchorItem, List<ItemStack> stacks) {
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new ItemStack(anchorItem);
        boolean anchored = event.getParentEntries().contains(anchor) && event.getSearchEntries().contains(anchor);
        var previous = anchor;
        for (var stack : stacks) {
            if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) continue;
            if (anchored) {
                event.insertAfter(previous, stack, visibility);
                previous = stack;
            } else {
                event.accept(stack, visibility);
            }
        }
    }
}
