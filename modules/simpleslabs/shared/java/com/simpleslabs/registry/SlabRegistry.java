package com.simpleslabs.registry;

import com.simpleslabs.SimpleSlabs;
import com.simpleslabs.block.HalfSlabBlock;
import com.simpleslabs.block.LayeredSlabBlock;
import com.simpleslabs.block.SideHalfSlabBlock;
import com.simpleslabs.item.HalfSlabItem;
import com.simplelib.api.shape.ShapeMaterials;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/**
 * Registration from the generated {@link HalfSlabMaterials} (tools/textures/half_slab_materials.json): per material the half
 * slab ({@code <slab path without _slab>_half_slab}, with item) and its side form ({@code <...>_side_half_slab}, no item). Like
 * the octets, another mod's half slab exists only while every mod in {@code mods} is loaded; its properties come from SimpleLib's
 * ShapeMaterials template and the real slab is read late.
 */
public final class SlabRegistry {
    public static final List<HalfSlabBlock> HALF_SLABS = new ArrayList<>();
    public static final List<SideHalfSlabBlock> SIDE_SLABS = new ArrayList<>();
    public static final List<Item> ITEMS = new ArrayList<>();
    /** The grass half slabs (main and side form): tinted by the biome on the client. */
    public static final List<Block> GRASS = new ArrayList<>();
    private static final Map<Block, HalfSlabMaterials.Material> MATERIALS = new HashMap<>();
    private static boolean blocks;
    private static boolean items;

    private SlabRegistry() {}

    public static String sidePath(String halfSlabPath) {
        return halfSlabPath.substring(0, halfSlabPath.length() - "half_slab".length()) + "side_half_slab";
    }

    /**
     * Properties template of a material: the Vanilla slab itself, else a Vanilla full block, else SimpleLib's rule for the other
     * mod's full block (or its slab stem when the full block is unknown, e.g. the chess checkers).
     */
    static ShapeMaterials.@Nullable Template template(HalfSlabMaterials.Material m) {
        Identifier slab = Identifier.parse(m.slab());
        if ("minecraft".equals(slab.getNamespace())) return BuiltInRegistries.BLOCK.getOptional(slab).map(ShapeMaterials::vanilla).orElse(null);
        if (m.base() != null) {
            Identifier base = Identifier.parse(m.base());
            if ("minecraft".equals(base.getNamespace())) return BuiltInRegistries.BLOCK.getOptional(base).map(ShapeMaterials::vanilla).orElse(null);
            return ShapeMaterials.template(base.getNamespace(), base.getPath());
        }
        return ShapeMaterials.template(slab.getNamespace(), slab.getPath().substring(0, slab.getPath().length() - "_slab".length()));
    }

    public static synchronized void registerBlocks() {
        if (blocks) return;
        blocks = true;
        for (HalfSlabMaterials.Material m : HalfSlabMaterials.ALL) {
            if (!m.mods().stream().allMatch(SimpleSlabs::isLoaded)) continue;
            ShapeMaterials.Template t = template(m);
            if (t == null) continue;
            Identifier slabId = Identifier.parse(m.slab());
            Supplier<Block> slab = () -> BuiltInRegistries.BLOCK.getValue(slabId);
            HalfSlabBlock main = Registry.register(BuiltInRegistries.BLOCK, SimpleSlabs.id(m.id()),
                    new HalfSlabBlock(t.properties(key(m.id())), m.falls(), slab));
            String sideId = sidePath(m.id());
            SideHalfSlabBlock side = Registry.register(BuiltInRegistries.BLOCK, SimpleSlabs.id(sideId),
                    new SideHalfSlabBlock(t.properties(key(sideId)), main));
            main.setSide(side);
            HALF_SLABS.add(main);
            SIDE_SLABS.add(side);
            MATERIALS.put(main, m);
            MATERIALS.put(side, m);
            if (m.grass()) {
                GRASS.add(main);
                GRASS.add(side);
            }
        }
        linkCopperAging();
    }

    /**
     * N86-7: copper half slabs age like Vanilla copper, the waxed ones keep their look (honeycomb, axe and dispenser work like on
     * copper); the side forms age in their own chains. SimpleLib CopperAging links the registered blocks.
     */
    private static void linkCopperAging() {
        Map<String, Block> main = new LinkedHashMap<>();
        Map<String, Block> side = new LinkedHashMap<>();
        for (HalfSlabBlock block : HALF_SLABS) {
            if (!"copper".equals(MATERIALS.get(block).group())) continue;
            main.put(BuiltInRegistries.BLOCK.getKey(block).getPath(), block);
            side.put(BuiltInRegistries.BLOCK.getKey(block.side()).getPath(), block.side());
        }
        com.simplelib.api.aging.CopperAging.link(SimpleSlabs.MOD_ID, main);
        com.simplelib.api.aging.CopperAging.link(SimpleSlabs.MOD_ID, side);
    }

    public static synchronized void registerItems() {
        if (items) return;
        items = true;
        com.simpleslabs.guide.SlabsGuide.register();
        for (HalfSlabBlock block : HALF_SLABS) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            ITEMS.add(Registry.register(BuiltInRegistries.ITEM, id, new HalfSlabItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix())));
        }
    }

    /** Wood half slabs burn like planks (not Nether wood), wool like wool - the octets' odds. */
    public static void registerFlammability() {
        for (Block block : MATERIALS.keySet()) {
            String group = MATERIALS.get(block).group();
            if ("wood".equals(group) && block.defaultBlockState().ignitedByLava()) ShapeMaterials.setFlammable(block, 5, 20);
            if ("wool".equals(group)) ShapeMaterials.setFlammable(block, 30, 60);
        }
    }

    public static HalfSlabMaterials.@Nullable Material material(Block block) {
        return MATERIALS.get(block);
    }

    public static void registerTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleSlabs.id("main"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemgroup.simpleslabs.half_slabs"))
                        .icon(() -> new ItemStack(ITEMS.isEmpty() ? Items.OAK_SLAB : ITEMS.get(0)))
                        .displayItems((params, out) -> {
                            tabStacks().forEach(out::accept);
                            if (SimpleSlabs.on()) out.accept(com.simpleslabs.guide.SlabsGuide.stack());
                        })
                        .build());
    }

    /** Tab order: the material list's order (groups as in Simple Octets: wood, stone ... concrete, terracotta, mod, other). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleSlabs.on()) return out;
        ITEMS.forEach(item -> out.add(new ItemStack(item)));
        return out;
    }

    private static ResourceKey<Block> key(String path) {
        return ResourceKey.create(Registries.BLOCK, SimpleSlabs.id(path));
    }

    /** Every half slab block of one form, for tests and the client. */
    public static List<LayeredSlabBlock> all() {
        List<LayeredSlabBlock> out = new ArrayList<>(HALF_SLABS);
        out.addAll(SIDE_SLABS);
        return out;
    }
}
