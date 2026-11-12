package com.simpleblocks.metal;

import com.simpleblocks.BlocksRegistry;
import com.simpleblocks.SimpleBlocks;
import com.simplelib.api.aging.AgingChain;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.MapColor;

/**
 * PAL35b (owner 11.11. "copper pillar and tiles like the other metals, with the oxidation stages and waxed like Vanilla copper",
 * docs/ai/PLAN-A-PAL35-2026-11-12.md part b): copper tiles and copper pillar weather in Vanilla's three stages (exposed, weathered,
 * oxidized) and every stage - the end stage too - has a waxed copy, built on SimpleLib's {@link AgingChain} (honeycomb, axe, dispenser,
 * Vanilla's neighbour rule and chance). The base blocks ({@code copper_tiles}, {@code copper_pillar}) are {@link BlocksRegistry} entries
 * with the properties of cut copper; the stages copy them with Vanilla's map colours. Both forms age as one front (group {@code copper}).
 */
public final class MetalBlocks {
    /** Vanilla's stage words, oldest last. */
    public static final List<String> STAGES = List.of("exposed", "weathered", "oxidized");
    public static final Identifier GROUP = SimpleBlocks.id("copper");
    /** Map colour per stage (0 = base), as Vanilla's copper blocks. */
    private static final MapColor[] COLORS = {MapColor.COLOR_ORANGE, MapColor.TERRACOTTA_LIGHT_GRAY, MapColor.WARPED_STEM, MapColor.WARPED_NYLIUM};

    public static final AgingChain COPPER_TILES = chain("copper_tiles");
    public static final AgingChain COPPER_PILLAR = chain("copper_pillar");
    public static final List<AgingChain> CHAINS = List.of(COPPER_TILES, COPPER_PILLAR);

    private MetalBlocks() {}

    /** One form: base -> exposed_x, weathered_x, oxidized_x (+ waxed_x ... waxed_oxidized_x); the base's class (Block / pillar). */
    private static AgingChain chain(String name) {
        return AgingChain.builder(SimpleBlocks.id(name), BlocksRegistry.BLOCKS.get(name))
                .stages(STAGES.stream().map(s -> s + "_" + name).toArray(String[]::new))
                .group(GROUP)
                .waxEnd()
                .chance(() -> SimpleBlocks.on() ? AgingChain.VANILLA_CHANCE : 0)
                .properties((stage, p) -> p.mapColor(COLORS[stage]))
                .build();
    }

    /** Every created block path of a form in tab order: the three stages, then waxed base ... waxed oxidized. */
    public static List<String> names(String base) {
        List<String> out = new ArrayList<>();
        STAGES.forEach(s -> out.add(s + "_" + base));
        out.add("waxed_" + base);
        STAGES.forEach(s -> out.add("waxed_" + s + "_" + base));
        return out;
    }

    /** All created block paths (both forms). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        CHAINS.forEach(c -> out.addAll(names(c.id().getPath())));
        return out;
    }

    /** Call after {@link BlocksRegistry#registerBlocks} registered the two base blocks. */
    public static void registerBlocks() {
        CHAINS.forEach(AgingChain::registerBlocks);
    }

    public static void registerItems() {
        CHAINS.forEach(AgingChain::registerItems);
    }

    /** The stages of the form whose base is {@code base} (empty for any other block), for the tab right behind the base. */
    public static List<ItemStack> tabStacks(String base) {
        List<ItemStack> out = new ArrayList<>();
        for (AgingChain c : CHAINS) {
            if (c.id().getPath().equals(base)) c.items().forEach(i -> out.add(new ItemStack(i)));
        }
        return out;
    }
}
