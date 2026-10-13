package com.simpletrims;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Which tool works on trim templates - the same rule as SimpleLib's crucibles (principle 5a, split plan 2.6): as long
 * as no partner with a hammer switched SimpleLib's axe ways off, any axe ({@code #minecraft:axes}); once one did
 * (SimpleBuilding today, Simple Hammers later) only the hammers in {@code #simpletrims:template_hammers}, which names
 * {@code #simplebuilding:sledgehammer_tools} as optional entry (modules meet by id, never by class). Used for hitting
 * placed templates and for the Pulsating recipe ({@code simpletrims:tool_shapeless}).
 */
public final class TemplateTools {
    /** Hammers that replace the axe once a hammer partner is loaded. */
    public static final TagKey<Item> HAMMERS = TagKey.create(Registries.ITEM, SimpleTrims.id("template_hammers"));
    /** Axes and hammers: the ingredient of the tool slot in recipes (the mode decides which half counts). */
    public static final TagKey<Item> TOOLS = TagKey.create(Registries.ITEM, SimpleTrims.id("template_tools"));

    /** Hammer mode: a partner switched the axe ways off. */
    public static boolean hammerMode() {
        return !com.simplelib.api.SimpleLibApi.axeWaysEnabled();
    }

    public static boolean isTemplateTool(ItemStack stack) {
        return isTemplateTool(stack, hammerMode());
    }

    /** The rule for an explicit mode (tests check both without loading a partner). */
    public static boolean isTemplateTool(ItemStack stack, boolean hammerMode) {
        if (!SimpleTrims.on() || stack.isEmpty()) return false;
        return hammerMode ? stack.is(HAMMERS) : stack.is(ItemTags.AXES);
    }

    /** The tag that names the tools of the current mode (recipe display). */
    public static TagKey<Item> currentTag() {
        return hammerMode() ? HAMMERS : ItemTags.AXES;
    }

    private TemplateTools() {}
}
