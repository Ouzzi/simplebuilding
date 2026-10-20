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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration order as Fabric/NeoForge. */
@Mod(SimpleStorage.MOD_ID)
public final class StorageForge {
    public StorageForge(FMLJavaModLoadingContext context) {
        com.simplebuilding.modules.simplestorage.forge.ModuleForgeTests.register(context.getModBusGroup());
        SimpleStorage.init();
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            var key = event.getRegistryKey();
            // Forge unlocks one registry per event: every part in its own.
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) com.simplestorage.registry.StorageComponents.register();
            if (key.equals(Registries.BLOCK)) StorageBlocks.register(ModList.isLoaded(com.simplestorage.crate.CrateWood.SIMPLE_END));
            if (key.equals(Registries.ITEM)) StorageItems.register();
            if (key.equals(Registries.BLOCK_ENTITY_TYPE)) {
                StorageBlockEntities.register(new StorageBlockEntities.Types() {
                    @Override
                    public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> create(
                            BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<net.minecraft.world.level.block.Block> blocks) {
                        return new BlockEntityType<>(factory, blocks);
                    }
                });
            }
            if (key.equals(Registries.ENTITY_TYPE)) com.simplestorage.registry.StorageEntities.register();
            if (key.equals(Registries.MENU)) StorageMenus.register();
            if (key.equals(Registries.CREATIVE_MODE_TAB)) StorageRegistry.registerTab();
        });
        FMLCommonSetupEvent.getBus(context.getModBusGroup()).addListener(event -> event.enqueueWork(() -> {
            StorageBlocks.registerFlammability();
            com.simplestorage.registry.StorageRegistry.registerBehaviours();
        }));
        boolean sb = ModList.isLoaded("simplebuilding");
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
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
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleStorage.on()) {
                event.getEntries().put(com.simplestorage.guide.StorageGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (ModList.isLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) StorageForgeClient.init(context);
    }

    /** Behind {@code anchor} in this order; never twice; at the end when the anchor is missing. */
    private static void insertAfter(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event,
                                    net.minecraft.world.item.Item anchorItem, java.util.List<net.minecraft.world.item.ItemStack> stacks) {
        var entries = event.getEntries();
        var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
        var anchor = new net.minecraft.world.item.ItemStack(anchorItem);
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
