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
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(out -> {
            if (!SimpleStorage.on()) return;
            var book = List.of(com.simplestorage.guide.StorageGuide.stack());
            // Before the writable book: a tail append clashes with other mods' insertAfter at the tail.
            if (out.getDisplayStacks().stream().anyMatch(s -> s.is(Items.WRITABLE_BOOK))) out.insertBefore(Items.WRITABLE_BOOK, book, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            else book.forEach(s -> out.accept(s, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));
        });
        if (loader.isModLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(loader.getConfigDir());
    }
}
