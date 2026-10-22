package com.simplebuilding.modules.simpleblocks;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge 26.3 entry; same registration as Fabric/NeoForge. Ancient City loot comes in through ReloadableServerRegistriesMixin. */
@Mod(SimpleBlocks.MOD_ID)
public final class BlocksForge {
    static net.minecraftforge.network.Channel<net.minecraft.network.protocol.common.custom.CustomPacketPayload> channel;

    public BlocksForge(FMLJavaModLoadingContext context) {
        // Old simplebuilding: ids of the moved building blocks resolve here (before the registry events: Forge aliases).
        SimpleBlocks.registerIdMoves();
        SimpleBlocks.simplebuildingLoaded = net.minecraftforge.fml.ModList.isLoaded("simplebuilding");
        com.simplebuilding.modules.simpleblocks.forge.ModuleForgeTests.register(context.getModBusGroup());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
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
        com.simpleblocks.saw.SawUpgrade.hammerMode = net.minecraftforge.fml.ModList.isLoaded("simplehammers");
        net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock.BUS.addListener(e -> {
            if (!com.simpleblocks.saw.SawUpgrade.onUse(e.getLevel(), e.getPos(), e.getEntity(), e.getHand())) return false;
            e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return true;
        });
        // Shapecutter: its recipe list goes to every client on join and after /reload.
        channel = net.minecraftforge.network.ChannelBuilder.named(SimpleBlocks.id("main")).payloadChannel()
                .protocol(net.minecraftforge.network.NetworkProtocol.PLAY)
                .clientbound()
                    .add(com.simpleblocks.saw.Sawing.Payload.TYPE, com.simpleblocks.saw.Sawing.Payload.CODEC.cast(), (payload, ctx) -> {
                        ctx.setPacketHandled(true);
                        ctx.enqueueWork(() -> com.simpleblocks.saw.Sawing.receive(payload.entries()));
                    })
                .build();
        net.minecraftforge.event.OnDatapackSyncEvent.BUS.addListener(event -> event.getPlayers().forEach(player -> {
            if (player.connection != null && channel.isRemotePresent(player.connection.getConnection())) {
                channel.send(com.simpleblocks.saw.Sawing.Payload.current(player.level().getServer()), net.minecraftforge.network.PacketDistributor.PLAYER.with(player));
            }
        }));
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) BlocksForgeClient.init(context);
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES) && SimpleBlocks.on()) {
                event.getEntries().put(com.simpleblocks.guide.BlocksGuide.stack(), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) && SimpleBlocks.on()) {
                var saw = new ItemStack(com.simpleblocks.saw.Sawing.SAW_ITEM);
                var stonecutter = new ItemStack(net.minecraft.world.item.Items.STONECUTTER);
                if (event.getEntries().contains(saw)) return;
                if (event.getEntries().contains(stonecutter)) event.getEntries().putAfter(stonecutter, saw, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                else event.getEntries().put(saw, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                return;
            }
            boolean natural = event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS);
            if (!natural && !event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) return;
            if (!SimpleBlocks.on() || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleBlocks.MOD_ID)) return;
            var entries = event.getEntries();
            var visibility = CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            for (BlocksRegistry.Placement placement : BlocksRegistry.placements()) {
                if (placement.natural() != natural) continue;
                var before = new ItemStack(placement.before());
                boolean anchored = entries.contains(before);
                ItemStack previous = before;
                for (ItemStack stack : placement.stacks()) {
                    if (entries.contains(stack)) continue;
                    if (anchored && placement.after()) {
                        entries.putAfter(previous, stack, visibility);
                        previous = stack;
                    } else if (anchored) entries.putBefore(before, stack, visibility);
                    else entries.put(stack, visibility);
                }
            }
        });
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) com.simpleblocks.guide.BlocksGuide.installQuests(FMLPaths.CONFIGDIR.get());
    }
}
