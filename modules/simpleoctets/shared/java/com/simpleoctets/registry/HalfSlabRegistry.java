package com.simpleoctets.registry;

import com.simpleoctets.SimpleOctets;
import com.simpleoctets.block.HalfSlabBlock;
import com.simpleoctets.item.HalfSlabItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * N86 half slabs (DRAFT, uncompiled): registration from the generated {@link HalfSlabMaterials} list (made from
 * tools/textures/half_slab_materials.json). Called from {@link OctetRegistry} (blocks, items, fire, tab), so the three loader
 * entry points stay as they are. Same rule as the octets: another mod's half slab only while that mod (all of
 * {@code mods}) is loaded; properties from a fixed Vanilla template, the real slab read late.
 */
public final class HalfSlabRegistry {
    public static final List<Block> BLOCKS = new ArrayList<>();
    public static final List<Item> ITEMS = new ArrayList<>();
    private static final Map<Block, String> GROUPS = new HashMap<>();
    private static boolean blocks;
    private static boolean items;

    private HalfSlabRegistry() {}

    public static synchronized void registerBlocks() {
        if (blocks) return;
        blocks = true;
        for (HalfSlabMaterials.Material m : HalfSlabMaterials.ALL) {   // N86-5: the list holds the agreed scope
            if (!m.mods().stream().allMatch(SimpleOctets::isLoaded)) continue;
            Block template = BuiltInRegistries.BLOCK.getOptional(Identifier.parse(m.template())).orElse(null);
            if (template == null) continue;
            Identifier slabId = Identifier.parse(m.slab());
            Supplier<Block> slab = () -> BuiltInRegistries.BLOCK.getValue(slabId);
            // N86-7: copper half slabs are static (one per oxidation stage, waxed or not) like the copper octets - no aging.
            int light = m.light() > 0 ? m.light() : template.defaultBlockState().getLightEmission();
            ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleOctets.id(m.id()));
            // Pinned map colour and light: state-dependent lambdas of the template (log axis, lit) would throw here.
            BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(template).setId(key)
                    .mapColor(template.defaultMapColor()).lightLevel(state -> light);
            Block block = Registry.register(BuiltInRegistries.BLOCK, SimpleOctets.id(m.id()), new HalfSlabBlock(slab, properties));
            BLOCKS.add(block);
            GROUPS.put(block, m.group());
        }
    }

    public static synchronized void registerItems() {
        if (items) return;
        items = true;
        for (Block block : BLOCKS) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            ITEMS.add(Registry.register(BuiltInRegistries.ITEM, id,
                    new HalfSlabItem((HalfSlabBlock) block, new Item.Properties().setId(key).useBlockDescriptionPrefix())));
        }
    }

    /** Wood half slabs burn like planks (not Nether wood), wool like wool (same values as the octets). */
    public static void registerFlammability() {
        var fire = (com.simpleoctets.mixin.FireBlockAccessor) net.minecraft.world.level.block.Blocks.FIRE;
        for (Block block : BLOCKS) {
            String group = GROUPS.get(block);
            if ("wood".equals(group) && block.defaultBlockState().ignitedByLava()) fire.simpleoctets$setFlammable(block, 5, 20);
            if ("wool".equals(group)) fire.simpleoctets$setFlammable(block, 30, 60);
        }
    }

    /** Tab order: behind the octets, in {@link HalfSlabMaterials#GROUPS} order (the list is sorted that way). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        ITEMS.forEach(item -> out.add(new ItemStack(item)));
        return out;
    }
}
