package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksLoot;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class BlocksFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BlocksRegistry.registerAll();
        // Stonecutter -> Shaping Saw in the world: axe alone, hammer with SimpleBuilding; 4 iron nuggets.
        com.simpleblocks.saw.SawUpgrade.hammerMode = FabricLoader.getInstance().isModLoaded("simplebuilding");
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) ->
                com.simpleblocks.saw.SawUpgrade.onUse(level, hit.getBlockPos(), player, hand) ? net.minecraft.world.InteractionResult.SUCCESS : net.minecraft.world.InteractionResult.PASS);
        // Shaping Saw: its recipe list goes to every client on join and after /reload (the client menu uses exactly this list).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(com.simpleblocks.saw.Sawing.Payload.TYPE, com.simpleblocks.saw.Sawing.Payload.CODEC);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
            if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, com.simpleblocks.saw.Sawing.Payload.TYPE)) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, com.simpleblocks.saw.Sawing.Payload.current(player.level().getServer()));
            }
        });
        net.fabricmc.fabric.api.loot.v3.LootTableEvents.MODIFY.register((key, table, source, registries) -> BlocksLoot.apply(key, table::withPool));
        for (boolean natural : new boolean[]{false, true}) {
            CreativeModeTabEvents.modifyOutputEvent(natural ? CreativeModeTabs.NATURAL_BLOCKS : CreativeModeTabs.BUILDING_BLOCKS).register(out -> {
                if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FabricLoader.getInstance().getConfigDir(), SimpleBlocks.MOD_ID)) return;
                for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                    if (placement.natural() != natural) continue;
                    // Never twice (another mod may have placed them already); at the end when the Vanilla item is missing.
                    var stacks = placement.stacks().stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
                    if (stacks.isEmpty()) continue;
                    if (out.getDisplayStacks().stream().anyMatch(s -> s.is(placement.before()))) out.insertBefore(placement.before(), stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
                }
            });
        }
        // The saw stands behind the Vanilla stonecutter in the functional blocks tab.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!SimpleBlocks.on()) return;
            var saw = java.util.List.of(new ItemStack(com.simpleblocks.saw.Sawing.SAW_ITEM));
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(com.simpleblocks.saw.Sawing.SAW_ITEM))) return;
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.STONECUTTER))) out.insertAfter(net.minecraft.world.item.Items.STONECUTTER, saw, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else saw.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleBlocks.on()) return;
            var book = java.util.List.of(com.simpleblocks.guide.BlocksGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.WRITABLE_BOOK))) out.insertBefore(net.minecraft.world.item.Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (FabricLoader.getInstance().isModLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FabricLoader.getInstance().getConfigDir());
    }
}
