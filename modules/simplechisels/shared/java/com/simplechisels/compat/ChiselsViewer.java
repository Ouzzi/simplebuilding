package com.simplechisels.compat;

import com.simplechisels.SimpleChisels;
import com.simplechisels.item.ChiselItem;
import com.simplechisels.registry.ChiselRegistry;
import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * Simple Chisels in the recipe viewer (JEI24), free of JEI classes: one "Special Ways" entry per chisel step (block -&gt;
 * next block, with every chisel whose tier has it). Only while SimpleBuilding is absent - it shows the same steps in its own
 * "Chisel" tab (from SimpleLib {@code ChiselActions.tools}). Registered from {@link SimpleChisels#init}.
 */
public final class ChiselsViewer {
    public static final String NOTE = "jei.simplechisels.note.";

    private ChiselsViewer() {
    }

    public static void register() {
        RecipeViewer.special(SimpleChisels.MOD_ID, ChiselsViewer::entries);
    }

    /** The forward and the sneaking steps of every chisel; empty with SimpleBuilding (its own tab) or switched off. */
    public static List<InWorldUpgrade> entries() {
        List<InWorldUpgrade> out = new ArrayList<>();
        if (SimpleChisels.isLoaded("simplebuilding") || !SimpleChisels.on()) return out;
        add(out, "forward", true, Component.translatable(NOTE + "forward"),
                Component.translatable(NOTE + "durability", ChiselItem.TRANSFORM_DAMAGE));
        add(out, "backward", false, Component.translatable(NOTE + "backward"),
                Component.translatable(NOTE + "durability", ChiselItem.REVERSE_TRANSFORM_DAMAGE));
        return out;
    }

    private static void add(List<InWorldUpgrade> out, String kind, boolean forward, Component... notes) {
        Map<String, Step> steps = new LinkedHashMap<>();
        for (ChiselItem chisel : ChiselRegistry.CHISELS.values()) {
            Map<Block, Block> map = forward ? chisel.getForwardMap() : chisel.getBackwardMap();
            for (Map.Entry<Block, Block> link : map.entrySet()) {
                Item from = link.getKey().asItem();
                Item to = link.getValue().asItem();
                if (from == Items.AIR || to == Items.AIR) continue;
                String id = BuiltInRegistries.ITEM.getKey(from).toString().replace(':', '/');
                steps.computeIfAbsent(id, k -> new Step(from, to, new ArrayList<>())).chisels().add(chisel);
            }
        }
        steps.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.naturalOrder())).forEach(entry -> {
            Step step = entry.getValue();
            out.add(new InWorldUpgrade("simplechisels/" + kind + "/" + entry.getKey(), List.of(InWorldUpgrade.Input.of(step.from(), 1)),
                    List.copyOf(step.chisels()), new ItemStack(step.to()), List.of(notes)));
        });
    }

    private record Step(Item from, Item to, List<Item> chisels) {
    }
}
