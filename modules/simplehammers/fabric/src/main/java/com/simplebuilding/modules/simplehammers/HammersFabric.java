package com.simplebuilding.modules.simplehammers;

import com.simplehammers.SimpleHammers;
import com.simplehammers.engine.SledgehammerProgress;
import com.simplehammers.engine.SledgehammerUpgrades;
import com.simplehammers.engine.SledgehammerUsageEvent;
import com.simplehammers.registry.HammerRegistry;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class HammersFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        FabricLoader loader = FabricLoader.getInstance();
        SimpleHammers.init(loader.getConfigDir(), loader::isModLoaded);
        HammerRegistry.registerItems();
        HammerRegistry.registerTab();
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                SledgehammerUsageEvent.handleBeforeBlockBreak(world, player, pos, state, blockEntity));
        ServerTickEvents.END_SERVER_TICK.register(SledgehammerProgress::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SledgehammerUpgrades.onDisconnect(handler.player));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleHammers.MOD_ID)) {
                for (Map.Entry<Item, List<ItemStack>> entry : HammerRegistry.vanillaPlacements().entrySet()) insertAfter(out, entry.getKey(), entry.getValue());
            }
            if (!SimpleHammers.on()) return;
            var book = List.of(com.simplehammers.guide.HammersGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplehammers.guide.HammersGuide.installQuests(loader.getConfigDir());
    }

    private static void insertAfter(FabricCreativeModeTabOutput out, Item anchor, List<ItemStack> stacks) {
        // Never twice (another mod may have placed them already).
        stacks = stacks.stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
        if (stacks.isEmpty()) return;
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
