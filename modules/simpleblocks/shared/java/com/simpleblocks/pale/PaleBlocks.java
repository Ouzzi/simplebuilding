package com.simpleblocks.pale;

import com.simpleblocks.SimpleBlocks;
import com.simplelib.api.aging.AgingChain;
import com.simplelib.api.aging.AgingCondition;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/**
 * DW04B-2/3 (docs/ai/PLAN-B-PALE01-2026-11-04.md): pumpkins, carved pumpkins, jack o'lanterns, melons and hay bales placed in
 * the Pale Garden fade in three stages (faded, bleached, pale = end stage) like copper weathers; a honeycomb keeps them as
 * they are, an axe scrapes the wax or a stage off. Built on SimpleLib's {@link AgingChain}: every stage is the Vanilla block's
 * class with its properties, so it behaves like the Vanilla block (light, golems, fall damage on hay, melon slices); only the
 * pumpkin differs ({@link PalePumpkinBlock}: shears keep the stage). The five chains age as one front (group {@code pale}).
 */
public final class PaleBlocks {
    /** Biomes where the blocks fade (data: {@code minecraft:pale_garden}). */
    public static final TagKey<Biome> PALES_BLOCKS = TagKey.create(Registries.BIOME, SimpleBlocks.id("pales_blocks"));
    /** Carved pumpkins and jack o'lanterns of every stage: golem heads like the Vanilla ones (PaleGolemHeadMixin). */
    public static final TagKey<Block> GOLEM_HEADS = TagKey.create(Registries.BLOCK, SimpleBlocks.id("golem_heads"));
    public static final Identifier GROUP = SimpleBlocks.id("pale");
    public static final List<String> STAGES = List.of("faded", "bleached", "pale");

    public static final AgingChain PUMPKIN = chain("pumpkin", "pumpkin", Blocks.PUMPKIN, MapColor.QUARTZ,
            PalePumpkinBlock::new, p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
    public static final AgingChain CARVED_PUMPKIN = chain("carved_pumpkin", "carvedPumpkin", Blocks.CARVED_PUMPKIN, MapColor.QUARTZ, null,
            p -> net.minecraft.world.waypoints.Waypoint.addHideAttribute(p)
                    .component(net.minecraft.core.component.DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD).setSwappable(false)
                            .setCameraOverlay(Identifier.withDefaultNamespace("misc/pumpkinblur")).build())
                    .compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
    public static final AgingChain JACK_O_LANTERN = chain("jack_o_lantern", "jackOLantern", Blocks.JACK_O_LANTERN, MapColor.QUARTZ, null, p -> p);
    public static final AgingChain MELON = chain("melon", "melon", Blocks.MELON, MapColor.GLOW_LICHEN, null,
            p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM));
    public static final AgingChain HAY_BLOCK = chain("hay_block", "hayBlock", Blocks.HAY_BLOCK, MapColor.QUARTZ, null,
            p -> p.compostable(ContextIntProviders.COMPOSTABLE_MEDIUM_HIGH));

    public static final List<AgingChain> CHAINS = List.of(PUMPKIN, CARVED_PUMPKIN, JACK_O_LANTERN, MELON, HAY_BLOCK);

    private PaleBlocks() {}

    /**
     * One chain: Vanilla block -> faded_x, bleached_x, pale_x (+ waxed_x, waxed_faded_x, waxed_bleached_x); from stage 2 on the
     * map shows them pale. Ages only in the Pale Garden, with the chance of {@code pale.<configKey>}.
     */
    private static AgingChain chain(String name, String configKey, Block base, MapColor pale,
                                    java.util.function.Function<net.minecraft.world.level.block.state.BlockBehaviour.Properties, Block> factory,
                                    java.util.function.UnaryOperator<Item.Properties> item) {
        AgingChain.Builder b = AgingChain.builder(SimpleBlocks.id(name), base)
                .stages(STAGES.stream().map(s -> s + "_" + name).toArray(String[]::new))
                .group(GROUP)
                .when(AgingCondition.biome(PALES_BLOCKS))
                .chance(() -> SimpleBlocks.on() ? PaleConfig.chance(configKey) : 0)
                .properties((stage, p) -> stage >= 2 ? p.mapColor(pale) : p)
                .item(item)
                .hint("tooltip.simpleblocks.pale");
        if (factory != null) b.factory(factory);
        return b.build();
    }

    /** Every created block path in tab order (per chain: faded, bleached, pale, then the waxed ones). */
    public static List<String> names() {
        List<String> out = new ArrayList<>();
        for (AgingChain c : CHAINS) {
            String base = c.id().getPath();
            STAGES.forEach(s -> out.add(s + "_" + base));
            out.add("waxed_" + base);
            STAGES.subList(0, 2).forEach(s -> out.add("waxed_" + s + "_" + base));
        }
        return out;
    }

    public static void registerBlocks() {
        CHAINS.forEach(AgingChain::registerBlocks);
    }

    public static void registerItems() {
        CHAINS.forEach(AgingChain::registerItems);
    }

    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        CHAINS.forEach(c -> c.items().forEach(i -> out.add(new ItemStack(i))));
        return out;
    }
}
