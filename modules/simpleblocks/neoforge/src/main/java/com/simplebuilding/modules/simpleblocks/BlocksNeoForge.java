package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksLoot;
import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleBlocks.MOD_ID)
public final class BlocksNeoForge {
    public BlocksNeoForge(IEventBus bus, net.neoforged.api.distmarker.Dist dist) {
        // Old simplebuilding: ids of the moved building blocks resolve here (before the registry events).
        SimpleBlocks.registerIdMoves();
        SimpleBlocks.simplebuildingLoaded = net.neoforged.fml.ModList.get().isLoaded("simplebuilding");
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.BLOCK)) BlocksRegistry.registerBlocks();
            if (key.equals(Registries.ITEM)) BlocksRegistry.registerItems();
            if (key.equals(Registries.ENTITY_TYPE)) BlocksRegistry.registerEntityTypes();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) BlocksRegistry.registerTab();
            if (key.equals(Registries.MENU)) BlocksRegistry.registerMenus();
            if (key.equals(Registries.RECIPE_TYPE)) BlocksRegistry.registerRecipeTypes();
            if (key.equals(Registries.RECIPE_SERIALIZER)) BlocksRegistry.registerRecipeSerializers();
        });
        // Stonecutter -> Shapecutter in the world: axe alone, hammer with Simple Hammers; 4 iron nuggets.
        com.simpleblocks.saw.SawUpgrade.hammerMode = net.neoforged.fml.ModList.get().isLoaded("simplehammers");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.isCanceled()) return;
            if (com.simpleblocks.saw.SawUpgrade.onUse(e.getLevel(), e.getPos(), e.getEntity(), e.getHand())) {
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                e.setCanceled(true);
            }
        });
        // Shapecutter: its recipe list goes to every client on join and after /reload.
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
                event.registrar("1").optional().playToClient(com.simpleblocks.saw.Sawing.Payload.TYPE, com.simpleblocks.saw.Sawing.Payload.CODEC,
                        (payload, context) -> context.enqueueWork(() -> com.simpleblocks.saw.Sawing.receive(payload.entries()))));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent event) ->
                event.getRelevantPlayers().forEach(player -> {
                    if (player.connection != null && player.connection.hasChannel(com.simpleblocks.saw.Sawing.Payload.TYPE)) {
                        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, com.simpleblocks.saw.Sawing.Payload.current(player.level().getServer()));
                    }
                }));
        if (dist == net.neoforged.api.distmarker.Dist.CLIENT) BlocksNeoClient.init(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.LootTableLoadEvent e) -> BlocksLoot.apply(
                net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, e.getName()), pool -> e.getTable().addPool(pool.build())));
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBlocks.on()) {
                event.accept(com.simpleblocks.guide.BlocksGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && SimpleBlocks.on()) {
                var saw = new ItemStack(com.simpleblocks.saw.Sawing.SAW_ITEM);
                var stonecutter = new ItemStack(net.minecraft.world.item.Items.STONECUTTER);
                if (event.getParentEntries().contains(saw) || event.getSearchEntries().contains(saw)) return;
                if (event.getParentEntries().contains(stonecutter) && event.getSearchEntries().contains(stonecutter)) {
                    event.insertAfter(stonecutter, saw, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                } else {
                    event.accept(saw, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                }
                return;
            }
            boolean natural = event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS);
            if (!natural && !event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) return;
            if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBlocks.MOD_ID)) return;
            var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                if (placement.natural() != natural) continue;
                var before = new ItemStack(placement.before());
                boolean anchored = event.getParentEntries().contains(before) && event.getSearchEntries().contains(before);
                ItemStack previous = before;
                for (ItemStack stack : placement.stacks()) {
                    if (event.getParentEntries().contains(stack) || event.getSearchEntries().contains(stack)) continue;
                    if (anchored && placement.after()) {
                        // Behind the anchor in order: each one behind the one before it.
                        event.insertAfter(previous, stack, visibility);
                        previous = stack;
                    } else if (anchored) event.insertBefore(before, stack, visibility);
                    else event.accept(stack, visibility);
                }
            }
        });
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
