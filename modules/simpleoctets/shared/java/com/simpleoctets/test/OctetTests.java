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
    static final BlockPos CELL = new BlockPos(2, 2, 2);

    static {
        ALL.put("registered", OctetTests::registered);
        ALL.put("stairs_and_slabs_have_octets", OctetTests::stairsAndSlabsHaveOctets);
        ALL.put("sub_mod_switch", OctetTests::subModSwitch);
        ALL.put("migration_from_simplebuilding", OctetTests::migrationFromSimplebuilding);
        ALL.put("foreign_materials_only_with_their_mod", OctetTests::foreignMaterialsOnlyWithTheirMod);
        ALL.put("recipes_cut_and_craft_back", OctetTests::recipesCutAndCraftBack);
        ALL.put("cells_for_carving_tools", OctetTests::cellsForCarvingTools);
        ALL.put("octets_place_into_the_sub_grid", OctetTests::octetsPlaceIntoTheSubGrid);
        ALL.put("octets_mix_materials_in_one_cell", OctetTests::octetsMixMaterialsInOneCell);
        ALL.put("sneaking_breaks_only_the_looked_at_octet", OctetTests::sneakingBreaksOnlyTheLookedAtOctet);
        ALL.put("eight_octets_of_one_material_become_the_full_block", OctetTests::eightOctetsOfOneMaterialBecomeTheFullBlock);
        ALL.put("sneaking_breaks_one_octet_off_full_blocks", OctetTests::sneakingBreaksOneOctetOffFullBlocks);
        ALL.put("corner_break_needs_the_tool_and_creative_drops_nothing", OctetTests::cornerBreakNeedsTheToolAndCreativeDropsNothing);
        ALL.put("corner_break_respects_protection_and_area_breaks", OctetTests::cornerBreakRespectsProtectionAndAreaBreaks);
        ALL.put("corner_break_only_with_a_hammer", OctetTests::cornerBreakOnlyWithAHammer);
        ALL.put("mixed_cell_persists_and_drops_every_material", OctetTests::mixedCellPersistsAndDropsEveryMaterial);
        ALL.put("melon_slice_places_melon_octets", OctetTests::melonSlicePlacesMelonOctets);
        ALL.put("wood_and_wool_octets_burn", OctetTests::woodAndWoolOctetsBurn);
        ALL.put("log_octets_mine_with_the_axe", OctetTests::logOctetsMineWithTheAxe);
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
        h.assertTrue(OctetRegistry.tabStacks().size() == items.size() + 1, "tab: every octet item and the melon slice");
        h.succeed();
    }

    /** Octet rule (docs/ai/PRINZIPIEN.md O1): reasoned exceptions of the coverage test, by stair/slab path (substring). */
    static final Map<String, String> NO_OCTET = Map.of(
            "dirt_slab", "natural slab (owner 17.10.: no octets for earth, grass, sand, gravel)",
            "grass_slab", "natural slab (owner 17.10.)",
            "sand_slab", "natural slab (owner 17.10.)",
            "gravel_slab", "natural slab (owner 17.10.)",
            "petrified_oak_slab", "no full block: it looks like oak planks, whose octet (oak_octet) exists",
            "checker", "chess checkers have the chess octet (checker_octet, SimpleBuilding)");

    /**
     * Every registered stair and slab (any mod) has an octet of its full block or a reasoned exception: a future block
     * family with stairs fails here until it gets an entry in the octet generator (tools/textures/octets_2026_10_09.py).
     */
    static void stairsAndSlabsHaveOctets(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            boolean stairs = block instanceof net.minecraft.world.level.block.StairBlock;
            if (!stairs && !(block instanceof net.minecraft.world.level.block.SlabBlock)) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (NO_OCTET.keySet().stream().anyMatch(id.getPath()::contains)) continue;
            String stem = id.getPath().substring(0, id.getPath().length() - (stairs ? 7 : 5));
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
        h.assertTrue(checked > 100, "only " + checked + " stairs/slabs checked");
        h.assertTrue(problems.isEmpty(), "stairs/slabs without an octet or a reasoned exception: " + problems);
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
    static void cornerBreakOnlyWithAHammer(GameTestHelper h) {
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
        float cell = rest.getDestroyProgress(player, level, pos);
        float block = Blocks.DIAMOND_BLOCK.defaultBlockState().getDestroyProgress(player, level, pos);
        h.assertTrue(Math.abs(cell - block) < 1.0E-6F, "the cell mines like the diamond block: " + cell + " vs " + block);
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

    /** Wood octets burn like their planks (not Nether wood), wool octets like wool. */
    static void woodAndWoolOctetsBurn(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var fire = (com.simpleoctets.mixin.FireBlockAccessor) Blocks.FIRE;
        for (Block cell : OctetRegistry.WOOD_OCTETS) {
            Block planks = ((MaterialOctetBlock) cell).source();
            if ((fire.simpleoctets$getIgniteOdds(cell.defaultBlockState()) > 0) != (fire.simpleoctets$getIgniteOdds(planks.defaultBlockState()) > 0)) {
                problems.add("fire of " + BuiltInRegistries.BLOCK.getKey(cell));
            }
        }
        if (fire.simpleoctets$getIgniteOdds(cell(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool"))).defaultBlockState()) <= 0) problems.add("wool octet does not burn");
        if (fire.simpleoctets$getIgniteOdds(cell(Blocks.STONE).defaultBlockState()) > 0) problems.add("stone octet burns");
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
