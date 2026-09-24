package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;

import java.util.EnumMap;
import java.util.Map;

/** Die vier Kreativ-Tabs der Mod, in {@link ModItemGroupsContent.Tab}-Reihenfolge registriert. */
public class ModItemGroups {
    public static final Map<ModItemGroupsContent.Tab, CreativeModeTab> GROUPS = new EnumMap<>(ModItemGroupsContent.Tab.class);

    static {
        for (ModItemGroupsContent.Tab tab : ModItemGroupsContent.Tab.values()) {
            GROUPS.put(tab, Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, tab.id),
                    FabricCreativeModeTab.builder()
                            .icon(tab.icon)
                            .title(Component.translatable(tab.translationKey()))
                            .displayItems((displayContext, entries) -> ModItemGroupsContent.populate(tab, entries, displayContext.holders()))
                            .build()));
        }
    }

    public static CreativeModeTab get(ModItemGroupsContent.Tab tab) {
        return GROUPS.get(tab);
    }

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for {}", Simplebuilding.MOD_ID);
    }
}
