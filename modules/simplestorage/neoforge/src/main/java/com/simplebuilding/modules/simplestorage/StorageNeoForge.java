package com.simplebuilding.modules.simplestorage;

import com.simplestorage.SimpleStorage;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: Vanilla Registry.register works inside the matching RegisterEvent. */
@Mod(SimpleStorage.MOD_ID)
public final class StorageNeoForge {
    public StorageNeoForge(IEventBus bus, Dist dist) {
        SimpleStorage.init();
        SimpleStorage.loadConfig(FMLPaths.CONFIGDIR.get());
        ModuleNeoTests.register(bus);
        bus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) com.simplestorage.registry.StorageComponents.register();
            if (key.equals(Registries.BLOCK)) StorageBlocks.register(ModList.get().isLoaded(com.simplestorage.crate.CrateWood.SIMPLE_END));
            if (key.equals(Registries.ITEM)) StorageItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                StorageBlockEntities.register(new StorageBlockEntities.Types() {
                    @Override
                    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> create(
                            BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<net.minecraft.world.level.block.Block> blocks) {
                        return new BlockEntityType<>(factory, blocks, false);
                    }
                });
            }
            if (key.equals(Registries.ENTITY_TYPE)) com.simplestorage.registry.StorageEntities.register();
            if (key.equals(Registries.MENU)) StorageMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) StorageRegistry.registerTab();
            if (key.equals(Registries.RECIPE_SERIALIZER)) StorageRegistry.registerSerializers();
        });
        // Split stage 8c-1: the bundle packets.
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) -> {
            var registrar = event.registrar("1").optional();
            registrar.playToServer(com.simplestorage.bundle.BundleNet.SelectPayload.TYPE, com.simplestorage.bundle.BundleNet.SelectPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplestorage.bundle.BundleNet.handle(payload, (net.minecraft.server.level.ServerPlayer) context.player())));
            registrar.playToServer(com.simplestorage.bundle.BundleNet.ScrollPayload.TYPE, com.simplestorage.bundle.BundleNet.ScrollPayload.CODEC,
                    (payload, context) -> context.enqueueWork(() -> com.simplestorage.bundle.BundleNet.handle(payload, (net.minecraft.server.level.ServerPlayer) context.player())));
        });
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
            StorageBlocks.registerFlammability();
            com.simplestorage.registry.StorageRegistry.registerBehaviours();
        }));
        bus.addListener(StorageNeoCapabilities::register);
        boolean sb = ModList.get().isLoaded("simplebuilding");
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            boolean vanillaTabs = StorageRegistry.fillsVanillaTabs(sb,
                    com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleStorage.MOD_ID));
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && vanillaTabs) {
                // On its own: the table behind the crafting table, the crates behind the barrel.
                insertAfter(event, net.minecraft.world.item.Items.CRAFTING_TABLE, java.util.List.of(new net.minecraft.world.item.ItemStack(StorageItems.STORAGE_CRAFTING_TABLE)));
                insertAfter(event, net.minecraft.world.item.Items.BARREL, StorageRegistry.crateStacks());
                insertAfter(event, net.minecraft.world.item.Items.TRAPPED_CHEST, StorageRegistry.chestStacks());
                insertAfter(event, net.minecraft.world.item.Items.SHULKER_BOX, StorageRegistry.shulkerBoxStacks());
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && vanillaTabs) {
                // On its own: the chest carts behind Vanilla's chest cart, the chest boats behind the bamboo chest raft.
                insertAfter(event, net.minecraft.world.item.Items.CHEST_MINECART, StorageRegistry.chestMinecartStacks());
                insertAfter(event, net.minecraft.world.item.Items.BAMBOO_CHEST_RAFT, StorageRegistry.chestBoatStacks());
                // Split stage 8c-1: the bundles and quivers behind Vanilla's bundle.
                insertAfter(event, net.minecraft.world.item.Items.BUNDLE, StorageRegistry.bundleStacks());
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleStorage.on()) {
                event.accept(com.simplestorage.guide.StorageGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (ModList.get().isLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (dist == Dist.CLIENT) StorageNeoClient.init(bus);
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(BuildCreativeModeTabContentsEvent event, net.minecraft.world.item.Item anchorItem, java.util.List<net.minecraft.world.item.ItemStack> stacks) {
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new net.minecraft.world.item.ItemStack(anchorItem);
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
