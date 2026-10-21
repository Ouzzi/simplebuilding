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
        SimpleStorage.loadConfig(FMLPaths.CONFIGDIR.get());
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
            if (key.equals(Registries.MENU)) {
                StorageMenus.register();
                // Split stage 8c-2: the opening data travel in Forge's menu buffer.
                StorageMenus.registerBackpack(net.minecraftforge.common.extensions.IForgeMenuType.create((syncId, inventory, buffer) ->
                        new com.simplestorage.backpack.BackpackMenu(syncId, inventory, com.simplestorage.backpack.BackpackOpenData.STREAM_CODEC.decode(buffer))));
            }
            if (key.equals(Registries.CREATIVE_MODE_TAB)) StorageRegistry.registerTab();
            if (key.equals(Registries.RECIPE_SERIALIZER)) StorageRegistry.registerSerializers();
        });
        // Split stage 8c-1: the bundle packets (client -> server).
        channel = net.minecraftforge.network.ChannelBuilder.named(SimpleStorage.id("main")).payloadChannel()
                .protocol(net.minecraftforge.network.NetworkProtocol.PLAY)
                .serverbound()
                    .add(com.simplestorage.bundle.BundleNet.SelectPayload.TYPE, com.simplestorage.bundle.BundleNet.SelectPayload.CODEC.cast(),
                            (payload, ctx) -> onPlayer(ctx, p -> com.simplestorage.bundle.BundleNet.handle(payload, p)))
                    .add(com.simplestorage.bundle.BundleNet.ScrollPayload.TYPE, com.simplestorage.bundle.BundleNet.ScrollPayload.CODEC.cast(),
                            (payload, ctx) -> onPlayer(ctx, p -> com.simplestorage.bundle.BundleNet.handle(payload, p)))
                    // Split stage 8c-2: the backpack key.
                    .add(com.simplestorage.backpack.BackpackNet.OpenPayload.TYPE, com.simplestorage.backpack.BackpackNet.OpenPayload.CODEC.cast(),
                            (payload, ctx) -> onPlayer(ctx, p -> com.simplestorage.backpack.BackpackNet.handle(payload, p)))
                .build();
        com.simplestorage.backpack.BackpackMenuProviders.opener = (player, opening) -> ((net.minecraftforge.common.extensions.IForgeServerPlayer) player).openMenu(
                opening.provider(), (net.minecraft.network.FriendlyByteBuf buffer) -> com.simplestorage.backpack.BackpackOpenData.STREAM_CODEC.encode(buffer, opening.data()));
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
                // Split stage 8c-1: the bundles and quivers behind Vanilla's bundle.
                insertAfter(event, net.minecraft.world.item.Items.BUNDLE, StorageRegistry.bundleStacks());
                // Split stage 8c-2: the backpacks behind the last quiver.
                insertAfter(event, StorageItems.ENDERITE_QUIVER, StorageRegistry.backpackStacks());
            }
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleStorage.on()) {
                event.getEntries().put(com.simplestorage.guide.StorageGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        });
        if (ModList.isLoaded("ftbquests")) com.simplestorage.guide.StorageGuide.installQuests(FMLPaths.CONFIGDIR.get());
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) StorageForgeClient.init(context);
    }

    /** The bundle packets' channel (split stage 8c-1). */
    static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    private static void onPlayer(net.minecraftforge.event.network.CustomPayloadEvent.Context ctx, java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action) {
        ctx.setPacketHandled(true);
        ctx.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = ctx.getSender();
            if (player != null) action.accept(player);
        });
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
