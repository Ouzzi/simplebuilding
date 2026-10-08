package com.simplebuilding.items;

import com.simplebuilding.Simplebuilding;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Die Kreativ-Tabs der Mod, in {@link ModItemGroupsContent.Tab}-Reihenfolge registriert. */
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
        // Suchtab: die Mod-Items zusaetzlich neben ihre Vanilla-Vorbilder in die Vanilla-Tabs (SearchTabPlacement).
        for (ResourceKey<CreativeModeTab> key : SearchTabPlacement.TABS) {
            CreativeModeTabEvents.modifyOutputEvent(key).register(output -> {
                if (!SearchTabPlacement.enabled()) {
                    return;
                }
                for (SearchTabPlacement.Placement placement : SearchTabPlacement.placementsIfEnabled(key)) {
                    List<ItemStack> present = output.getDisplayStacks();
                    if (present.stream().noneMatch(stack -> stack.is(placement.anchor()))) {
                        continue;
                    }
                    List<ItemStack> fresh = placement.stacks().stream()
                            .filter(stack -> present.stream().noneMatch(p -> ItemStack.isSameItemSameComponents(p, stack)))
                            .toList();
                    if (fresh.isEmpty()) {
                        continue;
                    }
                    if (placement.before()) {
                        output.insertBefore(placement.anchor(), fresh, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    } else {
                        output.insertAfter(placement.anchor(), fresh, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                }
            });
        }
    }

    /**
     * Entwickler-Tab hinter den Tabs der Mod. Immer registriert, aber nur gefuellt, wenn
     * {@link DevEnchantedTab#isShown()} gilt - leer blendet Vanilla ihn aus.
     */
    public static final CreativeModeTab DEV_ENCHANTED = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, DevEnchantedTab.ID),
            FabricCreativeModeTab.builder()
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
