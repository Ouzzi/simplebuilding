package com.simplebuilding.modules.simplestorage;

import com.simplestorage.SimpleStorage;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
import com.simplestorage.registry.StorageRegistry;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Fabric entry: registries are open during init, so everything registers directly. */
public final class StorageFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SimpleStorage.init();
        FabricLoader loader = FabricLoader.getInstance();
        SimpleStorage.loadConfig(loader.getConfigDir());
        StorageRegistry.registerSerializers();
        // Split stage 8c-1: the bundle packets (selection in a slot, scroll on a placed bundle).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.simplestorage.bundle.BundleNet.SelectPayload.TYPE, com.simplestorage.bundle.BundleNet.SelectPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.simplestorage.bundle.BundleNet.ScrollPayload.TYPE, com.simplestorage.bundle.BundleNet.ScrollPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplestorage.bundle.BundleNet.SelectPayload.TYPE,
                (payload, context) -> context.server().execute(() -> com.simplestorage.bundle.BundleNet.handle(payload, context.player())));
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplestorage.bundle.BundleNet.ScrollPayload.TYPE,
                (payload, context) -> context.server().execute(() -> com.simplestorage.bundle.BundleNet.handle(payload, context.player())));
        // Split stage 8c-2: the backpack key packet; menus open through Fabric's ExtendedMenuProvider (opening data in the buffer).
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(com.simplestorage.backpack.BackpackNet.OpenPayload.TYPE, com.simplestorage.backpack.BackpackNet.OpenPayload.CODEC);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(com.simplestorage.backpack.BackpackNet.OpenPayload.TYPE,
                (payload, context) -> context.server().execute(() -> com.simplestorage.backpack.BackpackNet.handle(payload, context.player())));
        com.simplestorage.backpack.BackpackMenuProviders.opener = (player, opening) -> player.openMenu(
                new net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider<com.simplestorage.backpack.BackpackOpenData>() {
                    @Override
                    public net.minecraft.network.chat.Component getDisplayName() {
                        return opening.provider().getDisplayName();
                    }

                    @Override
                    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inventory,
                            net.minecraft.world.entity.player.Player menuPlayer) {
                        return opening.provider().createMenu(syncId, inventory, menuPlayer);
                    }

                    @Override
                    public com.simplestorage.backpack.BackpackOpenData getScreenOpeningData(net.minecraft.server.level.ServerPlayer serverPlayer) {
                        return opening.data();
                    }
                });
        // Trinkets Updated (optional): backpacks and quivers in the back and belt slots count as worn (SimpleLib AccessorySlots).
        if (loader.isModLoaded(TrinketsHook.MOD_ID)) TrinketsHook.register();
        com.simplestorage.registry.StorageComponents.register();
        StorageBlocks.register(loader.isModLoaded(com.simplestorage.crate.CrateWood.SIMPLE_END));
        StorageItems.register();
        StorageBlockEntities.register(new StorageBlockEntities.Types() {
            @Override
            public <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> create(
                    BlockEntityType.BlockEntitySupplier<? extends T> factory, Set<net.minecraft.world.level.block.Block> blocks) {
                return new BlockEntityType<>(factory, blocks);
            }
        });
        com.simplestorage.registry.StorageEntities.register();
        StorageMenus.register();
        StorageMenus.registerBackpack(new net.fabricmc.fabric.api.menu.v1.ExtendedMenuType<>(com.simplestorage.backpack.BackpackMenu::new,
                com.simplestorage.backpack.BackpackOpenData.STREAM_CODEC));
        StorageRegistry.registerTab();
        StorageBlocks.registerFlammability();
        com.simplestorage.registry.StorageRegistry.registerBehaviours();
        boolean sb = loader.isModLoaded("simplebuilding");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(out -> {
            if (!StorageRegistry.fillsVanillaTabs(sb, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleStorage.MOD_ID))) return;
            // On its own: the table behind the crafting table, the crates behind the barrel.
            insertAfter(out, Items.CRAFTING_TABLE, List.of(new ItemStack(StorageItems.STORAGE_CRAFTING_TABLE)));
            insertAfter(out, Items.BARREL, StorageRegistry.crateStacks());
            insertAfter(out, Items.TRAPPED_CHEST, StorageRegistry.chestStacks());
            insertAfter(out, Items.SHULKER_BOX, StorageRegistry.shulkerBoxStacks());
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS).register(out -> {
            // Split stage 8b-3, on its own: the tier shulker shells behind Vanilla's shell.
            if (StorageRegistry.fillsVanillaTabs(sb, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleStorage.MOD_ID))) {
                insertAfter(out, Items.SHULKER_SHELL, StorageRegistry.shellStacks());
            }
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (StorageRegistry.fillsVanillaTabs(sb, com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(loader.getConfigDir(), SimpleStorage.MOD_ID))) {
                // On its own: the chest carts behind Vanilla's chest cart, the chest boats behind the bamboo chest raft.
                insertAfter(out, Items.CHEST_MINECART, StorageRegistry.chestMinecartStacks());
                insertAfter(out, Items.BAMBOO_CHEST_RAFT, StorageRegistry.chestBoatStacks());
                // Split stage 8c-1: the bundles and quivers behind Vanilla's bundle.
                insertAfter(out, Items.BUNDLE, StorageRegistry.bundleStacks());
                // Split stage 8c-2: the backpacks behind the last quiver.
                insertAfter(out, StorageItems.ENDERITE_QUIVER, StorageRegistry.backpackStacks());
            }
            if (!SimpleStorage.on()) return;
            var book = List.of(com.simplestorage.guide.StorageGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(loader.getConfigDir());
    }

    private static void insertAfter(net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTabOutput out, net.minecraft.world.item.Item anchor, List<ItemStack> stacks) {
        // Never twice (another mod may have placed them already).
        stacks = stacks.stream().filter(s -> out.getDisplayStacks().stream().noneMatch(d -> ItemStack.isSameItemSameComponents(d, s))).toList();
        if (stacks.isEmpty()) return;
        if (out.getDisplayStacks().stream().anyMatch(s -> s.is(anchor))) out.insertAfter(anchor, stacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        else stacks.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
    }
}
