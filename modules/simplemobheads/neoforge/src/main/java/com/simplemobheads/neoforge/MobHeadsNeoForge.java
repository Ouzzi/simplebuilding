package com.simplemobheads.neoforge;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.TestCentre;
import com.simplemobheads.ability.HeadAbilities;
import com.simplemobheads.command.MobHeadsCommands;
import com.simplemobheads.drop.HeadDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry: registration in the registry events (Vanilla {@code Registry.register} while the registry is open). */
@Mod(SimpleMobHeads.MOD_ID)
public final class MobHeadsNeoForge {
    public MobHeadsNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        SimpleMobHeads.init(FMLPaths.CONFIGDIR.get());
        TestCentre.register();
        if (net.neoforged.fml.ModList.get().isLoaded("ftbquests")) {
            com.simplemobheads.guide.MobHeadsGuide.installQuests(FMLPaths.CONFIGDIR.get());
        }
        com.simplebuilding.modules.simplemobheads.ModuleNeoTests.register(modBus);
        modBus.addListener((RegisterEvent event) -> {
            var key = event.getRegistryKey();
            if (key.equals(Registries.DATA_COMPONENT_TYPE)) {
                MobHeads.components();
            } else if (key.equals(Registries.LOOT_FUNCTION_TYPE)) {
                MobHeads.lootTypes(true, false);
            } else if (key.equals(Registries.LOOT_CONDITION_TYPE)) {
                MobHeads.lootTypes(false, true);
            } else if (key.equals(Registries.BLOCK)) {
                MobHeads.registerBlocks();
            } else if (key.equals(Registries.ITEM)) {
                MobHeads.registerItems();
            } else if (key.equals(Registries.CREATIVE_MODE_TAB)) {
                MobHeads.registerTab();
            }
        });
        // Behind the dragon head in Functional Blocks (and so in the search tab).
        modBus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (!event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) || !SimpleMobHeads.on()
                    || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(FMLPaths.CONFIGDIR.get(), SimpleMobHeads.MOD_ID)) {
                return;
            }
            ItemStack previous = new ItemStack(MobHeads.HEAD_ANCHOR);
            if (!event.getParentEntries().contains(previous)) {
                return;
            }
            for (ItemStack stack : MobHeads.baseStacks()) {
                event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                previous = stack;
            }
        });
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> HeadDrops.apply(
                ResourceKey.create(Registries.LOOT_TABLE, event.getName()), pool -> event.getTable().addPool(pool.build()), event.getRegistries()));
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> HeadDrops.onDeath(event.getEntity(), event.getSource()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> MobHeadsCommands.register(event.getDispatcher()));
        // Breeze head: NeoForge moves the trample check into Entity#canTrample and asks this event (the Vanilla mixin finds nothing).
        NeoForge.EVENT_BUS.addListener((BlockEvent.FarmlandTrampleEvent event) -> {
            if (!HeadAbilities.tramplesFarmland(event.getEntity())) {
                event.setCanceled(true);
            }
        });
        if (dist == Dist.CLIENT) {
            MobHeadsNeoClient.init(container);
        }
    }
}
