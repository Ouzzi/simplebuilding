package com.simpleoctets.test;

import com.simplelib.api.IdMoves;
import com.simpleoctets.SimpleOctets;
import com.simpleoctets.block.MaterialOctetBlock;
import com.simpleoctets.block.MixedOctetBlock;
import com.simpleoctets.block.MixedOctetBlockEntity;
import com.simpleoctets.block.OctetCellBlock;
import com.simpleoctets.block.OctetCells;
import com.simpleoctets.item.MaterialOctetItem;
import com.simpleoctets.registry.OctetMaterials;
import com.simpleoctets.registry.OctetRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests (split stage 6); the Fabric/NeoForge/Forge adapters register them as
 * module_game_test_<name> on {@code simpleoctets:empty}. Every test runs standalone and with SimpleBuilding; octets of
 * another mod's blocks are only checked while that mod is loaded (said in the log).
 */
public final class OctetTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    public static final Map<String, Integer> MAX_TICKS = new LinkedHashMap<>();
    /** a-hang50: the reload test waits for a real unload and load of its chunk. */
    public static final int RELOAD_MAX_TICKS = 1600;
    static final BlockPos CELL = new BlockPos(2, 2, 2);

    static {
        ALL.put("registered", OctetTests::registered);
        ALL.put("stairs_and_slabs_have_octets", OctetTests::stairsAndSlabsHaveOctets);
        ALL.put("sub_mod_switch", OctetTests::subModSwitch);
        ALL.put("migration_from_simplebuilding", OctetTests::migrationFromSimplebuilding);
        ALL.put("foreign_materials_only_with_their_mod", OctetTests::foreignMaterialsOnlyWithTheirMod);
        ALL.put("blazewood_octets_only_with_simple_enchanting", OctetTests::blazewoodOctetsOnlyWithSimpleEnchanting);
        ALL.put("mixed_cell_keeps_readable_materials", OctetTests::mixedCellKeepsReadableMaterials);
        ALL.put("mixed_cell_loads_block_entity_data_into_a_placed_cell", OctetTests::mixedCellLoadsBlockEntityDataIntoAPlacedCell);
        ALL.put("recipes_cut_and_craft_back", OctetTests::recipesCutAndCraftBack);
        ALL.put("cells_for_carving_tools", OctetTests::cellsForCarvingTools);
        ALL.put("octets_place_into_the_sub_grid", OctetTests::octetsPlaceIntoTheSubGrid);
        ALL.put("octets_mix_materials_in_one_cell", OctetTests::octetsMixMaterialsInOneCell);
        ALL.put("sneaking_breaks_only_the_looked_at_octet", OctetTests::sneakingBreaksOnlyTheLookedAtOctet);
        ALL.put("eight_octets_of_one_material_become_the_full_block", OctetTests::eightOctetsOfOneMaterialBecomeTheFullBlock);
        ALL.put("sneaking_breaks_one_octet_off_full_blocks", OctetTests::sneakingBreaksOneOctetOffFullBlocks);
        ALL.put("corner_break_needs_the_tool_and_creative_drops_nothing", OctetTests::cornerBreakNeedsTheToolAndCreativeDropsNothing);
        ALL.put("corner_break_respects_protection_and_area_breaks", OctetTests::cornerBreakRespectsProtectionAndAreaBreaks);
        ALL.put("corner_break_only_with_hammer", OctetTests::cornerBreakOnlyWithHammer);
        ALL.put("mixed_cell_persists_and_drops_every_material", OctetTests::mixedCellPersistsAndDropsEveryMaterial);
        ALL.put("mixed_cell_reload_drops_ghost_bits_without_hang", OctetTests::mixedCellReloadDropsGhostBitsWithoutHang);
        MAX_TICKS.put("mixed_cell_reload_drops_ghost_bits_without_hang", RELOAD_MAX_TICKS);
        ALL.put("melon_slice_places_melon_octets", OctetTests::melonSlicePlacesMelonOctets);
        ALL.put("vanilla_input_switches", OctetTests::vanillaInputSwitches);
        ALL.put("octets_keep_their_blocks_rules", OctetTests::octetsKeepTheirBlocksRules);
        ALL.put("wood_and_wool_octets_burn", OctetTests::woodAndWoolOctetsBurn);
        ALL.put("log_octets_mine_with_the_axe", OctetTests::logOctetsMineWithTheAxe);
        ALL.put("unwaxed_copper_octets_age", OctetTests::unwaxedCopperOctetsAge);
        ALL.put("axe_strips_only_the_clicked_octet", OctetTests::axeStripsOnlyTheClickedOctet);
        ALL.put("axe_strips_stem_octets_not_stone", OctetTests::axeStripsStemOctetsNotStone);
        ALL.put("discover_recipe_unlocks", com.simpleoctets.discover.OctetsDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simpleoctets.discover.OctetsDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simpleoctets.discover.OctetsDiscover::firstPickupTips);
    }

    private OctetTests() {}

    // ===================================================================================== registration

    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int woods = (int) OctetRegistry.activeWoods().stream().filter(w -> BuiltInRegistries.BLOCK.containsKey(
                Identifier.fromNamespaceAndPath(OctetRegistry.woodNamespace(w), w + "_planks"))).count();
        if (OctetRegistry.WOOD_OCTETS.size() != woods || OctetRegistry.WOOD_OCTET_ITEMS.size() != woods) problems.add("wood octets " + OctetRegistry.WOOD_OCTETS.size() + "/" + woods);
        if (OctetRegistry.MATERIAL_OCTETS.size() != OctetRegistry.MATERIAL_OCTET_ITEMS.size()) problems.add("material items");
        if (OctetRegistry.MATERIAL_OCTETS.size() < 140) problems.add("only " + OctetRegistry.MATERIAL_OCTETS.size() + " material octets");
        for (String path : List.of("melon_octet", "mixed_octet")) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleOctets.id(path))) problems.add("block " + path);
            if (BuiltInRegistries.ITEM.containsKey(SimpleOctets.id(path))) problems.add(path + " must have no item");
        }
        if (!BuiltInRegistries.BLOCK_ENTITY_TYPE.containsKey(SimpleOctets.id("mixed_octet"))
                || !OctetRegistry.MIXED_OCTET_BE.isValid(OctetRegistry.MIXED_OCTET.defaultBlockState())) problems.add("mixed block entity");
        List<Item> items = new ArrayList<>(OctetRegistry.WOOD_OCTET_ITEMS);
        items.addAll(OctetRegistry.MATERIAL_OCTET_ITEMS);
        for (Item item : items) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!(item instanceof MaterialOctetItem octet) || octet.cell() != BuiltInRegistries.BLOCK.getValue(id)) problems.add("item " + id);
            if (new ItemStack(item).getMaxStackSize() != 99) problems.add("stack of " + id);
            if (!(BuiltInRegistries.BLOCK.getValue(id) instanceof MaterialOctetBlock cell) || cell.piece() != item) problems.add("piece of " + id);
        }
        for (String group : OctetMaterials.GROUPS) {
            if (!"mod".equals(group) && OctetRegistry.materialOctets(group).isEmpty()) problems.add("empty group " + group);
        }
        Item netherite = BuiltInRegistries.ITEM.getValue(SimpleOctets.id("netherite_block_octet"));
        if (!new ItemStack(netherite).has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT)) problems.add("netherite octet not fire resistant");
        h.assertTrue(problems.isEmpty(), "registration: " + problems);
        h.assertTrue(OctetRegistry.tabStacks().size() == items.size() + 1 + OctetRegistry.polishedEndStoneShapes().size(),
                "tab: every octet item, the melon slice and the polished end stone shapes");
        h.succeed();
    }

    /**
     * Octet rule (docs/ai/PRINZIPIEN.md A4/O1): reasoned exceptions of the coverage test as exact block ids (PLAN40 simpleoctets-2:
     * a substring let any future id containing "sand_slab" or "checker" slip through unseen).
     */
    static final Map<String, String> NO_OCTET = Map.of(
            "simpleblocks:dirt_slab", "natural slab (owner 17.10.: no octets for earth, grass, sand, gravel)",
            "simpleblocks:grass_slab", "natural slab (owner 17.10.)",
            "simpleblocks:sand_slab", "natural slab (owner 17.10.)",
            "simpleblocks:red_sand_slab", "natural slab: red sand is sand (owner 17.10., PLAN40 simpleblocks-3)",
            "simpleblocks:gravel_slab", "natural slab (owner 17.10.)",
            "minecraft:petrified_oak_slab", "no full block: it looks like oak planks, whose octet (oak_octet) exists");

    /** The chess checker shapes ({@code <x>_checker_stairs/_slab} of a {@code simplechess:<x>_checker}, Simple Chess since b-chess47) have the chess octet. */
    static boolean chessChecker(Identifier id, String stem) {
        return ("simplechess".equals(id.getNamespace()) || "simplebuilding".equals(id.getNamespace())) && stem.endsWith("_checker")
                && BuiltInRegistries.BLOCK.containsKey(Identifier.fromNamespaceAndPath(id.getNamespace(), stem));
    }

    /**
     * Every registered stair, slab and wall (any mod) has an octet of its full block or a reasoned exception: a future block
     * family fails here until it gets an entry in the octet generator (tools/textures/octets_2026_10_09.py). A shape counts by its
     * class (StairBlock/SlabBlock/WallBlock) or by its id ending ({@code _stairs/_slab/_wall}, own shape classes); a shape class
     * whose id lacks the ending is reported, so nothing slips past unchecked.
     */
    static void stairsAndSlabsHaveOctets(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String path = id.getPath();
            // N86: half slabs of Simple Slabs (no SlabBlock) end in "_slab" too - they are pieces like the octets, not a shape
            // that needs an octet of "<x>_half"; their own coverage is SlabTests (every slab has a half slab).
            if (path.endsWith("_half_slab")) continue;
            boolean stairsClass = block instanceof net.minecraft.world.level.block.StairBlock;
            boolean slabClass = block instanceof net.minecraft.world.level.block.SlabBlock;
            boolean wallClass = block instanceof net.minecraft.world.level.block.WallBlock;
            String suffix = path.endsWith("_stairs") ? "_stairs" : path.endsWith("_slab") ? "_slab" : path.endsWith("_wall") ? "_wall" : null;
            if (suffix == null) {
                if (stairsClass || slabClass || wallClass) problems.add(id + " (shape class without _stairs/_slab/_wall)");
                continue;
            }
            if (path.equals(suffix.substring(1))) continue;
            if (NO_OCTET.containsKey(id.toString())) continue;
            String stem = path.substring(0, path.length() - suffix.length());
            if (chessChecker(id, stem)) continue;
            List<String> bases = List.of(stem, stem + "_planks", stem + "_block", stem + "s");
            checked++;
            boolean found = false;
            for (OctetMaterials.Material m : OctetMaterials.ALL) {
                // Simple Blocks cuts stairs/slabs for Vanilla blocks (calcite, cut sandstone ...): their octets live under minecraft.
                boolean sameBlock = m.namespace().equals(id.getNamespace())
                        || ("simpleblocks".equals(id.getNamespace()) && "minecraft".equals(m.namespace()));
                if (sameBlock && bases.contains(m.base())
                        && BuiltInRegistries.BLOCK.containsKey(SimpleOctets.id(m.key() + "_octet"))) found = true;
            }
            for (String wood : OctetRegistry.activeWoods()) {
                if (id.getNamespace().equals(OctetRegistry.woodNamespace(wood)) && bases.contains(wood + "_planks")
                        && BuiltInRegistries.BLOCK.containsKey(SimpleOctets.id(wood + "_octet"))) found = true;
            }
            if (!found) problems.add(id.toString());
        }
        h.assertTrue(checked > 150, "only " + checked + " stairs/slabs/walls checked");
        h.assertTrue(problems.isEmpty(), "stairs/slabs/walls without an octet or a reasoned exception: " + problems);
        for (String path : List.of("polished_end_stone", "cut_sandstone", "cut_red_sandstone", "smooth_stone")) {
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleOctets.id(path + "_octet")), "octet of " + path);
        }
        h.succeed();
    }

    static void subModSwitch(GameTestHelper h) {
        com.simplebuilding.framework.api.SubMods.set(SimpleOctets.MOD_ID, false);
        try {
            h.assertFalse(SimpleOctets.on(), "switched off");
            h.assertTrue(OctetRegistry.tabStacks().isEmpty(), "no tab stacks while off");
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(SimpleOctets.id("oak_octet")), "registrations stay");
            h.setBlock(CELL.below(), Blocks.STONE);
            ServerPlayer player = mockPlayer(h);
            player.setShiftKeyDown(true);
            h.assertTrue(com.simpleoctets.MelonSlices.tryPlace(topClick(h, player, new ItemStack(Items.MELON_SLICE), 0, 0)) == null,
                    "no melon octet while off");
        } finally {
            com.simplebuilding.framework.api.SubMods.set(SimpleOctets.MOD_ID, true);
        }
        h.assertTrue(!OctetRegistry.tabStacks().isEmpty(), "tab back on");
        h.succeed();
    }

    /** Old {@code simplebuilding:} ids (blocks in chunks, items, the mixed cell's block entity, recipe book, advancements). */
    static void migrationFromSimplebuilding(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Block cell : OctetRegistry.cells()) {
            String path = BuiltInRegistries.BLOCK.getKey(cell).getPath();
            if (BuiltInRegistries.BLOCK.getValue(SimpleOctets.sb(path)) != cell) problems.add("block " + path);
            Item item = BuiltInRegistries.ITEM.getValue(SimpleOctets.id(path));
            if (item != Items.AIR && BuiltInRegistries.ITEM.getValue(SimpleOctets.sb(path)) != item) problems.add("item " + path);
        }
        if (BuiltInRegistries.BLOCK.getValue(SimpleOctets.sb("mixed_octet")) != OctetRegistry.MIXED_OCTET) problems.add("mixed block");
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleOctets.sb("mixed_octet")) != OctetRegistry.MIXED_OCTET_BE) problems.add("mixed block entity");
        for (String path : SimpleOctets.recipePaths()) {
            if (!SimpleOctets.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleOctets.sb(path)))) problems.add("recipe " + path);
            Identifier advancement = SimpleOctets.id("recipes/building_blocks/" + path);
            if (!advancement.equals(IdMoves.advancement(SimpleOctets.sb("recipes/building_blocks/" + path)))) problems.add("advancement " + path);
        }
        // A mixed cell of an old world: the saved block entity with every simpleoctets: id written as simplebuilding: (as
        // SimpleBuilding saved it before the move) loads with its materials.
        BlockState[] parts = new BlockState[8];
        parts[0] = material(Blocks.OAK_PLANKS);
        parts[1] = material(Blocks.STONE);
        OctetCells.set(h.getLevel(), h.absolutePos(CELL), parts, false, Block.UPDATE_ALL);
        BlockState mixed = h.getBlockState(CELL);
        CompoundTag saved = h.getLevel().getBlockEntity(h.absolutePos(CELL)).saveWithFullMetadata(h.getLevel().registryAccess());
        CompoundTag tag;
        try {
            tag = net.minecraft.nbt.TagParser.parseCompoundFully(saved.toString().replace(SimpleOctets.MOD_ID + ":", SimpleOctets.SB_NAMESPACE + ":"));
        } catch (Exception e) {
            throw h.assertionException("old tag: " + e);
        }
        if (!tag.toString().contains("simplebuilding:oak_octet")) problems.add("old tag without old ids: " + tag);
        BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(CELL), mixed, tag, h.getLevel().registryAccess());
        if (!(loaded instanceof MixedOctetBlockEntity be) || be.part(0) != parts[0] || be.part(1) != parts[1]) {
            problems.add("old mixed cell loads as " + loaded + (loaded instanceof MixedOctetBlockEntity be ? java.util.Arrays.toString(be.parts()) : "") + " from " + tag);
        }
        h.assertTrue(problems.isEmpty(), "migration: " + problems);
        h.succeed();
    }

    /**
     * a-hang50 (owner 14.11.: the world hung after he came back, closing hung, reopening took ages; his hang dumps): a mixed
     * cell with a ghost bit (a state bit without material) is saved with its chunk, the chunk really unloads (its block entity
     * is removed) and loads again from disk. The load must not stall the server - the old {@code setLevel} ran its ghost check
     * inline inside the chunk's FULL step and waited for that very chunk forever - and the ghost bit is gone afterwards. A hung
     * server thread can never fail a test, so {@link StallWatch} ends the run with a thread dump after 60 s without a tick.
     */
    static void mixedCellReloadDropsGhostBitsWithoutHang(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(-1250, 1250); // far from every test structure
        BlockPos pos = new BlockPos(chunk.getMinBlockX() + 8, 100, chunk.getMinBlockZ() + 8);
        BlockState oak = material(Blocks.OAK_PLANKS), stone = material(Blocks.STONE);
        StallWatch watch = StallWatch.start(level.getServer(), "mixed_cell_reload_drops_ghost_bits_without_hang");
        int[] step = {0}, waited = {0};
        BlockEntity[] before = {null};
        level.setChunkForced(chunk.x(), chunk.z(), true);
        h.onEachTick(() -> {
            try {
                waited[0]++;
                boolean full = level.getChunkSource().getChunkNow(chunk.x(), chunk.z()) != null;
                switch (step[0]) {
                    case 0 -> { // loaded: a mixed cell of oak and stone
                        if (!full) {
                            h.assertTrue(waited[0] < 400, "the far chunk never loaded");
                            return;
                        }
                        BlockState[] parts = new BlockState[8];
                        parts[0] = oak;
                        parts[1] = stone;
                        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
                        h.assertTrue(level.getBlockState(pos).getBlock() instanceof MixedOctetBlock && level.getBlockEntity(pos) instanceof MixedOctetBlockEntity,
                                "mixed cell placed: " + level.getBlockState(pos));
                        step[0] = 1;
                        waited[0] = 0;
                    }
                    case 1 -> { // after its own queued check ran: a third bit without material (the ghost), then let the chunk go
                        if (waited[0] < 6) return;
                        BlockState mixed = level.getBlockState(pos);
                        level.setBlock(pos, mixed.setValue(OctetCellBlock.OCTETS[2], true), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        before[0] = level.getBlockEntity(pos);
                        h.assertTrue(OctetCellBlock.mask(level.getBlockState(pos)) == 0b111 && before[0] instanceof MixedOctetBlockEntity m
                                && m.part(2) == null, "ghost bit set: " + level.getBlockState(pos));
                        level.setChunkForced(chunk.x(), chunk.z(), false);
                        step[0] = 2;
                        waited[0] = 0;
                    }
                    case 2 -> { // saved and really unloaded: the chunk took its block entity with it
                        if (!before[0].isRemoved()) {
                            h.assertTrue(waited[0] < 800, "the chunk never unloaded");
                            return;
                        }
                        level.setChunkForced(chunk.x(), chunk.z(), true);
                        step[0] = 3;
                        waited[0] = 0;
                    }
                    case 3 -> { // loaded from disk again (this is where the old code hung)
                        if (!full) {
                            h.assertTrue(waited[0] < 400, "the chunk never loaded again");
                            return;
                        }
                        step[0] = 4;
                        waited[0] = 0;
                    }
                    case 4 -> { // the queued check had its ticks: the ghost bit is gone, both materials stay
                        if (waited[0] < 6) return;
                        BlockState now = level.getBlockState(pos);
                        BlockEntity be = level.getBlockEntity(pos);
                        String seen = now + " " + (be instanceof MixedOctetBlockEntity m ? java.util.Arrays.toString(m.parts()) : String.valueOf(be));
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                        level.setChunkForced(chunk.x(), chunk.z(), false);
                        watch.stop();
                        step[0] = 5;
                        h.assertTrue(be != before[0], "loaded from disk, not kept in memory: " + seen);
                        h.assertTrue(OctetCellBlock.mask(now) == 0b011 && be instanceof MixedOctetBlockEntity m && m.part(0) == oak && m.part(1) == stone,
                                "ghost bit dropped after the load, materials kept: " + seen);
                        h.succeed();
                    }
                    default -> { }
                }
            } catch (RuntimeException e) {
                watch.stop();
                level.setChunkForced(chunk.x(), chunk.z(), false);
                throw e;
            }
        });
    }

    /**
     * a-hang50: ends the run with a dump of the server thread when the server tick stands for {@link #STALL_SECONDS} while a
     * test watches it (a hang inside a chunk load never comes back to the test); gone after {@link #stop} or ten minutes.
     */
    static final class StallWatch {
        static final int STALL_SECONDS = 60;
        private volatile boolean done;

        static StallWatch start(net.minecraft.server.MinecraftServer server, String test) {
            StallWatch w = new StallWatch();
            Thread t = new Thread(() -> {
                long started = System.nanoTime(), since = started;
                int last = server.getTickCount();
                while (!w.done && System.nanoTime() - started < 600_000_000_000L) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException e) {
                        return;
                    }
                    int now = server.getTickCount();
                    if (now != last) {
                        last = now;
                        since = System.nanoTime();
                    } else if (!w.done && System.nanoTime() - since > STALL_SECONDS * 1_000_000_000L) {
                        StringBuilder out = new StringBuilder("[simpleoctets] " + test + ": the server thread stands for " + STALL_SECONDS
                                + " s - ending the run\n");
                        Thread.getAllStackTraces().forEach((thread, stack) -> {
                            if (!"Server thread".equals(thread.getName())) return;
                            for (StackTraceElement frame : stack) out.append("    at ").append(frame).append('\n');
                        });
                        com.mojang.logging.LogUtils.getLogger().error(out.toString());
                        System.out.println(out);
                        System.out.flush();
                        Runtime.getRuntime().halt(3);
                    }
                }
            }, "simpleoctets-stall-watch");
            t.setDaemon(true);
            t.start();
            return w;
        }

        void stop() {
            done = true;
        }
    }

    /**
     * REV2-B: a mixed cell whose saved palette names a block of a removed mod keeps every other material (before, the strict
     * list codec dropped the whole palette and left an invisible cell without drops).
     */
    /**
     * O8 audit 2: block entity data loaded into an already placed cell (what /setblock with NBT, structure templates and
     * build tools do: place the state, then {@code loadWithComponents} on the new block entity) keeps its materials, also
     * after the queued ghost-bit check of {@code setLevel} ran. On a dedicated server a cell set by
     * {@code setblock ... simpleoctets:mixed_octet[...]{Palette:[...],Octets:[I;...]}} read back {@code Palette: []}.
     */
    static void mixedCellLoadsBlockEntityDataIntoAPlacedCell(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockState oak = material(Blocks.OAK_PLANKS), stone = material(Blocks.STONE);
        BlockState[] parts = new BlockState[8];
        parts[0] = oak;
        parts[1] = stone;
        BlockPos source = h.absolutePos(CELL), copy = h.absolutePos(CELL.east(2)), command = h.absolutePos(CELL.east(4));
        OctetCells.set(level, source, parts, false, Block.UPDATE_ALL);
        BlockState mixed = level.getBlockState(source);
        CompoundTag saved = level.getBlockEntity(source).saveWithoutMetadata(level.registryAccess());
        // structure-template path: place the state, then load the data into the block entity the placement made
        level.setBlock(copy, mixed, Block.UPDATE_CLIENTS);
        level.getBlockEntity(copy).loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), saved));
        String copied = describe(level, copy);
        h.assertTrue(level.getBlockEntity(copy) instanceof MixedOctetBlockEntity be && be.part(0) == oak && be.part(1) == stone,
                "data loaded into a placed cell keeps both materials: " + copied + " from " + saved);
        // the command itself
        String cmd = "setblock " + command.getX() + " " + command.getY() + " " + command.getZ() + " "
                + net.minecraft.commands.arguments.blocks.BlockStateParser.serialize(mixed) + saved;
        level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack().withLevel(level).withSuppressedOutput(), cmd);
        String commanded = describe(level, command);
        h.assertTrue(level.getBlockEntity(command) instanceof MixedOctetBlockEntity be && be.part(0) == oak && be.part(1) == stone,
                "/setblock with the saved data keeps both materials: " + commanded + " after " + cmd);
        // the queued ghost-bit check (setLevel) must not empty either cell afterwards
        h.runAfterDelay(5, () -> {
            for (BlockPos at : new BlockPos[] {copy, command}) {
                h.assertTrue(level.getBlockEntity(at) instanceof MixedOctetBlockEntity be && be.part(0) == oak && be.part(1) == stone
                        && OctetCellBlock.mask(level.getBlockState(at)) == 0b11, "still both materials after the queued check: " + describe(level, at));
            }
            h.succeed();
        });
    }

    private static String describe(ServerLevel level, BlockPos at) {
        return level.getBlockState(at) + " " + (level.getBlockEntity(at) instanceof MixedOctetBlockEntity be
                ? java.util.Arrays.toString(be.parts()) : String.valueOf(level.getBlockEntity(at)));
    }

    static void mixedCellKeepsReadableMaterials(GameTestHelper h) {
        BlockState[] parts = new BlockState[8];
        parts[0] = material(Blocks.OAK_PLANKS);
        parts[1] = material(Blocks.STONE);
        OctetCells.set(h.getLevel(), h.absolutePos(CELL), parts, false, Block.UPDATE_ALL);
        BlockState mixed = h.getBlockState(CELL);
        CompoundTag saved = h.getLevel().getBlockEntity(h.absolutePos(CELL)).saveWithFullMetadata(h.getLevel().registryAccess());
        String gone = BuiltInRegistries.BLOCK.getKey(parts[1].getBlock()).toString();
        CompoundTag tag;
        try {
            tag = net.minecraft.nbt.TagParser.parseCompoundFully(saved.toString().replace(gone, "removedmod:gone_octet"));
        } catch (Exception e) {
            throw h.assertionException("tag: " + e);
        }
        h.assertTrue(tag.toString().contains("removedmod:gone_octet"), "palette names the removed block: " + tag);
        BlockEntity loaded = BlockEntity.loadStatic(h.absolutePos(CELL), mixed, tag, h.getLevel().registryAccess());
        h.assertTrue(loaded instanceof MixedOctetBlockEntity be && be.part(0) == parts[0] && be.part(1) == null,
                "the readable material stays: " + (loaded instanceof MixedOctetBlockEntity be ? java.util.Arrays.toString(be.parts()) : String.valueOf(loaded)));
        h.succeed();
    }

    /** SimpleBuilding's palettes and Simple End's woods have octets exactly while that mod is loaded. */
    static void foreignMaterialsOnlyWithTheirMod(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (OctetMaterials.Material material : OctetMaterials.ALL) {
            if ("minecraft".equals(material.namespace())) continue;
            boolean loaded = SimpleOctets.isLoaded(material.namespace());
            Identifier id = SimpleOctets.id(material.key() + "_octet");
            if (BuiltInRegistries.BLOCK.containsKey(id) != loaded) problems.add(id + " registered=" + !loaded + " with " + material.namespace() + " loaded=" + loaded);
            if (loaded) {
                Block source = ((MaterialOctetBlock) BuiltInRegistries.BLOCK.getValue(id)).source();
                if (source != BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(material.namespace(), material.base()))) problems.add("source of " + id);
            }
            var recipe = h.getLevel().getServer().getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, SimpleOctets.id(material.base() + "_from_octets")));
            if (recipe.isPresent() != loaded) problems.add("recipe of " + id + " present=" + recipe.isPresent());
        }
        if (!SimpleOctets.isLoaded(SimpleOctets.SB_NAMESPACE)) com.mojang.logging.LogUtils.getLogger().info("[standalone] simplebuilding not loaded - its palettes have no octets");
        h.assertTrue(problems.isEmpty(), "foreign materials: " + problems);
        h.succeed();
    }

    /**
     * PLAN40 simpleenchanting-1: the Crimson/Warped Blazewood octets (cells, items, crafting back) exist exactly while Simple
     * Enchanting is loaded; their source is the blazewood planks of the installation (SimpleBuilding's with it, the
     * {@code simpleenchanting:} id resolves there), they do not burn, and 8 octets craft back into those planks.
     */
    static void blazewoodOctetsOnlyWithSimpleEnchanting(GameTestHelper h) {
        boolean loaded = SimpleOctets.isLoaded(OctetRegistry.SIMPLE_ENCHANTING);
        List<String> problems = new ArrayList<>();
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (String wood : List.of("crimson_blazewood", "warped_blazewood")) {
            Identifier id = SimpleOctets.id(wood + "_octet");
            if (BuiltInRegistries.BLOCK.containsKey(id) != loaded || BuiltInRegistries.ITEM.containsKey(id) != loaded) problems.add(id + " registered with Simple Enchanting loaded=" + loaded);
            if (OctetRegistry.activeWoods().contains(wood) != loaded) problems.add(wood + " active");
            var back = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleOctets.id(wood + "_planks_from_octets")));
            if (back.isPresent() != loaded) problems.add("crafting back of " + wood + " present=" + back.isPresent());
            if (!loaded) continue;
            Block cell = BuiltInRegistries.BLOCK.getValue(id);
            Block planks = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath(OctetRegistry.SIMPLE_ENCHANTING, wood + "_planks"));
            if (planks == Blocks.AIR || !(cell instanceof MaterialOctetBlock octet) || octet.source() != planks) problems.add("source of " + id);
            if (com.simplelib.api.shape.ShapeMaterials.igniteOdds(cell.defaultBlockState()) > 0) problems.add(id + " burns (Nether wood)");
            if (back.isPresent() && back.get().value() instanceof net.minecraft.world.item.crafting.CraftingRecipe recipe) {
                List<ItemStack> eight = new ArrayList<>();
                for (int i = 0; i < 8; i++) eight.add(new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                eight.add(ItemStack.EMPTY);
                var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, eight);
                ItemStack out = recipe.matches(input, h.getLevel()) ? recipe.assemble(input) : ItemStack.EMPTY;
                if (!out.is(planks.asItem())) problems.add("8 " + id + " craft " + out);
            }
        }
        if (!loaded) com.mojang.logging.LogUtils.getLogger().info("[standalone] simpleenchanting not loaded - no blazewood octets");
        h.assertTrue(problems.isEmpty(), "blazewood octets: " + problems);
        h.succeed();
    }

    /**
     * Every cell: 8 are cut from the full block, 8 craft back. Octets are exclusive to the Shapecutter (docs/ai/PRINZIPIEN.md
     * A4b): with Simple Blocks only its {@code simpleblocks:sawing} recipe exists and no stonecutter recipe yields an octet
     * (the Vanilla stonecutter stays Vanilla); without it the {@code minecraft:stonecutting} fallback keeps the module playable.
     */
    static void recipesCutAndCraftBack(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        boolean saw = SimpleOctets.isLoaded("simpleblocks");
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (Block cell : OctetRegistry.cells()) {
            if (cell == OctetRegistry.MELON_OCTET) continue;
            Block full = ((MaterialOctetBlock) cell).source();
            String base = BuiltInRegistries.BLOCK.getKey(full).getPath();
            String key = BuiltInRegistries.BLOCK.getKey(cell).getPath();
            var sawing = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleOctets.id(key + "_from_" + base + "_sawing")));
            var cut = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleOctets.id(key + "_from_" + base + "_stonecutting")));
            var back = recipes.byKey(ResourceKey.create(Registries.RECIPE, SimpleOctets.id(base + "_from_octets")));
            if (sawing.isPresent() != saw) problems.add("Shapecutter recipe of " + key + " present=" + sawing.isPresent());
            if (sawing.isPresent() && !"simpleblocks:sawing".equals(String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(sawing.get().value().getType())))) {
                problems.add("type of " + key + "_from_" + base + "_sawing");
            }
            if (cut.isPresent() == saw) problems.add("stonecutter recipe of " + key + " present=" + cut.isPresent());
            if (back.isEmpty()) problems.add("no way back to " + base);
        }
        for (var holder : recipes.getRecipes()) {
            if (holder.value().getType() != net.minecraft.world.item.crafting.RecipeType.STONECUTTING
                    || !(holder.value() instanceof net.minecraft.world.item.crafting.SingleItemRecipe recipe)) continue;
            var first = recipe.input().items().findFirst();
            if (first.isEmpty()) continue;
            ItemStack out = recipe.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(first.get().value())));
            if (saw && SimpleOctets.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(out.getItem()).getNamespace())) {
                problems.add("stonecutter yields an octet with Simple Blocks: " + holder.id().identifier());
            }
        }
        h.assertTrue(problems.isEmpty(), "recipes (Simple Blocks " + (saw ? "loaded" : "absent") + "): " + problems);
        h.succeed();
    }

    /** SimpleLib's OctetCells knows every cell (the sledgehammer of Simple Hammers carves through it). */
    static void cellsForCarvingTools(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Block cell : OctetRegistry.cells()) {
            Block base = ((MaterialOctetBlock) cell).source();
            if (com.simplelib.api.OctetCells.cellFor(base) != cell) problems.add("cell of " + BuiltInRegistries.BLOCK.getKey(base));
            if (com.simplelib.api.OctetCells.pieceOf(cell) != ((MaterialOctetBlock) cell).piece()) problems.add("piece of " + cell);
        }
        h.assertTrue(com.simplelib.api.OctetCells.cellFor(Blocks.MELON) == OctetRegistry.MELON_OCTET
                && com.simplelib.api.OctetCells.pieceOf(OctetRegistry.MELON_OCTET) == Items.MELON_SLICE, "melon: cell and slice");
        h.assertTrue(problems.isEmpty(), "OctetCells: " + problems);
        h.succeed();
    }

    // ===================================================================================== behaviour

    static void octetsPlaceIntoTheSubGrid(GameTestHelper h) {
        h.setBlock(CELL.below(), Blocks.STONE);
        ServerPlayer player = mockPlayer(h);
        Item oak = piece(Blocks.OAK_PLANKS);
        ItemStack stack = new ItemStack(oak, 4);
        h.assertValueEqual(oak.useOn(topClick(h, player, stack, 0, 0)), InteractionResult.SUCCESS, "placing an oak octet");
        BlockState placed = h.getBlockState(CELL);
        h.assertTrue(placed.is(cell(Blocks.OAK_PLANKS)) && OctetCellBlock.mask(placed) == bit(0, 0, 0), "oak octet not in the clicked corner: " + placed);
        h.assertValueEqual(stack.getCount(), 3, "octet consumed");
        oak.useOn(topClick(h, player, stack, 1, 0));
        h.assertValueEqual(OctetCellBlock.count(h.getBlockState(CELL)), 2, "second octet joins the cell");
        h.assertTrue(OctetCells.placed(h.getLevel(), OctetCells.material(cell(Blocks.STONE).defaultBlockState()), h.absolutePos(CELL),
                OctetCellBlock.index(0, 0, 0), null) == null, "an occupied octet took a second one");
        // a full cell is a full cube and holds no water
        BlockState full = OctetCellBlock.withMask(cell(Blocks.OAK_PLANKS).defaultBlockState(), OctetCellBlock.FULL);
        h.assertTrue(full.isCollisionShapeFullBlock(h.getLevel(), h.absolutePos(CELL)), "a full cell is a full cube");
        h.assertFalse(full.getValue(OctetCellBlock.WATERLOGGED), "a full cell holds no water");
        h.succeed();
    }

    /** Octet items of two materials (oak, stone) fill one cell; taking one out turns it pure again. */
    static void octetsMixMaterialsInOneCell(GameTestHelper h) {
        h.setBlock(CELL.below(), Blocks.STONE);
        ServerPlayer player = mockPlayer(h);
        BlockPos pos = h.absolutePos(CELL);
        piece(Blocks.OAK_PLANKS).useOn(topClick(h, player, new ItemStack(piece(Blocks.OAK_PLANKS), 4), 0, 0));
        h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "a pure cell has no block entity");
        piece(Blocks.STONE).useOn(topClick(h, player, new ItemStack(piece(Blocks.STONE), 4), 1, 0));
        BlockState mixed = h.getBlockState(CELL);
        h.assertTrue(mixed.is(OctetRegistry.MIXED_OCTET), "two materials make a mixed cell: " + mixed);
        BlockState[] parts = OctetCells.parts(h.getLevel(), pos, mixed);
        h.assertTrue(parts[OctetCellBlock.index(0, 0, 0)] == material(Blocks.OAK_PLANKS)
                && parts[OctetCellBlock.index(1, 0, 0)] == material(Blocks.STONE), "materials per octet: " + java.util.Arrays.toString(parts));
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + 0.75, pos.getY() + 0.5, pos.getZ() + 0.25), Direction.UP, pos, false);
        h.getBlockState(CELL).useWithoutItem(h.getLevel(), player, hit);
        h.assertTrue(player.getMainHandItem().is(piece(Blocks.STONE)), "the stone octet came into the hand: " + player.getMainHandItem());
        BlockState pure = h.getBlockState(CELL);
        h.assertTrue(pure.is(cell(Blocks.OAK_PLANKS)) && OctetCellBlock.mask(pure) == bit(0, 0, 0), "one material left is a pure cell again: " + pure);
        h.assertTrue(h.getLevel().getBlockEntity(pos) == null, "the pure cell dropped its block entity");
        h.succeed();
    }

    /** A sneaking survival player breaks only the octet it looks at; without sneaking the whole cell goes. */
    static void sneakingBreaksOnlyTheLookedAtOctet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            parts[i] = OctetCellBlock.y(i) == 0 ? material(Blocks.STONE) : material(Blocks.OAK_PLANKS);
        }
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        ServerPlayer player = survivalPlayer(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.setShiftKeyDown(true);
        for (int x = 0; x < 2; x++) {
            for (int z = 0; z < 2; z++) {
                Vec3 top = new Vec3(pos.getX() + x * 0.5 + 0.25, pos.getY() + 1.0, pos.getZ() + z * 0.5 + 0.25);
                player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
                player.gameMode.destroyBlock(pos);
            }
        }
        BlockState rest = h.getBlockState(CELL);
        h.assertTrue(rest.is(cell(Blocks.STONE)) && OctetCellBlock.mask(rest) == OctetCellBlock.BOTTOM_HALF, "four stone octets are left: " + rest);
        h.assertValueEqual(dropped(h, piece(Blocks.OAK_PLANKS)), 4, "one oak octet per break");
        player.setShiftKeyDown(false);
        player.gameMode.destroyBlock(pos);
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, piece(Blocks.STONE)), 4, "the whole rest of the cell");
        h.succeed();
    }

    /**
     * Owner SP22-1: eight octets of one material become its full block when the eighth is placed (no loss: 1 block = 8
     * octets), water goes; the melon (slices are no octets) and mixed cells stay cells. Every material octet maps to its
     * full block and back.
     */
    static void eightOctetsOfOneMaterialBecomeTheFullBlock(GameTestHelper h) {
        h.setBlock(CELL.below(), Blocks.STONE);
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        for (int i = 1; i < 8; i++) {
            parts[i] = material(Blocks.DIAMOND_BLOCK);
        }
        OctetCells.set(level, pos, parts, true, Block.UPDATE_ALL);
        BlockState seven = h.getBlockState(CELL);
        h.assertTrue(seven.is(cell(Blocks.DIAMOND_BLOCK)) && OctetCellBlock.count(seven) == 7 && OctetCells.wet(seven), "seven wet diamond octets: " + seven);
        ServerPlayer player = mockPlayer(h);
        ItemStack stack = new ItemStack(piece(Blocks.DIAMOND_BLOCK), 2);
        h.assertValueEqual(stack.getItem().useOn(topClick(h, player, stack, 0, 0)), InteractionResult.SUCCESS, "placing the eighth diamond octet");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK, CELL);
        h.assertTrue(level.getFluidState(pos).isEmpty(), "the full block holds no water");
        h.assertValueEqual(stack.getCount(), 1, "one octet used");
        // melon and mixed cells stay cells
        java.util.Arrays.fill(parts, OctetCells.material(OctetRegistry.MELON_OCTET.defaultBlockState()));
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        h.assertTrue(h.getBlockState(CELL).is(OctetRegistry.MELON_OCTET) && OctetCellBlock.count(h.getBlockState(CELL)) == 8, "eight melon octets stay a cell");
        java.util.Arrays.fill(parts, material(Blocks.DIAMOND_BLOCK));
        parts[3] = material(Blocks.GOLD_BLOCK);
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        h.assertTrue(h.getBlockState(CELL).is(OctetRegistry.MIXED_OCTET), "a full mixed cell stays mixed");
        java.util.Arrays.fill(parts, material(Blocks.OAK_PLANKS));
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        h.assertBlockPresent(Blocks.OAK_PLANKS, CELL);
        // every material with an octet item: octets -> its full block -> back to the same cell
        List<String> problems = new ArrayList<>();
        for (Block cell : OctetRegistry.cells()) {
            MaterialOctetBlock octet = (MaterialOctetBlock) cell;
            BlockState full = OctetCells.fullBlock(OctetCells.material(cell.defaultBlockState()));
            boolean item = octet.piece() instanceof MaterialOctetItem;
            if (!item) {
                if (full != null) problems.add(cell + " (no octet item) merges into " + full);
                continue;
            }
            if (full == null || full.getBlock() != octet.source()) problems.add(cell + " -> " + full);
            else if (OctetCells.cornerCell(full.getBlock()) != cell) problems.add(full + " breaks into " + OctetCells.cornerCell(full.getBlock()));
        }
        h.assertTrue(problems.isEmpty(), "octets <-> full block: " + problems);
        h.succeed();
    }

    /** Looks straight down from above at the upper octet (x, 1, z) of {@link #CELL}. */
    static void aimFromAbove(GameTestHelper h, ServerPlayer player, int x, int z) {
        BlockPos pos = h.absolutePos(CELL);
        Vec3 top = new Vec3(pos.getX() + x * 0.5 + 0.25, pos.getY() + 1.0, pos.getZ() + z * 0.5 + 0.25);
        player.snapTo(top.x, top.y + 1.0 - player.getEyeHeight(), top.z, 0.0F, 90.0F);
    }

    static void clearDrops(GameTestHelper h) {
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CELL)).inflate(3.0)).forEach(ItemEntity::discard);
    }

    /**
     * B23-M8: the corner break needs a hammer ({@code #simplelib:hammers}); Simple Octets does not know a hammer mod, so the
     * tests take Simple Hammers' sledgehammer of {@code tier} by id while it is loaded, else {@code null} (standalone: the
     * hammer part of a test is skipped, said in the log).
     */
    static ItemStack hammer(String tier) {
        ItemStack stack = BuiltInRegistries.ITEM.getOptional(Identifier.fromNamespaceAndPath("simplehammers", tier + "_sledgehammer"))
                .map(ItemStack::new).orElse(ItemStack.EMPTY);
        if (stack.isEmpty() || !com.simplelib.api.SimpleLibApi.isHammer(stack)) {
            SimpleOctets.LOG.info("[octets test] no hammer loaded ({} sledgehammer): corner break with a hammer skipped", tier);
            return null;
        }
        return stack;
    }

    /**
     * B23-M8 (owner 23.10., correcting SP22-1): sneaking with a pickaxe (or any non-hammer) mines the whole full block like
     * Vanilla - only a hammer breaks a corner off. Runs standalone too.
     */
    static void cornerBreakOnlyWithHammer(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CELL);
        ServerPlayer player = survivalPlayer(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        player.setShiftKeyDown(true);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        aimFromAbove(h, player, 1, 1);
        h.assertTrue(player.gameMode.destroyBlock(pos), "the break was refused");
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, Items.DIAMOND_BLOCK), 1, "sneaking with a pickaxe drops the whole block");
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "and no octet");
        clearDrops(h);
        ItemStack hammer = hammer("diamond");
        if (hammer != null) {
            player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
            h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
            aimFromAbove(h, player, 1, 1);
            player.gameMode.destroyBlock(pos);
            h.assertTrue(OctetCellBlock.count(h.getBlockState(CELL)) == 7, "sneaking with a hammer leaves seven octets: " + h.getBlockState(CELL));
            h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 1, "and one octet drops");
        }
        player.setShiftKeyDown(false);
        h.succeed();
    }

    /**
     * Owner SP22-1: sneaking with a hammer (B23-M8), a player breaks the looked-at corner off a full block: a cell of 7
     * octets stays, exactly one octet drops - what the block would drop, as an octet (stone -> a cobblestone octet).
     * Without sneaking: Vanilla.
     */
    static void sneakingBreaksOneOctetOffFullBlocks(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CELL);
        ServerPlayer player = survivalPlayer(h);
        ItemStack hammer = hammer("diamond");
        if (hammer == null) {
            h.succeed();
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setShiftKeyDown(true);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        aimFromAbove(h, player, 1, 1);
        h.assertTrue(player.gameMode.destroyBlock(pos), "the corner break was refused");
        BlockState rest = h.getBlockState(CELL);
        h.assertTrue(rest.is(cell(Blocks.DIAMOND_BLOCK)) && OctetCellBlock.mask(rest) == (OctetCellBlock.FULL & ~bit(1, 1, 1)),
                "seven diamond octets without the looked-at one: " + rest);
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 1, "exactly one diamond octet drops");
        h.assertValueEqual(dropped(h, Items.DIAMOND_BLOCK), 0, "no diamond block drops");
        // the next sneaking break takes the next octet of the cell
        aimFromAbove(h, player, 0, 1);
        player.gameMode.destroyBlock(pos);
        h.assertValueEqual(OctetCellBlock.count(h.getBlockState(CELL)), 6, "the cell breaks on octet by octet");
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 2, "two diamond octets");
        clearDrops(h);
        // stone drops cobblestone: its corner drops a cobblestone octet, the rest stays stone
        h.setBlock(CELL, Blocks.STONE);
        aimFromAbove(h, player, 0, 0);
        player.gameMode.destroyBlock(pos);
        h.assertTrue(h.getBlockState(CELL).is(cell(Blocks.STONE)) && OctetCellBlock.count(h.getBlockState(CELL)) == 7, "seven stone octets");
        h.assertValueEqual(dropped(h, piece(Blocks.COBBLESTONE)), 1, "a stone corner drops a cobblestone octet");
        h.assertValueEqual(dropped(h, piece(Blocks.STONE)), 0, "no stone octet without silk touch");
        clearDrops(h);
        // without sneaking: the whole block, Vanilla
        player.setShiftKeyDown(false);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        player.gameMode.destroyBlock(pos);
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, Items.DIAMOND_BLOCK), 1, "without sneaking the block drops whole");
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "and no octet");
        h.succeed();
    }

    /**
     * Owner SP22-1: tool tier and creative mode. A stone hammer (B23-M8: corner breaks need a hammer) breaks the corner of a
     * diamond block but drops nothing (Vanilla: the block needs iron), the cell takes as long as the block and drops nothing
     * to it either; creative: 7 octets, no drop.
     */
    static void cornerBreakNeedsTheToolAndCreativeDropsNothing(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CELL);
        ServerLevel level = h.getLevel();
        ServerPlayer player = survivalPlayer(h);
        ItemStack stoneHammer = hammer("stone");
        ItemStack diamondHammer = hammer("diamond");
        if (stoneHammer == null || diamondHammer == null) {
            h.succeed();
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, stoneHammer);
        player.setShiftKeyDown(true);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        aimFromAbove(h, player, 1, 0);
        player.gameMode.destroyBlock(pos);
        BlockState rest = h.getBlockState(CELL);
        h.assertTrue(rest.is(cell(Blocks.DIAMOND_BLOCK)) && OctetCellBlock.count(rest) == 7, "seven octets after the wooden pickaxe: " + rest);
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "a stone hammer got a diamond octet");
        // mining speed and drops of the cell, compared with a plain wooden pickaxe (the hammer's own area slow-down is not the topic)
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        float cell = rest.getDestroyProgress(player, level, pos);
        float block = Blocks.DIAMOND_BLOCK.defaultBlockState().getDestroyProgress(player, level, pos);
        h.assertTrue(Math.abs(cell - block) < 1.0E-6F, "the cell mines like the diamond block: " + cell + " vs " + block);
        // the same with the stone hammer in hand (a-mech23a: the hammer's slow-down once counted twice on the cell)
        player.setItemInHand(InteractionHand.MAIN_HAND, stoneHammer);
        float cellHammer = rest.getDestroyProgress(player, level, pos);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        float blockHammer = h.getBlockState(CELL).getDestroyProgress(player, level, pos);
        h.setBlock(CELL, rest);
        h.assertTrue(Math.abs(cellHammer - blockHammer) < 1.0E-6F, "the cell mines like the diamond block with a hammer: " + cellHammer + " vs " + blockHammer);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
        player.setShiftKeyDown(false);
        player.gameMode.destroyBlock(pos);
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "the diamond cell dropped octets to a wooden pickaxe");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        h.setBlock(CELL, rest);
        player.gameMode.destroyBlock(pos);
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 7, "an iron pickaxe gets the seven octets");
        clearDrops(h);
        // creative: the corner goes, nothing drops
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, diamondHammer);
        player.setShiftKeyDown(true);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        aimFromAbove(h, player, 0, 0);
        player.gameMode.destroyBlock(pos);
        h.assertTrue(OctetCellBlock.count(h.getBlockState(CELL)) == 7, "creative: seven octets left: " + h.getBlockState(CELL));
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "creative: no drop");
        player.setShiftKeyDown(false);
        h.succeed();
    }

    /**
     * A refused break changes nothing (adventure mode: the same check as Vanilla's protection, which every loader runs
     * before the octet code; claims hook in there too). Area breaks while sneaking (vein/strip miner, hammer) mine the
     * neighbours they break whole, and the start block of that break too.
     */
    static void cornerBreakRespectsProtectionAndAreaBreaks(GameTestHelper h) {
        BlockPos pos = h.absolutePos(CELL);
        ServerPlayer player = survivalPlayer(h);
        ItemStack hammer = hammer("diamond");
        if (hammer == null) {
            h.succeed();
            return;
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
        player.setShiftKeyDown(true);
        h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
        aimFromAbove(h, player, 1, 1);
        player.setGameMode(GameType.ADVENTURE);
        h.assertFalse(player.gameMode.destroyBlock(pos), "adventure mode broke a corner");
        h.assertBlockPresent(Blocks.DIAMOND_BLOCK, CELL);
        h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "adventure mode: no drop");
        player.setGameMode(GameType.SURVIVAL);
        // an area break: first a neighbour the player does not look at, then the looked-at block, in one tick
        h.setBlock(CELL.east(), Blocks.DIAMOND_BLOCK);
        player.gameMode.destroyBlock(pos.east());
        h.assertBlockPresent(Blocks.AIR, CELL.east());
        player.gameMode.destroyBlock(pos);
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, Items.DIAMOND_BLOCK), 2, "an area break mines whole blocks");
        player.setShiftKeyDown(false);
        h.succeed();
    }

    /** The mixed cell survives a save and load of its block entity; without a player every octet drops; blueprints keep the main material. */
    static void mixedCellPersistsAndDropsEveryMaterial(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(CELL);
        BlockState[] parts = new BlockState[8];
        parts[0] = material(Blocks.OAK_PLANKS);
        parts[1] = material(Blocks.OAK_PLANKS);
        parts[2] = material(Blocks.STONE);
        parts[5] = material(Blocks.STONE_BRICKS);
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        BlockState mixed = h.getBlockState(CELL);
        h.assertTrue(mixed.is(OctetRegistry.MIXED_OCTET), "mixed: " + mixed);
        h.assertValueEqual(mixed.getValue(MixedOctetBlock.LIGHT), parts[5].getLightEmission(), "light of the brightest material");
        MixedOctetBlockEntity be = (MixedOctetBlockEntity) level.getBlockEntity(pos);
        CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
        BlockEntity loaded = BlockEntity.loadStatic(pos, mixed, tag, level.registryAccess());
        h.assertTrue(loaded instanceof MixedOctetBlockEntity, "the mixed cell loads its block entity");
        BlockState[] back = ((MixedOctetBlockEntity) loaded).parts();
        h.assertTrue(back[0] == parts[0] && back[1] == parts[1] && back[2] == parts[2] && back[5] == parts[5], "materials after save and load: " + tag);
        BlockState scanned = OctetCells.portable(level, pos, mixed);
        h.assertTrue(scanned.is(cell(Blocks.OAK_PLANKS)) && OctetCellBlock.mask(scanned) == OctetCellBlock.mask(mixed),
                "a blueprint keeps the most common material with every octet: " + scanned);
        level.destroyBlock(pos, true);
        h.assertBlockPresent(Blocks.AIR, CELL);
        h.assertValueEqual(dropped(h, piece(Blocks.OAK_PLANKS)), 2, "oak octets");
        h.succeed();
    }

    /** Sneak + right-click with a melon slice places a melon octet; without sneaking it is eaten; it drops its slice. */
    static void melonSlicePlacesMelonOctets(GameTestHelper h) {
        h.setBlock(CELL.below(), Blocks.STONE);
        ServerPlayer player = mockPlayer(h);
        ItemStack slices = new ItemStack(Items.MELON_SLICE, 3);
        player.setShiftKeyDown(false);
        h.assertFalse(Items.MELON_SLICE.useOn(topClick(h, player, slices, 1, 1)).consumesAction(), "a melon slice was placed without sneaking");
        h.assertBlockPresent(Blocks.AIR, CELL);
        player.setShiftKeyDown(true);
        h.assertTrue(Items.MELON_SLICE.useOn(topClick(h, player, slices, 1, 1)).consumesAction(), "the sneaking melon slice was not placed");
        player.setShiftKeyDown(false);
        BlockState placed = h.getBlockState(CELL);
        h.assertTrue(placed.is(OctetRegistry.MELON_OCTET) && OctetCellBlock.mask(placed) == bit(1, 0, 1), "melon octet not in the clicked corner: " + placed);
        h.assertValueEqual(slices.getCount(), 2, "melon slice consumed");
        h.getLevel().destroyBlock(h.absolutePos(CELL), true);
        h.assertValueEqual(dropped(h, Items.MELON_SLICE), 1, "the melon octet drops its slice");
        h.succeed();
    }

    /**
     * PLAN40 simpleoctets-4 (A12): both Vanilla-input changes can be switched off on the server. Melon placing off: the sneaking
     * slice is not placed (it is eaten as usual). Corner breaking off: sneaking with a hammer breaks the whole block like Vanilla.
     * The file's defaults are on and survive a broken file.
     */
    static void vanillaInputSwitches(GameTestHelper h) {
        boolean melon = com.simpleoctets.OctetsConfig.melonSlicePlacing, corner = com.simpleoctets.OctetsConfig.cornerBreaking;
        try {
            com.simpleoctets.OctetsConfig.reset();
            h.assertTrue(com.simpleoctets.OctetsConfig.melonSlicePlacing && com.simpleoctets.OctetsConfig.cornerBreaking, "defaults on");
            com.google.gson.JsonObject broken = new com.google.gson.JsonObject();
            broken.addProperty("features", "nonsense");
            com.simpleoctets.OctetsConfig.apply(broken);
            h.assertTrue(com.simpleoctets.OctetsConfig.melonSlicePlacing && com.simpleoctets.OctetsConfig.cornerBreaking, "broken group keeps the defaults");
            com.google.gson.JsonObject off = com.simpleoctets.OctetsConfig.toJson();
            off.getAsJsonObject("features").addProperty("melonSlicePlacing", false);
            off.getAsJsonObject("features").addProperty("cornerBreaking", false);
            com.simpleoctets.OctetsConfig.apply(off);
            h.assertTrue(!com.simpleoctets.OctetsConfig.melonSlicePlacing && !com.simpleoctets.OctetsConfig.cornerBreaking, "switched off from the file");

            h.setBlock(CELL.below(), Blocks.STONE);
            ServerPlayer eater = mockPlayer(h);
            ItemStack slices = new ItemStack(Items.MELON_SLICE, 3);
            eater.setShiftKeyDown(true);
            h.assertFalse(Items.MELON_SLICE.useOn(topClick(h, eater, slices, 1, 1)).consumesAction(), "melon placing off: the sneaking slice was placed");
            eater.setShiftKeyDown(false);
            h.assertBlockPresent(Blocks.AIR, CELL);
            h.assertValueEqual(slices.getCount(), 3, "melon placing off: no slice used");

            ItemStack hammer = hammer("diamond");
            if (hammer != null) {
                BlockPos pos = h.absolutePos(CELL);
                ServerPlayer player = survivalPlayer(h);
                player.setItemInHand(InteractionHand.MAIN_HAND, hammer);
                player.setShiftKeyDown(true);
                h.setBlock(CELL, Blocks.DIAMOND_BLOCK);
                aimFromAbove(h, player, 1, 1);
                h.assertTrue(player.gameMode.destroyBlock(pos), "the break was refused");
                player.setShiftKeyDown(false);
                h.assertBlockPresent(Blocks.AIR, CELL);
                h.assertValueEqual(dropped(h, Items.DIAMOND_BLOCK), 1, "corner breaking off: the hammer breaks the whole block");
                h.assertValueEqual(dropped(h, piece(Blocks.DIAMOND_BLOCK)), 0, "and no octet drops");
            }
        } finally {
            com.simpleoctets.OctetsConfig.melonSlicePlacing = melon;
            com.simpleoctets.OctetsConfig.cornerBreaking = corner;
        }
        h.succeed();
    }

    /**
     * PLAN40 simpleoctets-2: an octet keeps what its block does. Wool octets and the Hushed Deepslate Tiles octet swallow
     * vibrations (Vanilla tags dampens_vibrations + occludes_vibration_signals) like wool; SimpleBuilding's purpur blocks give
     * Purpur octets (their sound, not End stone's), and every astral block glows (light 10) like its block.
     */
    static void octetsKeepTheirBlocksRules(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        List<String> dampening = new ArrayList<>(List.of("white_wool_octet", "black_wool_octet", "lime_wool_octet"));
        if (SimpleOctets.isLoaded("simpleblocks")) dampening.add("hushed_deepslate_tiles_octet");
        for (String path : dampening) {
            BlockState state = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id(path)).defaultBlockState();
            if (!state.is(net.minecraft.tags.BlockTags.DAMPENS_VIBRATIONS)) problems.add(path + " does not dampen vibrations");
            if (!state.is(net.minecraft.tags.BlockTags.OCCLUDES_VIBRATION_SIGNALS)) problems.add(path + " does not occlude vibrations");
        }
        if (cell(Blocks.STONE).defaultBlockState().is(net.minecraft.tags.BlockTags.DAMPENS_VIBRATIONS)) problems.add("stone octet dampens vibrations");
        if (SimpleOctets.isLoaded("simplebuilding")) {
            for (String purpur : List.of("astral_purpur_block_octet", "nihil_purpur_block_octet", "ender_purpur_block_octet")) {
                Block octet = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id(purpur));
                if (octet.defaultBlockState().getSoundType() != Blocks.PURPUR_BLOCK.defaultBlockState().getSoundType()) problems.add(purpur + " does not sound like purpur");
            }
            for (String astral : List.of("astral_purpur_block", "astral_end_stone", "astralit_block")) {
                Block block = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("simplebuilding", astral));
                Block octet = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id(astral + "_octet"));
                if (octet.defaultBlockState().getLightEmission() != block.defaultBlockState().getLightEmission()) {
                    problems.add(astral + " octet light " + octet.defaultBlockState().getLightEmission() + " != block " + block.defaultBlockState().getLightEmission());
                }
            }
        }
        h.assertTrue(problems.isEmpty(), "octet rules: " + problems);
        h.succeed();
    }

    /** N86-7 (owner 18.11.): unwaxed copper octets age like Vanilla copper (random ticks, octets kept), waxed ones keep their look. */
    static void unwaxedCopperOctetsAge(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        Block copper = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id("copper_block_octet"));
        Block exposed = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id("exposed_copper_octet"));
        Block waxed = BuiltInRegistries.BLOCK.getValue(SimpleOctets.id("waxed_cut_copper_octet"));
        var entry = com.simplelib.api.aging.Aging.entry(copper);
        if (entry == null || !entry.ages()) problems.add("copper octet is no aging stage");
        if (com.simplelib.api.aging.Aging.entry(waxed) == null || !com.simplelib.api.aging.Aging.entry(waxed).waxed()) problems.add("waxed octet not waxed");
        BlockState two = OctetCellBlock.withMask(copper.defaultBlockState(), bit(0, 0, 0) | bit(1, 0, 0));
        h.setBlock(CELL, two);
        BlockPos pos = h.absolutePos(CELL);
        var random = net.minecraft.util.RandomSource.create(86L);
        for (int i = 0; i < 2000 && h.getBlockState(CELL).is(copper); i++) h.getBlockState(CELL).randomTick(h.getLevel(), pos, random);
        BlockState aged = h.getBlockState(CELL);
        if (!aged.is(exposed) || OctetCellBlock.mask(aged) != (bit(0, 0, 0) | bit(1, 0, 0))) problems.add("random ticks: " + aged);
        h.setBlock(CELL.east(), waxed);
        for (int i = 0; i < 400; i++) h.getBlockState(CELL.east()).randomTick(h.getLevel(), h.absolutePos(CELL.east()), random);
        if (!h.getBlockState(CELL.east()).is(waxed)) problems.add("waxed octet aged");
        h.assertTrue(problems.isEmpty(), "copper octets: " + problems);
        h.succeed();
    }

    /** Wood octets burn like their planks (not Nether wood), wool octets like wool. */
    static void woodAndWoolOctetsBurn(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (Block cell : OctetRegistry.WOOD_OCTETS) {
            Block planks = ((MaterialOctetBlock) cell).source();
            if ((com.simplelib.api.shape.ShapeMaterials.igniteOdds(cell.defaultBlockState()) > 0) != (com.simplelib.api.shape.ShapeMaterials.igniteOdds(planks.defaultBlockState()) > 0)) {
                problems.add("fire of " + BuiltInRegistries.BLOCK.getKey(cell));
            }
        }
        if (com.simplelib.api.shape.ShapeMaterials.igniteOdds(cell(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool"))).defaultBlockState()) <= 0) problems.add("wool octet does not burn");
        if (com.simplelib.api.shape.ShapeMaterials.igniteOdds(cell(Blocks.STONE).defaultBlockState()) > 0) problems.add("stone octet burns");
        h.assertTrue(problems.isEmpty(), "fire: " + problems);
        h.succeed();
    }

    /** Log and stem octets (also stripped) are wood: axe tag, not pickaxe. */
    static void logOctetsMineWithTheAxe(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Block cell : OctetRegistry.MATERIAL_OCTETS) {
            String path = BuiltInRegistries.BLOCK.getKey(cell).getPath();
            if (!path.endsWith("_log_octet") && !path.endsWith("_stem_octet")) continue;
            checked++;
            BlockState state = cell.defaultBlockState();
            if (!state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_AXE)) problems.add("not axe: " + path);
            if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) problems.add("pickaxe: " + path);
        }
        h.assertTrue(checked >= 24, "only " + checked + " log/stem octets found");
        h.assertTrue(problems.isEmpty(), "mining tool: " + problems);
        h.succeed();
    }

    // ===================================================================================== helpers

    /**
     * N100: an axe on a cell of log octets strips only the clicked octet (the cell turns mixed: one stripped, three plain),
     * reports SUCCESS (arm swing) and costs one durability; on the stripped octet it does nothing more.
     */
    static void axeStripsOnlyTheClickedOctet(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(CELL);
        BlockState log = material(Blocks.OAK_LOG);
        BlockState stripped = material(Blocks.STRIPPED_OAK_LOG);
        BlockState[] parts = new BlockState[8];
        for (int i = 0; i < 8; i++) {
            parts[i] = OctetCellBlock.y(i) == 0 ? log : null;
        }
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        ServerPlayer player = mockPlayer(h);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        int clicked = OctetCellBlock.index(1, 0, 0);
        BlockHitResult hit = new BlockHitResult(new Vec3(pos.getX() + 0.75, pos.getY() + 0.5, pos.getZ() + 0.25), Direction.UP, pos, false);
        InteractionResult result = h.getBlockState(CELL).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(result == InteractionResult.SUCCESS, "the axe reports SUCCESS: " + result);
        BlockState[] now = OctetCells.parts(level, pos, h.getBlockState(CELL));
        for (int i = 0; i < 8; i++) {
            BlockState want = i == clicked ? stripped : parts[i];
            h.assertTrue(now[i] == want, "octet " + i + " is " + now[i] + ", wanted " + want);
        }
        h.assertTrue(h.getBlockState(CELL).is(OctetRegistry.MIXED_OCTET), "log + stripped log make a mixed cell: " + h.getBlockState(CELL));
        h.assertValueEqual(axe.getDamageValue(), 1, "axe durability");
        InteractionResult again = h.getBlockState(CELL).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertFalse(again.consumesAction(), "a stripped octet is not stripped again: " + again);
        h.assertValueEqual(axe.getDamageValue(), 1, "axe durability after the second click");
        h.succeed();
    }

    /**
     * N120: the strip also works on a side face and on stems (crimson stem octets, a 7-of-8 cell like the probe scene): only the
     * clicked eighth turns stripped, the gap stays empty; an axe on a stone octet does nothing (no strip, no durability).
     */
    static void axeStripsStemOctetsNotStone(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos pos = h.absolutePos(CELL);
        BlockState stem = material(Blocks.CRIMSON_STEM);
        BlockState stripped = material(Blocks.STRIPPED_CRIMSON_STEM);
        int gap = OctetCellBlock.index(1, 1, 1);
        BlockState[] parts = new BlockState[8];
        for (int i = 0; i < 8; i++) parts[i] = i == gap ? null : stem;
        OctetCells.set(level, pos, parts, false, Block.UPDATE_ALL);
        ServerPlayer player = mockPlayer(h);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        // north face, upper east quarter: the octet x 1, y 1, z 0
        int clicked = OctetCellBlock.index(1, 1, 0);
        BlockHitResult north = new BlockHitResult(new Vec3(pos.getX() + 0.75, pos.getY() + 0.75, pos.getZ()), Direction.NORTH, pos, false);
        InteractionResult result = h.getBlockState(CELL).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, north);
        h.assertTrue(result == InteractionResult.SUCCESS, "the axe strips a crimson stem octet: " + result);
        BlockState[] now = OctetCells.parts(level, pos, h.getBlockState(CELL));
        for (int i = 0; i < 8; i++) {
            BlockState want = i == gap ? null : i == clicked ? stripped : stem;
            h.assertTrue(now[i] == want, "octet " + i + " is " + now[i] + ", wanted " + want);
        }
        h.assertTrue(h.getBlockState(CELL).is(OctetRegistry.MIXED_OCTET), "stem + stripped stem make a mixed cell: " + h.getBlockState(CELL));
        h.assertValueEqual(axe.getDamageValue(), 1, "axe durability after the strip");
        // a stone octet cell: the axe passes, nothing changes
        BlockState stone = material(Blocks.STONE);
        BlockState[] stoneParts = new BlockState[8];
        for (int i = 0; i < 8; i++) stoneParts[i] = i == gap ? null : stone;
        OctetCells.set(level, pos, stoneParts, false, Block.UPDATE_ALL);
        BlockState before = h.getBlockState(CELL);
        InteractionResult onStone = before.useItemOn(axe, level, player, InteractionHand.MAIN_HAND, north);
        h.assertFalse(onStone.consumesAction(), "an axe on a stone octet does nothing: " + onStone);
        h.assertTrue(h.getBlockState(CELL) == before, "the stone cell is unchanged: " + h.getBlockState(CELL));
        h.assertTrue(OctetCells.parts(level, pos, h.getBlockState(CELL))[clicked] == stone, "the clicked stone octet stays stone");
        h.assertValueEqual(axe.getDamageValue(), 1, "axe durability after the stone click");
        h.succeed();
    }

    static Block cell(Block full) {
        return com.simplelib.api.OctetCells.cellFor(full);
    }

    static Item piece(Block full) {
        return ((MaterialOctetBlock) cell(full)).piece();
    }

    static BlockState material(Block full) {
        return OctetCells.material(cell(full).defaultBlockState());
    }

    static int bit(int x, int y, int z) {
        return 1 << OctetCellBlock.index(x, y, z);
    }

    /** A click on the top face of the block below {@link #CELL} at the given quarter (the lower octet there). */
    static UseOnContext topClick(GameTestHelper h, ServerPlayer player, ItemStack stack, int x, int z) {
        BlockPos floor = h.absolutePos(CELL.below());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult top = new BlockHitResult(new Vec3(floor.getX() + x * 0.5 + 0.25, floor.getY() + 1.0, floor.getZ() + z * 0.5 + 0.25),
                Direction.UP, floor, false);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, top);
    }

    static ServerPlayer mockPlayer(GameTestHelper h) {
        ServerPlayer player = (ServerPlayer) h.makeMockServerPlayer(GameType.SURVIVAL);
        Vec3 away = h.absoluteVec(new Vec3(0.5, 8.0, 0.5));
        player.snapTo(away.x, away.y, away.z, 0.0F, 0.0F);
        return player;
    }

    static int dropped(GameTestHelper h, Item item) {
        int count = 0;
        for (ItemEntity entity : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CELL)).inflate(3.0))) {
            if (entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        return count;
    }

    /** A real survival player (the mock player cannot run the block break with drops). */
    static ServerPlayer survivalPlayer(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "octet-tests"), false);
        ServerLevel level = h.getLevel();
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        h.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }
}
