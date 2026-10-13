package com.simplebuilding.modules.simplemobheads.forge;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.TestCentre;
import com.simplemobheads.ability.HeadAbilities;
import com.simplemobheads.command.MobHeadsCommands;
import com.simplemobheads.drop.HeadDrops;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

/** Forge entry: registration in the registry events, loot through {@code ReloadableServerRegistriesMixin}. */
@net.minecraftforge.fml.common.Mod(SimpleMobHeads.MOD_ID)
public final class MobHeadsForge {
    public MobHeadsForge(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        SimpleMobHeads.init(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        TestCentre.register();
        if (net.minecraftforge.fml.ModList.isLoaded("ftbquests")) {
            com.simplemobheads.guide.MobHeadsGuide.installQuests(net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get());
        }
        ModuleForgeTests.register(bus);
        net.minecraftforge.registries.RegisterEvent.getBus(bus).addListener(event -> {
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
        net.minecraftforge.event.BuildCreativeModeTabContentsEvent.BUS.addListener(event -> {
            if (!event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS) || !SimpleMobHeads.on()
                    || !com.simplebuilding.framework.api.CreativeTabSettings.addItemsToVanillaTabs(
                    net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get(), SimpleMobHeads.MOD_ID)) {
                return;
            }
            var entries = event.getEntries();
            ItemStack previous = new ItemStack(MobHeads.HEAD_ANCHOR);
            if (!entries.contains(previous)) {
                return;
            }
            for (ItemStack stack : MobHeads.baseStacks()) {
                entries.putAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                previous = stack;
            }
        });
        net.minecraftforge.event.entity.living.LivingDeathEvent.BUS.addListener(event -> HeadDrops.onDeath(event.getEntity(), event.getSource()));
        net.minecraftforge.event.RegisterCommandsEvent.BUS.addListener(event -> MobHeadsCommands.register(event.getDispatcher()));
        // EventBus 7 cancellable listeners return true to prevent trampling (breeze head).
        net.minecraftforge.event.level.BlockEvent.FarmlandTrampleEvent.BUS.addListener(event -> !HeadAbilities.tramplesFarmland(event.getEntity()));
    }
}
