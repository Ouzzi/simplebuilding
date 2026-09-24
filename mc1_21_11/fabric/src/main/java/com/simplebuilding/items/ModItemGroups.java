package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
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
                    FabricItemGroup.builder()
                            .icon(tab.icon)
                            .title(Component.translatable(tab.translationKey()))
                            .displayItems((displayContext, entries) -> ModItemGroupsContent.populate(tab, entries, displayContext.holders()))
                            .build()));
        }
    }

    /**
     * Entwickler-Tab hinter den vier Tabs. Immer registriert, aber nur gefuellt, wenn
     * {@link DevEnchantedTab#isShown()} gilt - leer blendet Vanilla ihn aus.
     */
    public static final CreativeModeTab DEV_ENCHANTED = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, DevEnchantedTab.ID),
            FabricItemGroup.builder()
                    .icon(DevEnchantedTab::icon)
                    .title(Component.translatable(DevEnchantedTab.translationKey()))
                    .displayItems((displayContext, entries) -> DevEnchantedTab.populateIfShown(entries, displayContext.holders()))
                    .build());

    public static CreativeModeTab get(ModItemGroupsContent.Tab tab) {
        return GROUPS.get(tab);
    }

    public static void registerItemGroups() {
        Simplebuilding.LOGGER.info("Registering Item Groups for {}", Simplebuilding.MOD_ID);
    }
}
