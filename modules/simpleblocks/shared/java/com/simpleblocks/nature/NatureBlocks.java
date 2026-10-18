package com.simpleblocks.nature;

import com.simpleblocks.SimpleBlocks;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The building blocks that moved here from SimpleBuilding (docs/ai/PLAN-B-BLOCKSMOVE-2026-10-18.md): sand and gravel
 * without gravity (suspended) and with reversed gravity (levitating, rising as {@link LevitatingBlockEntity}), dirt, grass,
 * sand and gravel slabs (two make the full Vanilla block; sand and gravel slabs fall), cracked ice (cracks under whoever
 * stands on it), chiseled packed and blue ice, the nautilus shell block and three more froglight colors. Old worlds keep
 * them: {@link SimpleBlocks#registerIdMoves} maps every {@code simplebuilding:} id here. Properties are copied from the
 * Vanilla block they vary, like in SimpleBuilding.
 */
public final class NatureBlocks {
    /** Block path -> block, in creative-tab order (gravity, slabs, ice and shell, froglights). */
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** The rising block; set when the entity types register. */
    public static EntityType<LevitatingBlockEntity> LEVITATING_BLOCK;
    public static final String LEVITATING_ENTITY = "levitating_block";

    // Both suspended blocks have a full collision box: you stand on them, items stay on top and rising blocks land below.
    public static final Block SUSPENDED_SAND = add("suspended_sand", Blocks.SAND, Block::new);
    public static final Block SUSPENDED_GRAVEL = add("suspended_gravel", Blocks.GRAVEL, Block::new);
    public static final Block LEVITATING_SAND = add("levitating_sand", Blocks.SAND, LevitatingBlock::new);
    public static final Block LEVITATING_GRAVEL = add("levitating_gravel", Blocks.GRAVEL, LevitatingBlock::new);
    public static final Block DIRT_SLAB = add("dirt_slab", Blocks.DIRT, s -> new MergingSlabBlock(s, Blocks.DIRT));
    /** Tinted by the biome like the grass block; without Silk Touch it drops the dirt slab. Never spreads, never dies. */
    public static final Block GRASS_SLAB = add("grass_slab", Blocks.GRASS_BLOCK, s -> new MergingSlabBlock(s, Blocks.GRASS_BLOCK));
    /** Fall like sand and gravel ({@link FallingSlabBlock}); two slabs make the full Vanilla block ({@link MergingSlabBlock}). */
    public static final Block SAND_SLAB = add("sand_slab", Blocks.SAND, s -> new FallingSlabBlock(s, Blocks.SAND));
    public static final Block GRAVEL_SLAB = add("gravel_slab", Blocks.GRAVEL, s -> new FallingSlabBlock(s, Blocks.GRAVEL));
    /** Cracks in four stages under living entities and then breaks away ({@link CrackedIceBlock}). */
    public static final Block CRACKED_ICE = add("cracked_ice", Blocks.ICE, CrackedIceBlock::new);
    public static final Block CHISELED_PACKED_ICE = add("chiseled_packed_ice", Blocks.PACKED_ICE, Block::new);
    public static final Block CHISELED_BLUE_ICE = add("chiseled_blue_ice", Blocks.BLUE_ICE, Block::new);
    /** A pillar like the bone block: spiral on the ends, striped sides. */
    public static final Block NAUTILUS_SHELL_BLOCK = add("nautilus_shell_block", Blocks.BONE_BLOCK,
            s -> new RotatedPillarBlock(s.mapColor(MapColor.TERRACOTTA_WHITE)));
    /** Froglights in the gaps of the three Vanilla colors: red, cyan, blue. */
    public static final Block SCARLET_FROGLIGHT = froglight("scarlet_froglight", MapColor.COLOR_RED);
    public static final Block AQUA_FROGLIGHT = froglight("aqua_froglight", MapColor.COLOR_CYAN);
    public static final Block AZURE_FROGLIGHT = froglight("azure_froglight", MapColor.COLOR_BLUE);

    /** Every moved block path in registration (and tab) order. */
    public static final List<String> NAMES = List.copyOf(BLOCKS.keySet());
    /** Groups of the creative tab, in order. */
    public static final List<List<String>> ROWS = List.of(NAMES.subList(0, 4), NAMES.subList(4, 8), NAMES.subList(8, 12), NAMES.subList(12, 15));

    /** Recipe ids that came from SimpleBuilding (recipe book) -> folder of their unlock advancement {@code recipes/<folder>/<path>}. */
    public static final Map<String, String> MOVED_RECIPES = movedRecipes();

    private NatureBlocks() {}

    private static Map<String, String> movedRecipes() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String path : List.of("suspended_sand", "suspended_gravel", "levitating_sand", "levitating_gravel", "dirt_slab", "grass_slab",
                "sand_slab", "gravel_slab", "cracked_ice_from_ice_stonecutting", "chiseled_packed_ice_from_packed_ice_stonecutting",
                "chiseled_blue_ice_from_blue_ice_stonecutting", "nautilus_shell_block", "scarlet_froglight", "aqua_froglight", "azure_froglight")) {
            out.put(path, "building_blocks");
        }
        out.put("nautilus_shell_from_nautilus_shell_block", "misc");
        return Map.copyOf(out);
    }

    private static Block froglight(String name, MapColor color) {
        return add(name, Blocks.OCHRE_FROGLIGHT, s -> new RotatedPillarBlock(s.mapColor(color)));
    }

    private static Block add(String name, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleBlocks.id(name));
        Block block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key));
        BLOCKS.put(name, block);
        return block;
    }

    public static void registerBlocks() {
        BLOCKS.forEach((name, block) -> Registry.register(BuiltInRegistries.BLOCK, SimpleBlocks.id(name), block));
    }

    public static void registerItems() {
        BLOCKS.forEach((name, block) -> {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleBlocks.id(name));
            Item.Properties properties = new Item.Properties().setId(key).useBlockDescriptionPrefix();
            String tooltip = name.startsWith("levitating_") ? "levitating" : name.startsWith("suspended_") ? "suspended" : null;
            Item item = tooltip == null ? new BlockItem(block, properties) : new GravityBlockItem(block, properties, tooltip);
            ITEMS.put(name, Registry.register(BuiltInRegistries.ITEM, key, item));
        });
    }

    /**
     * The rising block: built like Vanilla's {@code falling_block} - the same 0.98 hit box, so {@code blockPosition()} meets
     * the free cell under a ceiling, and no loot table.
     */
    public static void registerEntityTypes() {
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SimpleBlocks.id(LEVITATING_ENTITY));
        EntityType<LevitatingBlockEntity> type = EntityType.Builder.<LevitatingBlockEntity>of(LevitatingBlockEntity::new, MobCategory.MISC)
                .noLootTable()
                .sized(0.98F, 0.98F)
                .clientTrackingRange(10)
                .updateInterval(20)
                .build(key);
        LEVITATING_BLOCK = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    /** Suspended and levitating sand/gravel: one line on what the block does (it looks like plain sand). */
    static final class GravityBlockItem extends BlockItem {
        private final String tooltip;

        GravityBlockItem(Block block, Item.Properties properties, String tooltip) {
            super(block, properties);
            this.tooltip = tooltip;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            lines.accept(Component.translatable("tooltip." + SimpleBlocks.MOD_ID + "." + tooltip).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}
