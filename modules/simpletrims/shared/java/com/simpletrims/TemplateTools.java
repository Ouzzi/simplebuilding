package com.simpletrims;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;

/**
 * Which tool works on trim templates (stage 1 of the module split). Without SimpleBuilding it is any axe
 * (vanilla fallback); with SimpleBuilding it is the sledgehammer family, found by the public item tag
 * {@code simplebuilding:sledgehammer_tools} (never by class, principle: modules meet by id).
 */
public final class TemplateTools {
    public static final TagKey<net.minecraft.world.item.Item> SLEDGEHAMMERS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("simplebuilding", "sledgehammer_tools"));

    public static boolean isTemplateTool(ItemStack stack, boolean simpleBuildingLoaded) {
        return simpleBuildingLoaded ? stack.is(SLEDGEHAMMERS) : stack.is(ItemTags.AXES);
    }

    private TemplateTools() {}
}
