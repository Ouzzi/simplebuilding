package com.simplebuilding.woodwork;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.jspecify.annotations.Nullable;

/**
 * The woodwork family (docs/ai/PLAN-HOLZWERK-2026-10-09.md, {@code McVersion.WOODWORK}): per wood type a hollow log
 * and a hollow stripped log, a sheet and a stripped sheet, a wooden cauldron, a crate and carved wood. Blocks are
 * registered from {@code ModBlocks}, items from {@code ModItems}; without the flag every list is empty.
 */
public final class WoodBlocks {
    /** All blocks of one wood type. */
    public record Family(WoodKind wood, HollowLogBlock hollow, HollowLogBlock hollowStripped, Block sheet, Block strippedSheet,
                         WoodenCauldronBlock cauldron, CrateBlock crate, CarvedLogBlock carved) {
        /** In creative-tab order. */
        public List<Block> blocks() {
            return List.of(this.hollow, this.hollowStripped, this.sheet, this.strippedSheet, this.cauldron, this.crate, this.carved);
        }
    }

    private static final List<Item> ITEMS = new ArrayList<>();

    private WoodBlocks() {
    }

    public static String hollowId(WoodKind wood, boolean stripped) {
        return "hollow_" + (stripped ? wood.strippedLog() : wood.log());
    }

    public static String sheetId(WoodKind wood, boolean stripped) {
        return (stripped ? "stripped_" : "") + wood.id() + "_sheet";
    }

    public static String cauldronId(WoodKind wood) {
        return wood.id() + "_cauldron";
    }

    public static String crateId(WoodKind wood) {
        return wood.id() + "_crate";
    }

    public static String carvedId(WoodKind wood) {
        return "carved_" + wood.log();
    }

    /** Registers every block; called once from {@code ModBlocks}. */
    public static List<Family> registerBlocks() {
        List<Family> out = new ArrayList<>();
        for (WoodKind wood : WoodKind.values()) {
            Block log = wood.logBlock();
            Block stripped = wood.strippedBlock();
            Block planks = wood.planks();
            HollowLogBlock hollow = register(hollowId(wood, false), log,
                    s -> new HollowLogBlock(wood, false, tube(s)));
            HollowLogBlock hollowStripped = register(hollowId(wood, true), stripped,
                    s -> new HollowLogBlock(wood, true, tube(s)));
            Block sheet = register(sheetId(wood, false), planks,
                    s -> new IronBarsBlock(s.mapColor(log.defaultMapColor()).strength(1.0F).noOcclusion()));
            Block strippedSheet = register(sheetId(wood, true), planks,
                    s -> new IronBarsBlock(s.mapColor(stripped.defaultMapColor()).strength(1.0F).noOcclusion()));
            WoodenCauldronBlock cauldron = register(cauldronId(wood), planks,
                    s -> new WoodenCauldronBlock(wood, s.strength(2.0F).noOcclusion()));
            CrateBlock crate = register(crateId(wood), planks,
                    s -> new CrateBlock(wood, s.strength(1.5F).noOcclusion()));
            CarvedLogBlock carved = register(carvedId(wood), stripped,
                    s -> new CarvedLogBlock(wood, s.mapColor(stripped.defaultMapColor())));
            out.add(new Family(wood, hollow, hollowStripped, sheet, strippedSheet, cauldron, crate, carved));
        }
        return List.copyOf(out);
    }

    /** Tube properties: see-through, no suffocation (the log's map colour per axis stays). */
    private static BlockBehaviour.Properties tube(BlockBehaviour.Properties settings) {
        return settings.noOcclusion().isSuffocating((state, level, pos) -> false);
    }

    @SuppressWarnings("unchecked")
    private static <B extends Block> B register(String name, Block base, Function<BlockBehaviour.Properties, B> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, id);
        B block = factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key));
        return (B) Registry.register(BuiltInRegistries.BLOCK, id, block);
    }

    /** Registers the block items through {@code registrar} (name, factory) - from {@code ModItems}. */
    public static void registerItems(BiFunction<String, Function<Item.Properties, Item>, Item> registrar) {
        for (Family family : families()) {
            for (Block block : family.blocks()) {
                String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
                int fuel = family.wood().nether() ? 0 : block instanceof IronBarsBlock ? 100 : 300;
                ITEMS.add(registrar.apply(name, s -> new BlockItem(block, fuel > 0 ? McVersion.cookingFuel(s, fuel) : s)));
            }
        }
    }

    /** Burnable woods catch fire like logs (tubes, carved wood) and planks (sheets, cauldrons, crates). */
    public static void registerFlammability() {
        com.simplebuilding.mixin.FireBlockFlammableInvoker fire = (com.simplebuilding.mixin.FireBlockFlammableInvoker) Blocks.FIRE;
        for (Family family : families()) {
            if (family.wood().nether()) {
                continue;
            }
            fire.simplebuilding$setFlammable(family.hollow(), 5, 5);
            fire.simplebuilding$setFlammable(family.hollowStripped(), 5, 5);
            fire.simplebuilding$setFlammable(family.carved(), 5, 5);
            fire.simplebuilding$setFlammable(family.sheet(), 5, 20);
            fire.simplebuilding$setFlammable(family.strippedSheet(), 5, 20);
            fire.simplebuilding$setFlammable(family.cauldron(), 5, 20);
            fire.simplebuilding$setFlammable(family.crate(), 5, 20);
        }
    }

    public static List<Family> families() {
        return ModBlocks.WOOD_FAMILIES;
    }

    /** Every woodwork item in creative-tab order (wood by wood). */
    public static List<Item> items() {
        return List.copyOf(ITEMS);
    }

    /** All crates (block entity type). */
    public static Block[] crates() {
        return families().stream().map(Family::crate).toArray(Block[]::new);
    }

    /** All wooden cauldrons (water tint). */
    public static Block[] cauldrons() {
        return families().stream().map(Family::cauldron).toArray(Block[]::new);
    }

    public static List<Block> blocks() {
        List<Block> out = new ArrayList<>();
        families().forEach(f -> out.addAll(f.blocks()));
        return out;
    }

    public static @Nullable Family family(WoodKind wood) {
        for (Family family : families()) {
            if (family.wood() == wood) {
                return family;
            }
        }
        return null;
    }

    /** The carved wood a stripped log turns into, or null. */
    public static @Nullable CarvedLogBlock carvedFor(Block strippedLog) {
        for (Family family : families()) {
            if (family.wood().strippedBlock() == strippedLog) {
                return family.carved();
            }
        }
        return null;
    }

    /** Families keyed by wood, for tests and datagen. */
    public static Map<WoodKind, Family> byWood() {
        Map<WoodKind, Family> map = new EnumMap<>(WoodKind.class);
        families().forEach(f -> map.put(f.wood(), f));
        return map;
    }
}
