package com.simplebuilding.woodwork;

import com.simplebuilding.platform.ModEnvironment;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;

/**
 * The wood types of the woodwork family (docs/ai/PLAN-HOLZWERK-2026-10-09.md): the Vanilla log, its stripped log
 * and its planks. {@code log} is the Vanilla path stem used in block ids ({@code oak_log}, {@code crimson_stem},
 * {@code bamboo_block}); Nether woods do not burn.
 *
 * <p>Ashwood and Duskwood belong to Simple End (docs/ai/PLAN-ENDWOOD-FULL-2026-10-12.md): they count only while that
 * mod is loaded ({@link #active()}), and their blocks are looked up by id when first needed - never while registering,
 * since Fabric does not order the mods' entrypoints. Their woodwork blocks take the properties of the oak ones
 * ({@link #template}) with their own map colours.
 */
public enum WoodKind {
    OAK("oak", "oak_log", () -> Blocks.OAK_LOG, () -> Blocks.STRIPPED_OAK_LOG, () -> Blocks.OAK_PLANKS, false),
    SPRUCE("spruce", "spruce_log", () -> Blocks.SPRUCE_LOG, () -> Blocks.STRIPPED_SPRUCE_LOG, () -> Blocks.SPRUCE_PLANKS, false),
    BIRCH("birch", "birch_log", () -> Blocks.BIRCH_LOG, () -> Blocks.STRIPPED_BIRCH_LOG, () -> Blocks.BIRCH_PLANKS, false),
    JUNGLE("jungle", "jungle_log", () -> Blocks.JUNGLE_LOG, () -> Blocks.STRIPPED_JUNGLE_LOG, () -> Blocks.JUNGLE_PLANKS, false),
    ACACIA("acacia", "acacia_log", () -> Blocks.ACACIA_LOG, () -> Blocks.STRIPPED_ACACIA_LOG, () -> Blocks.ACACIA_PLANKS, false),
    DARK_OAK("dark_oak", "dark_oak_log", () -> Blocks.DARK_OAK_LOG, () -> Blocks.STRIPPED_DARK_OAK_LOG, () -> Blocks.DARK_OAK_PLANKS, false),
    MANGROVE("mangrove", "mangrove_log", () -> Blocks.MANGROVE_LOG, () -> Blocks.STRIPPED_MANGROVE_LOG, () -> Blocks.MANGROVE_PLANKS, false),
    CHERRY("cherry", "cherry_log", () -> Blocks.CHERRY_LOG, () -> Blocks.STRIPPED_CHERRY_LOG, () -> Blocks.CHERRY_PLANKS, false),
    PALE_OAK("pale_oak", "pale_oak_log", () -> Blocks.PALE_OAK_LOG, () -> Blocks.STRIPPED_PALE_OAK_LOG, () -> Blocks.PALE_OAK_PLANKS, false),
    CRIMSON("crimson", "crimson_stem", () -> Blocks.CRIMSON_STEM, () -> Blocks.STRIPPED_CRIMSON_STEM, () -> Blocks.CRIMSON_PLANKS, true),
    WARPED("warped", "warped_stem", () -> Blocks.WARPED_STEM, () -> Blocks.STRIPPED_WARPED_STEM, () -> Blocks.WARPED_PLANKS, true),
    BAMBOO("bamboo", "bamboo_block", () -> Blocks.BAMBOO_BLOCK, () -> Blocks.STRIPPED_BAMBOO_BLOCK, () -> Blocks.BAMBOO_PLANKS, false),
    ASHWOOD("ashwood", MapColor.COLOR_GRAY, MapColor.COLOR_LIGHT_GRAY),
    DUSKWOOD("duskwood", MapColor.TERRACOTTA_GRAY, MapColor.TERRACOTTA_LIGHT_GRAY);

    /** The mod that owns the End woods. */
    public static final String SIMPLE_END = "simpleend";

    private final String id;
    private final String log;
    private final Supplier<Block> logBlock;
    private final Supplier<Block> strippedBlock;
    private final Supplier<Block> planks;
    private final boolean nether;
    private final String namespace;
    private final MapColor barkColor;
    private final MapColor woodColor;

    WoodKind(String id, String log, Supplier<Block> logBlock, Supplier<Block> strippedBlock, Supplier<Block> planks, boolean nether) {
        this.id = id;
        this.log = log;
        this.logBlock = logBlock;
        this.strippedBlock = strippedBlock;
        this.planks = planks;
        this.nether = nether;
        this.namespace = "minecraft";
        this.barkColor = null;
        this.woodColor = null;
    }

    /** A Simple End wood: blocks {@code simpleend:<id>_log}, {@code stripped_<id>_log}, {@code <id>_planks}. */
    WoodKind(String id, MapColor barkColor, MapColor woodColor) {
        this.id = id;
        this.log = id + "_log";
        this.logBlock = () -> lookup(SIMPLE_END, id + "_log");
        this.strippedBlock = () -> lookup(SIMPLE_END, "stripped_" + id + "_log");
        this.planks = () -> lookup(SIMPLE_END, id + "_planks");
        this.nether = false;
        this.namespace = SIMPLE_END;
        this.barkColor = barkColor;
        this.woodColor = woodColor;
    }

    private static Block lookup(String namespace, String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(namespace, path));
    }

    /** The woods whose blocks exist in this game: Vanilla always, the End woods only with Simple End. */
    public static List<WoodKind> active() {
        return Arrays.stream(values()).filter(WoodKind::available).toList();
    }

    /** Whether the wood's mod is loaded. */
    public boolean available() {
        return !this.external() || ModEnvironment.isModLoaded(this.namespace);
    }

    /** {@code minecraft} or {@code simpleend}. */
    public String namespace() {
        return this.namespace;
    }

    /** A wood of another mod (looked up by id, registered only with that mod). */
    public boolean external() {
        return !"minecraft".equals(this.namespace);
    }

    /**
     * The block whose properties a woodwork block copies: the wood's own log/stripped log/planks, for an End wood the
     * oak one (its own may not be registered yet). {@code part} is 0 log, 1 stripped log, 2 planks.
     */
    public Block template(int part) {
        if (!this.external()) {
            return part == 0 ? this.logBlock() : part == 1 ? this.strippedBlock() : this.planks();
        }
        return part == 0 ? Blocks.OAK_LOG : part == 1 ? Blocks.STRIPPED_OAK_LOG : Blocks.OAK_PLANKS;
    }

    /** The map colour of {@link #template}: the template's own, for an End wood its bark or wood colour. */
    public MapColor color(int part) {
        if (!this.external()) {
            return this.template(part).defaultMapColor();
        }
        return part == 0 ? this.barkColor : this.woodColor;
    }

    /** {@code oak}, {@code dark_oak}, {@code crimson}, {@code bamboo}. */
    public String id() {
        return this.id;
    }

    /** The Vanilla log id path: {@code oak_log}, {@code crimson_stem}, {@code bamboo_block}. */
    public String log() {
        return this.log;
    }

    /** The stripped log id path: {@code stripped_oak_log}. */
    public String strippedLog() {
        return "stripped_" + this.log;
    }

    public Block logBlock() {
        return this.logBlock.get();
    }

    public Block strippedBlock() {
        return this.strippedBlock.get();
    }

    public Block planks() {
        return this.planks.get();
    }

    /** Crimson and warped: no fire, no furnace fuel, a cauldron of it holds lava. */
    public boolean nether() {
        return this.nether;
    }
}
