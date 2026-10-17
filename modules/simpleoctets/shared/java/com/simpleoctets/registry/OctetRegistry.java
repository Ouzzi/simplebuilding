package com.simpleoctets.registry;

import com.simpleoctets.SimpleOctets;
import com.simpleoctets.block.MaterialOctetBlock;
import com.simpleoctets.block.MixedOctetBlock;
import com.simpleoctets.block.MixedOctetBlockEntity;
import com.simpleoctets.item.MaterialOctetItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

/**
 * Loader-neutral registration (split stage 6, docs/ai/PLAN-S6-OCTETS-2026-10-17.md): blocks, items, the mixed cell's block
 * entity, fire table, creative tab. Same ids and properties as SimpleBuilding had them ({@code simplebuilding:<x>_octet}
 * before). Octets of another mod's block (SimpleBuilding's palettes, Simple End's woods and logs) exist only while that
 * mod is loaded; their properties come from a fixed template and the source block is read late, so the load order of the
 * mods does not matter.
 */
public final class OctetRegistry {
    /** Woods with planks, stairs and slab (order = tab order); Ash-/Duskwood belong to Simple End. */
    public static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "pale_oak", "poplar", "bamboo", "crimson", "warped", "ashwood", "duskwood");
    public static final String SIMPLE_END = "simpleend";
    private static final Set<String> END_WOODS = Set.of("ashwood", "duskwood");
    /** Vanilla template of another mod's block whose properties that mod copies (Simple Blocks, BlocksRegistry). */
    private static final Map<String, Block> TEMPLATES = Map.of("blue_nether_bricks", Blocks.RED_NETHER_BRICKS,
            "hushed_deepslate_tiles", Blocks.DEEPSLATE_TILES);
    /** Octets stack to the engine maximum (owner N18: "128"; item stacks are saved with at most 99). */
    public static final int MAX_STACK = 99;

    public static final List<Block> WOOD_OCTETS = new ArrayList<>();
    public static final List<Block> MATERIAL_OCTETS = new ArrayList<>();
    public static final List<Item> WOOD_OCTET_ITEMS = new ArrayList<>();
    public static final List<Item> MATERIAL_OCTET_ITEMS = new ArrayList<>();
    private static final Map<Block, String> GROUPS = new HashMap<>();
    /** The melon octet: a placed melon slice (sneak + right-click); drops melon slices. */
    public static Block MELON_OCTET;
    /** Mixed cell (Queue N48): octets of several materials, material per octet in the block entity. No item, no loot table. */
    public static Block MIXED_OCTET;
    public static BlockEntityType<MixedOctetBlockEntity> MIXED_OCTET_BE;
    private static boolean blocks;
    private static boolean items;

    /** Namespace of a wood's planks. */
    public static String woodNamespace(String wood) {
        return END_WOODS.contains(wood) ? SIMPLE_END : "minecraft";
    }

    /** The octet woods of this game (Simple End woods only with Simple End). */
    public static List<String> activeWoods() {
        return WOODS.stream().filter(w -> "minecraft".equals(woodNamespace(w)) || SimpleOctets.isLoaded(woodNamespace(w))).toList();
    }

    public static synchronized void registerBlocks() {
        if (blocks) return;
        blocks = true;
        for (String wood : activeWoods()) {
            String name = wood + "_octet";
            Identifier planksId = Identifier.fromNamespaceAndPath(woodNamespace(wood), wood + "_planks");
            if (!"minecraft".equals(planksId.getNamespace())) {
                // Simple End planks: properties of oak planks (Simple End's own may not be registered yet), source read late.
                MapColor color = "ashwood".equals(wood) ? MapColor.COLOR_LIGHT_GRAY : MapColor.TERRACOTTA_LIGHT_GRAY;
                WOOD_OCTETS.add(block(name, Blocks.OAK_PLANKS, s -> new MaterialOctetBlock(() -> BuiltInRegistries.BLOCK.getValue(planksId),
                        piece(name), properties(s.mapColor(color).lightLevel(state -> 0)))));
                continue;
            }
            Block planks = BuiltInRegistries.BLOCK.getOptional(planksId).orElse(null);
            if (planks == null) continue;
            WOOD_OCTETS.add(block(name, planks, s -> new MaterialOctetBlock(planks, piece(name), properties(s, planks))));
        }
        MELON_OCTET = block("melon_octet", Blocks.MELON, s -> new MaterialOctetBlock(Blocks.MELON, () -> Items.MELON_SLICE, properties(s, Blocks.MELON)));
        for (OctetMaterials.Material material : OctetMaterials.ALL) {
            Block cell = materialCell(material);
            if (cell != null) {
                GROUPS.put(cell, material.group());
                MATERIAL_OCTETS.add(cell);
            }
        }
        MIXED_OCTET = block("mixed_octet", null, unused -> new MixedOctetBlock(BlockBehaviour.Properties.of().setId(key("mixed_octet"))
                .mapColor(MapColor.STONE).strength(0.8F).sound(SoundType.STONE).noLootTable().isValidSpawn((state, world, pos, type) -> false)));
    }

    private static @Nullable Block materialCell(OctetMaterials.Material material) {
        String name = material.key() + "_octet";
        Identifier sourceId = Identifier.fromNamespaceAndPath(material.namespace(), material.base());
        if (!"minecraft".equals(material.namespace())) {
            // Another mod's block: only with that mod; a fixed template, the source read when used.
            if (!SimpleOctets.isLoaded(material.namespace())) return null;
            Supplier<Block> source = () -> BuiltInRegistries.BLOCK.getValue(sourceId);
            if (SIMPLE_END.equals(material.namespace())) {
                boolean stripped = material.base().startsWith("stripped_");
                boolean ash = material.base().contains("ashwood");
                MapColor color = ash ? (stripped ? MapColor.COLOR_LIGHT_GRAY : MapColor.COLOR_GRAY)
                        : (stripped ? MapColor.TERRACOTTA_LIGHT_GRAY : MapColor.TERRACOTTA_GRAY);
                return block(name, stripped ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_LOG, s -> new MaterialOctetBlock(source, piece(name),
                        properties(s.mapColor(color).lightLevel(state -> 0))));
            }
            Block template = TEMPLATES.get(material.base());
            if (template != null) {
                // Simple Blocks (blue nether bricks, hushed deepslate tiles): the Vanilla block it copies itself.
                return block(name, template, s -> new MaterialOctetBlock(source, piece(name), properties(s, template)));
            }
            // SimpleBuilding's palettes (Astralit, Nihilith, Ender Quartz): End stone like the astral/nihil End stone they
            // are made of; Astralit glows like it (light 10), Ender Quartz is purple.
            int light = material.base().contains("astralit") ? 10 : 0;
            MapColor color = material.base().contains("ender_quartz") ? MapColor.COLOR_PURPLE : Blocks.END_STONE.defaultMapColor();
            return block(name, Blocks.END_STONE, s -> new MaterialOctetBlock(source, piece(name),
                    properties(s.mapColor(color).lightLevel(state -> light))));
        }
        Block source = BuiltInRegistries.BLOCK.getOptional(sourceId).orElse(null);
        if (source == null) return null;
        return block(name, source, s -> new MaterialOctetBlock(source, piece(name), properties(s, source)));
    }

    public static synchronized void registerItems() {
        if (items) return;
        items = true;
        com.simpleoctets.guide.OctetsGuide.register();
        for (Block cell : WOOD_OCTETS) {
            WOOD_OCTET_ITEMS.add(item(BuiltInRegistries.BLOCK.getKey(cell).getPath(), s -> new MaterialOctetItem(() -> cell, s.stacksTo(MAX_STACK))));
        }
        for (Block cell : MATERIAL_OCTETS) {
            String name = BuiltInRegistries.BLOCK.getKey(cell).getPath();
            // Netherite octet like the Netherite block: uncommon and fire resistant.
            boolean netherite = name.startsWith("netherite_");
            MATERIAL_OCTET_ITEMS.add(item(name, s -> new MaterialOctetItem(() -> cell,
                    netherite ? s.stacksTo(MAX_STACK).rarity(Rarity.UNCOMMON).fireResistant() : s.stacksTo(MAX_STACK))));
        }
    }

    public static void registerBlockEntity(BlockEntityType<MixedOctetBlockEntity> type) {
        MIXED_OCTET_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, SimpleOctets.id("mixed_octet"), type);
    }

    /** Wood octets burn like their planks (not Nether wood), wool like wool, logs like logs. */
    public static void registerFlammability() {
        var fire = (com.simpleoctets.mixin.FireBlockAccessor) Blocks.FIRE;
        for (Block cell : WOOD_OCTETS) {
            if (cell.defaultBlockState().ignitedByLava()) fire.simpleoctets$setFlammable(cell, 5, 20);
        }
        for (Block cell : MATERIAL_OCTETS) {
            if ("wool".equals(GROUPS.get(cell))) fire.simpleoctets$setFlammable(cell, 30, 60);
            if ("log".equals(GROUPS.get(cell)) && cell.defaultBlockState().ignitedByLava()) fire.simpleoctets$setFlammable(cell, 5, 5);
        }
    }

    /** The material octets of one group of {@link OctetMaterials#GROUPS}, in list order. */
    public static List<Block> materialOctets(String group) {
        return MATERIAL_OCTETS.stream().filter(cell -> group.equals(GROUPS.get(cell))).toList();
    }

    /** The group of a material octet cell, null for wood/melon/mixed. */
    public static @Nullable String groupOf(Block cell) {
        return GROUPS.get(cell);
    }

    /** Every cell with an item or a placing item (wood, melon, materials), without the mixed cell. */
    public static List<Block> cells() {
        List<Block> out = new ArrayList<>(WOOD_OCTETS);
        if (MELON_OCTET != null) out.add(MELON_OCTET);
        out.addAll(MATERIAL_OCTETS);
        return out;
    }

    public static void registerTab() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleOctets.id("main"),
                CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                        .title(Component.translatable("itemGroup.simpleoctets"))
                        .icon(() -> new ItemStack(WOOD_OCTET_ITEMS.isEmpty() ? Items.OAK_PLANKS : WOOD_OCTET_ITEMS.get(0)))
                        .displayItems((params, out) -> {
                            tabStacks().forEach(out::accept);
                            if (SimpleOctets.on()) out.accept(com.simpleoctets.guide.OctetsGuide.stack());
                        })
                        .build());
    }

    /** Tab order: wood octets, the melon slice (places the melon octet), then the material groups. */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleOctets.on()) return out;
        WOOD_OCTET_ITEMS.forEach(item -> out.add(new ItemStack(item)));
        out.add(new ItemStack(Items.MELON_SLICE));
        for (String group : OctetMaterials.GROUPS) {
            for (Block cell : materialOctets(group)) {
                out.add(new ItemStack(BuiltInRegistries.ITEM.getValue(BuiltInRegistries.BLOCK.getKey(cell))));
            }
        }
        return out;
    }

    /** Vanilla tabs that get octets: wool, concrete and terracotta to the colored blocks, the rest to the building blocks. */
    public static final List<ResourceKey<CreativeModeTab>> VANILLA_TABS = List.of(CreativeModeTabs.BUILDING_BLOCKS, CreativeModeTabs.COLORED_BLOCKS);
    private static final Set<String> COLORED = Set.of("wool", "concrete", "terracotta");

    /**
     * Vanilla tab placement (per-mod switch "add items to Vanilla tabs"): each octet item of {@code tab} right after the
     * slab of its material, else after its full block; anchor -> octet items in tab order.
     */
    public static Map<Item, List<ItemStack>> vanillaPlacements(ResourceKey<CreativeModeTab> tab) {
        Map<Item, List<ItemStack>> out = new java.util.LinkedHashMap<>();
        if (!SimpleOctets.on()) return out;
        List<Item> octets = new ArrayList<>(WOOD_OCTET_ITEMS);
        octets.addAll(MATERIAL_OCTET_ITEMS);
        for (Item octet : octets) {
            if (!(octet instanceof MaterialOctetItem item) || !(item.cell() instanceof MaterialOctetBlock cell)) continue;
            boolean colored = COLORED.contains(GROUPS.get(cell));
            if (!tab.equals(colored ? CreativeModeTabs.COLORED_BLOCKS : CreativeModeTabs.BUILDING_BLOCKS)) continue;
            Block full = cell.source();
            if (full == null || full == Blocks.AIR) continue;
            Item anchor = slab(BuiltInRegistries.BLOCK.getKey(full));
            out.computeIfAbsent(anchor != null ? anchor : full.asItem(), k -> new ArrayList<>()).add(new ItemStack(octet));
        }
        return out;
    }

    private static @Nullable Item slab(Identifier full) {
        String path = full.getPath();
        List<String> candidates = new ArrayList<>(List.of(path + "_slab"));
        if (path.endsWith("_planks")) candidates.add(path.substring(0, path.length() - 7) + "_slab");
        if (path.endsWith("_block")) candidates.add(path.substring(0, path.length() - 6) + "_slab");
        if (path.endsWith("s")) candidates.add(path.substring(0, path.length() - 1) + "_slab");
        for (String candidate : candidates) {
            Identifier id = Identifier.fromNamespaceAndPath(full.getNamespace(), candidate);
            if (BuiltInRegistries.ITEM.containsKey(id)) return BuiltInRegistries.ITEM.getValue(id);
        }
        return null;
    }

    private static Supplier<Item> piece(String name) {
        Identifier id = SimpleOctets.id(name);
        return () -> BuiltInRegistries.ITEM.getValue(id);
    }

    /**
     * Octet cells have no blockstate properties of the source, so state-dependent lambdas copied from it (log axis in
     * the map colour, lit in the light level) would throw: pin both to the source's default state.
     */
    private static BlockBehaviour.Properties properties(BlockBehaviour.Properties s, Block source) {
        int light = source.defaultBlockState().getLightEmission();
        return properties(s.mapColor(source.defaultMapColor()).lightLevel(state -> light));
    }

    private static BlockBehaviour.Properties properties(BlockBehaviour.Properties s) {
        return s.noLootTable().isValidSpawn((state, world, pos, type) -> false);
    }

    private static ResourceKey<Block> key(String name) {
        return ResourceKey.create(Registries.BLOCK, SimpleOctets.id(name));
    }

    private static Block block(String name, @Nullable Block base, Function<BlockBehaviour.Properties, Block> factory) {
        BlockBehaviour.Properties settings = base == null ? BlockBehaviour.Properties.of().setId(key(name))
                : BlockBehaviour.Properties.ofFullCopy(base).setId(key(name));
        return Registry.register(BuiltInRegistries.BLOCK, SimpleOctets.id(name), factory.apply(settings));
    }

    private static Item item(String name, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleOctets.id(name));
        return Registry.register(BuiltInRegistries.ITEM, SimpleOctets.id(name), factory.apply(new Item.Properties().setId(key)));
    }

    private OctetRegistry() {}
}
