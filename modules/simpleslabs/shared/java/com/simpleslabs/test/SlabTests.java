package com.simpleslabs.test;

import com.simplelib.api.aging.Aging;
import com.simpleslabs.SimpleSlabs;
import com.simpleslabs.block.HalfSlabBlock;
import com.simpleslabs.block.LayeredSlabBlock;
import com.simpleslabs.block.SideHalfSlabBlock;
import com.simpleslabs.item.HalfSlabItem;
import com.simpleslabs.registry.HalfSlabMaterials;
import com.simpleslabs.registry.SlabRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Loader independent module tests of Simple Slabs (N86); the Fabric adapter registers them on {@code simpleslabs:empty}.
 * Materials of another mod (sand, gravel, grass: Simple Blocks) are only checked while that mod is loaded (said in the message).
 */
public final class SlabTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();
    static final BlockPos CELL = new BlockPos(2, 2, 2);

    static {
        ALL.put("registered", SlabTests::registered);
        ALL.put("every_slab_has_its_half_slab", SlabTests::everySlabHasItsHalfSlab);
        ALL.put("owner_layer_patterns_by_clicks", SlabTests::ownerLayerPatternsByClicks);
        ALL.put("top_and_bottom_placement", SlabTests::topAndBottomPlacement);
        ALL.put("shapes_follow_the_layers", SlabTests::shapesFollowTheLayers);
        ALL.put("side_half_slab_and_side_slab", SlabTests::sideHalfSlabAndSideSlab);
        ALL.put("half_slab_onto_its_slab", SlabTests::halfSlabOntoItsSlab);
        ALL.put("every_layer_drops", SlabTests::everyLayerDrops);
        ALL.put("water_and_full_cells", SlabTests::waterAndFullCells);
        ALL.put("copper_half_slabs_age_like_copper", SlabTests::copperHalfSlabsAgeLikeCopper);
        ALL.put("sand_and_gravel_half_slabs_fall", SlabTests::sandAndGravelHalfSlabsFall);
        ALL.put("falling_layers_merge", SlabTests::fallingLayersMerge);
        ALL.put("grass_half_slab_is_tinted_grass", SlabTests::grassHalfSlabIsTintedGrass);
        ALL.put("recipes_cut_and_craft_back", SlabTests::recipesCutAndCraftBack);
        ALL.put("wood_and_wool_burn", SlabTests::woodAndWoolBurn);
        ALL.put("creative_tab_and_switch", SlabTests::creativeTabAndSwitch);
        ALL.put("guide_book", com.simpleslabs.guide.SlabsGuide::gameTest);
    }

    private SlabTests() {}

    // ===================================================================================== registration

    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        int expected = 0;
        for (HalfSlabMaterials.Material m : HalfSlabMaterials.ALL) {
            boolean loaded = m.mods().stream().allMatch(SimpleSlabs::isLoaded);
            if (loaded) expected++;
            Identifier id = SimpleSlabs.id(m.id());
            Identifier side = SimpleSlabs.id(SlabRegistry.sidePath(m.id()));
            if (BuiltInRegistries.BLOCK.containsKey(id) != loaded) problems.add(id + " registered=" + !loaded);
            if (BuiltInRegistries.BLOCK.containsKey(side) != loaded) problems.add(side + " registered=" + !loaded);
            if (BuiltInRegistries.ITEM.containsKey(side)) problems.add(side + " must have no item");
            if (loaded && !(BuiltInRegistries.ITEM.getValue(id) instanceof HalfSlabItem item && item.halfSlab() == BuiltInRegistries.BLOCK.getValue(id))) {
                problems.add("item of " + id);
            }
        }
        if (HalfSlabMaterials.ALL.size() != 170) problems.add("material list holds " + HalfSlabMaterials.ALL.size() + ", owner N86-5: 170");
        if (SlabRegistry.HALF_SLABS.size() != expected || SlabRegistry.SIDE_SLABS.size() != expected) problems.add("registered " + SlabRegistry.HALF_SLABS.size());
        for (HalfSlabBlock block : SlabRegistry.HALF_SLABS) {
            if (block.getStateDefinition().getPossibleStates().size() != 512) problems.add("states of " + block);
            if (block.side().getStateDefinition().getPossibleStates().size() != 64) problems.add("side states of " + block);
            if (block.defaultBlockState().is(net.minecraft.tags.BlockTags.SLABS)) problems.add(block + " in #minecraft:slabs (N86-11)");
        }
        h.assertTrue(problems.isEmpty(), "registration: " + problems);
        h.succeed();
    }

    /** Owner N86-5: every slab of a covered namespace has its half slab (only petrified_oak_slab, without a block of its own, not). */
    static void everySlabHasItsHalfSlab(GameTestHelper h) {
        Set<String> namespaces = HalfSlabMaterials.ALL.stream().map(m -> Identifier.parse(m.slab()).getNamespace()).collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            if (!(block instanceof SlabBlock) || !namespaces.contains(id.getNamespace()) || id.getPath().equals("petrified_oak_slab")) continue;
            checked++;
            String half = id.getPath().substring(0, id.getPath().length() - "_slab".length()) + "_half_slab";
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleSlabs.id(half))) missing.add(id.toString());
        }
        h.assertTrue(missing.isEmpty() && checked >= 100, "slabs without half slab (" + checked + " checked): " + missing);
        h.succeed();
    }

    // ===================================================================================== placing

    /** The owner's examples by real clicks: 0-4 + 12-16 px = layers {0,1,6,7}, 0-4 + 8-12 px = {0,1,4,5}. */
    static void ownerLayerPatternsByClicks(GameTestHelper h) {
        ServerPlayer player = mockPlayer(h);
        int[][] patterns = {{6, 7}, {4, 5}};
        int[] masks = {0b11000011, 0b00110011};
        for (int p = 0; p < patterns.length; p++) {
            BlockPos cell = CELL.east(p);
            h.setBlock(cell.below(), Blocks.STONE);
            ItemStack stack = new ItemStack(item(Blocks.STONE_SLAB), 8);
            h.assertValueEqual(stack.getItem().useOn(click(h, player, stack, cell.below(), Direction.UP, 0.5, 1.0, 0.5)), InteractionResult.SUCCESS,
                    "first half slab on the floor");
            // on the half slab's top face (y = 2 px): the next layer
            stack.getItem().useOn(click(h, player, stack, cell, Direction.UP, 0.5, 2 / 16.0, 0.5));
            for (int layer : patterns[p]) {
                // on its side at the height of the wanted layer
                stack.getItem().useOn(click(h, player, stack, cell, Direction.NORTH, 0.5, (layer * 2 + 1) / 16.0, 0.0));
            }
            BlockState state = h.getBlockState(cell);
            h.assertTrue(state.is(half(Blocks.STONE_SLAB)) && HalfSlabBlock.mask(state) == masks[p],
                    "pattern " + p + ": mask " + Integer.toBinaryString(state.getBlock() instanceof HalfSlabBlock ? HalfSlabBlock.mask(state) : -1));
            h.assertValueEqual(stack.getCount(), 4, "one item per layer");
        }
        h.succeed();
    }

    /** A click on a bottom face puts layer 7 under the block; a side face the clicked height (a neighbour cell). */
    static void topAndBottomPlacement(GameTestHelper h) {
        ServerPlayer player = mockPlayer(h);
        h.setBlock(CELL.above(), Blocks.STONE);
        ItemStack stack = new ItemStack(item(Blocks.OAK_SLAB), 4);
        stack.getItem().useOn(click(h, player, stack, CELL.above(), Direction.DOWN, 0.5, 0.0, 0.5));
        h.assertValueEqual(HalfSlabBlock.mask(h.getBlockState(CELL)), 1 << 7, "under a block: layer 7");
        h.setBlock(CELL.east().east(), Blocks.STONE);
        stack.getItem().useOn(click(h, player, stack, CELL.east().east(), Direction.WEST, 0.0, 5 / 16.0, 0.5));
        BlockState side = h.getBlockState(CELL.east());
        h.assertTrue(side.is(half(Blocks.OAK_SLAB)) && HalfSlabBlock.mask(side) == 1 << 2, "side face at 5 px: layer 2, got " + side);
        h.succeed();
    }

    static void shapesFollowTheLayers(GameTestHelper h) {
        HalfSlabBlock block = half(Blocks.STONE_SLAB);
        BlockPos pos = h.absolutePos(CELL);
        BlockState bottom = block.withMask(0b11);
        var shape = bottom.getShape(h.getLevel(), pos);
        h.assertTrue(shape.min(Direction.Axis.Y) == 0 && shape.max(Direction.Axis.Y) == 4 / 16.0, "two bottom layers = 0-4 px");
        BlockState gap = block.withMask(0b11000011);
        h.assertTrue(gap.getShape(h.getLevel(), pos).toAabbs().size() == 2, "0-4 + 12-16 px are two boxes");
        h.assertTrue(block.withMask(0xFF).isCollisionShapeFullBlock(h.getLevel(), pos), "eight layers are a full cube");
        h.assertTrue(bottom.isFaceSturdy(h.getLevel(), pos, Direction.DOWN) && !bottom.isFaceSturdy(h.getLevel(), pos, Direction.UP),
                "bottom layers carry on the floor side only");
        h.succeed();
    }

    /** Sneaking on a side face: side half slab against that block; again and again: thicker, 4 layers = side slab. */
    static void sideHalfSlabAndSideSlab(GameTestHelper h) {
        ServerPlayer player = mockPlayer(h);
        player.setShiftKeyDown(true);
        BlockPos wall = CELL.west();
        h.setBlock(wall, Blocks.STONE);
        ItemStack stack = new ItemStack(item(Blocks.STONE_SLAB), 8);
        for (int i = 1; i <= 4; i++) {
            stack.getItem().useOn(click(h, player, stack, wall, Direction.EAST, 1.0, 0.5, 0.5));
            BlockState state = h.getBlockState(CELL);
            h.assertTrue(state.getBlock() instanceof SideHalfSlabBlock && state.getValue(SideHalfSlabBlock.FACING) == Direction.WEST
                    && state.getValue(SideHalfSlabBlock.THICKNESS) == i, "sneak click " + i + ": " + state);
        }
        var shape = h.getBlockState(CELL).getShape(h.getLevel(), h.absolutePos(CELL));
        h.assertTrue(shape.max(Direction.Axis.X) == 0.5 && shape.max(Direction.Axis.Y) == 1.0, "side slab = 8 px against the west wall");
        // a click on its open face (from the east) grows it too
        stack.getItem().useOn(click(h, player, stack, CELL, Direction.EAST, 0.5, 0.5, 0.5));
        h.assertValueEqual(h.getBlockState(CELL).getValue(SideHalfSlabBlock.THICKNESS), 5, "open face click grows the side form");
        h.assertValueEqual(stack.getCount(), 3, "one item per layer");
        h.succeed();
    }

    /** Owner N86-2 "slab + half slab": a half slab on its own bottom slab = layers 0-3 + 4. */
    static void halfSlabOntoItsSlab(GameTestHelper h) {
        ServerPlayer player = mockPlayer(h);
        h.setBlock(CELL, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
        ItemStack stack = new ItemStack(item(Blocks.STONE_SLAB), 2);
        h.assertValueEqual(stack.getItem().useOn(click(h, player, stack, CELL, Direction.UP, 0.5, 0.5, 0.5)), InteractionResult.SUCCESS, "onto the slab");
        BlockState state = h.getBlockState(CELL);
        h.assertTrue(state.is(half(Blocks.STONE_SLAB)) && HalfSlabBlock.mask(state) == 0b11111, "slab + half slab = layers 0-4: " + state);
        h.setBlock(CELL.east(), Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
        stack.getItem().useOn(click(h, player, stack, CELL.east(), Direction.DOWN, 0.5, 0.5, 0.5));
        h.assertValueEqual(HalfSlabBlock.mask(h.getBlockState(CELL.east())), 0b11111000, "top slab + half slab = layers 3-7");
        // another material's slab is not merged
        h.setBlock(CELL.west(), Blocks.OAK_SLAB);
        stack = new ItemStack(item(Blocks.STONE_SLAB), 1);
        stack.getItem().useOn(click(h, player, stack, CELL.west(), Direction.UP, 0.5, 0.5, 0.5));
        h.assertTrue(h.getBlockState(CELL.west()).is(Blocks.OAK_SLAB), "oak slab stays an oak slab");
        h.succeed();
    }

    // ===================================================================================== drops, water

    static void everyLayerDrops(GameTestHelper h) {
        HalfSlabBlock block = half(Blocks.STONE_SLAB);
        BlockPos pos = h.absolutePos(CELL);
        List<String> problems = new ArrayList<>();
        Object[][] cases = {{block.withMask(0b11000011), 4}, {block.withMask(0b1), 1}, {block.withMask(0xFF), 8},
                {block.side().defaultBlockState().setValue(SideHalfSlabBlock.THICKNESS, 4), 4},
                {block.side().defaultBlockState(), 1}};
        for (Object[] c : cases) {
            int n = Block.getDrops((BlockState) c[0], h.getLevel(), pos, null).stream().filter(s -> s.is(block.asItem())).mapToInt(ItemStack::getCount).sum();
            if (n != (int) c[1]) problems.add(c[0] + " drops " + n + " instead of " + c[1]);
        }
        // in the world: breaking the 4-layer cell puts exactly 4 items next to it
        clearDrops(h);
        h.setBlock(CELL, block.withMask(0b11000011));
        h.getLevel().destroyBlock(pos, true);
        int world = 0;
        for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0))) if (e.getItem().is(block.asItem())) world += e.getItem().getCount();
        if (world != 4) problems.add("breaking in the world dropped " + world);
        h.assertTrue(block.side().defaultBlockState().getCloneItemStack(h.getLevel(), pos, false).is(block.asItem()),
                "pick block on the side form gives the half slab");
        h.assertTrue(problems.isEmpty(), "drops: " + problems);
        h.succeed();
    }

    static void waterAndFullCells(GameTestHelper h) {
        ServerPlayer player = mockPlayer(h);
        HalfSlabBlock block = half(Blocks.STONE_SLAB);
        h.setBlock(CELL.below(), Blocks.STONE);
        h.setBlock(CELL, Blocks.WATER);
        ItemStack stack = new ItemStack(block.asItem(), 2);
        stack.getItem().useOn(click(h, player, stack, CELL.below(), Direction.UP, 0.5, 1.0, 0.5));
        BlockState wet = h.getBlockState(CELL);
        h.assertTrue(wet.is(block) && wet.getValue(LayeredSlabBlock.WATERLOGGED) && wet.getFluidState().is(Fluids.WATER), "placed into water: waterlogged");
        BlockState full = block.withMask(wet, 0xFF);
        h.assertFalse(full.getValue(LayeredSlabBlock.WATERLOGGED), "a full cell holds no water");
        h.assertFalse(block.canPlaceLiquid(null, h.getLevel(), h.absolutePos(CELL), full, Fluids.WATER), "no water into a full cell");
        h.assertTrue(block.canPlaceLiquid(null, h.getLevel(), h.absolutePos(CELL), block.withMask(0b1), Fluids.WATER), "water into one layer");
        BlockState side = block.side().defaultBlockState().setValue(SideHalfSlabBlock.THICKNESS, 8).setValue(LayeredSlabBlock.WATERLOGGED, false);
        h.assertFalse(block.side().canPlaceLiquid(null, h.getLevel(), h.absolutePos(CELL), side, Fluids.WATER), "no water into a full side form");
        h.succeed();
    }

    // ===================================================================================== copper, falling, grass

    /** N86-7: unwaxed copper half slabs (and side forms) age like Vanilla copper, waxed ones do not; honeycomb/axe through the chain. */
    static void copperHalfSlabsAgeLikeCopper(GameTestHelper h) {
        HalfSlabBlock cut = byId("cut_copper_half_slab"), exposed = byId("exposed_cut_copper_half_slab"), waxed = byId("waxed_cut_copper_half_slab");
        HalfSlabBlock oxidized = byId("oxidized_cut_copper_half_slab");
        List<String> problems = new ArrayList<>();
        var entry = Aging.entry(cut);
        if (entry == null || !entry.ages() || entry.stage() != 0) problems.add("cut copper half slab is not an aging stage 0");
        if (Aging.entry(waxed) == null || !Aging.entry(waxed).waxed()) problems.add("waxed half slab not waxed");
        if (Aging.entry(oxidized) == null || Aging.entry(oxidized).ages()) problems.add("oxidized half slab still ages");
        if (Aging.entry(cut.side()) == null || !Aging.entry(cut.side()).ages()) problems.add("side form does not age");
        BlockState state = cut.withMask(0b101);
        if (!state.isRandomlyTicking()) problems.add("unwaxed half slab does not random-tick");
        if (!Aging.next(entry, state).equals(exposed.withMask(0b101))) problems.add("next stage keeps the layers");
        if (!Aging.waxed(state).map(s -> s.equals(waxed.withMask(0b101))).orElse(false)) problems.add("honeycomb");
        // real random ticks (Vanilla chance ~1/18 per tick without neighbours): it ages within a few hundred ticks
        h.setBlock(CELL, state);
        BlockPos pos = h.absolutePos(CELL);
        RandomSource random = RandomSource.create(86L);
        for (int i = 0; i < 2000 && h.getBlockState(CELL).is(cut); i++) h.getBlockState(CELL).randomTick(h.getLevel(), pos, random);
        if (!h.getBlockState(CELL).is(exposed) || HalfSlabBlock.mask(h.getBlockState(CELL)) != 0b101) problems.add("random ticks: " + h.getBlockState(CELL));
        h.setBlock(CELL.east(), waxed.withMask(0b1));
        for (int i = 0; i < 400; i++) h.getBlockState(CELL.east()).randomTick(h.getLevel(), h.absolutePos(CELL.east()), random);
        if (!h.getBlockState(CELL.east()).is(waxed)) problems.add("waxed half slab aged");
        h.assertTrue(problems.isEmpty(), "copper: " + problems);
        h.succeed();
    }

    /** N86-5: sand, red sand and gravel half slabs fall as their layers and land bottom-aligned (Simple Blocks only). */
    static void sandAndGravelHalfSlabsFall(GameTestHelper h) {
        HalfSlabBlock sand = byId("sand_half_slab"), gravel = byId("gravel_half_slab");
        if (sand == null || gravel == null) {
            h.assertFalse(SimpleSlabs.isLoaded("simpleblocks"), "sand/gravel half slabs missing with Simple Blocks");
            h.succeed();
            return;
        }
        h.setBlock(CELL.below(2), Blocks.STONE);
        h.setBlock(CELL.east().below(2), Blocks.STONE);
        h.setBlock(CELL.west().below(), Blocks.STONE);
        h.setBlock(CELL, sand.withMask(0b10100000));                       // two floating layers
        h.setBlock(CELL.east(), gravel.side().defaultBlockState().setValue(SideHalfSlabBlock.THICKNESS, 3));
        h.setBlock(CELL.west(), sand.withMask(0b1000));                    // on the ground: settles in its cell
        h.succeedWhen(() -> {
            BlockState landed = h.getBlockState(CELL.below());
            h.assertTrue(landed.is(sand) && HalfSlabBlock.mask(landed) == 0b11, "sand fell and lies bottom-aligned: " + landed);
            h.assertTrue(h.getBlockState(CELL).isAir(), "the cell it left is empty");
            BlockState g = h.getBlockState(CELL.east().below());
            h.assertTrue(g.is(gravel) && HalfSlabBlock.mask(g) == 0b111, "gravel side form fell as 3 layers: " + g);
            BlockState settled = h.getBlockState(CELL.west());
            h.assertTrue(settled.is(sand) && HalfSlabBlock.mask(settled) == 0b1, "a floating layer on the ground settles: " + settled);
        });
    }

    /** A falling sand half slab lands on a sand half slab: the layers add up; what does not fit goes into the cell above. */
    static void fallingLayersMerge(GameTestHelper h) {
        HalfSlabBlock sand = byId("sand_half_slab");
        if (sand == null) {
            h.succeed();
            return;
        }
        h.setBlock(CELL.below(), Blocks.STONE);
        h.setBlock(CELL, sand.withMask(0b111111));          // 6 layers on the floor
        h.setBlock(CELL.above(2), sand.withMask(0b1111));   // 4 layers fall onto it: 8 here, 2 in the cell above
        h.succeedWhen(() -> {
            h.assertValueEqual(HalfSlabBlock.mask(h.getBlockState(CELL)), 0xFF, "the lower cell is full");
            BlockState above = h.getBlockState(CELL.above());
            h.assertTrue(above.is(sand) && HalfSlabBlock.mask(above) == 0b11, "2 layers in the cell above: " + above);
            int near = 0;
            for (ItemEntity e : h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CELL)).inflate(1.0))) {
                if (e.getItem().is(sand.asItem())) near += e.getItem().getCount();
            }
            h.assertValueEqual(near, 0, "nothing dropped");
        });
    }

    /** N86-5: grass half slab = its own grass model with the biome tint on the client (SlabsFabricClient); dirt is plain. */
    static void grassHalfSlabIsTintedGrass(GameTestHelper h) {
        HalfSlabBlock grass = byId("grass_half_slab");
        if (grass == null) {
            h.succeed();
            return;
        }
        h.assertTrue(SlabRegistry.GRASS.contains(grass) && SlabRegistry.GRASS.contains(grass.side()), "grass forms registered for the biome tint");
        h.assertFalse(SlabRegistry.GRASS.contains(byId("dirt_half_slab")), "dirt is not tinted");
        h.assertFalse(grass.falls() || byId("dirt_half_slab").falls(), "grass and dirt do not fall");
        h.assertTrue(byId("sand_half_slab").falls() && byId("red_sand_half_slab").falls() && byId("gravel_half_slab").falls(), "sand, red sand, gravel fall");
        h.succeed();
    }

    // ===================================================================================== recipes, fire, tab

    /** N86-6 for 2 px: saw 1 slab -> 4, 1 block -> 8 (stonecutter only without Simple Blocks); 4 half slabs -> 1 slab. */
    static void recipesCutAndCraftBack(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        boolean saw = SimpleSlabs.isLoaded("simpleblocks");
        var recipes = h.getLevel().getServer().getRecipeManager();
        for (HalfSlabBlock block : SlabRegistry.HALF_SLABS) {
            HalfSlabMaterials.Material m = SlabRegistry.material(block);
            String slabPath = Identifier.parse(m.slab()).getPath();
            var sawing = recipes.byKey(key("half_slab/" + m.id() + "_from_" + slabPath + "_sawing"));
            var cut = recipes.byKey(key("half_slab/" + m.id() + "_from_" + slabPath + "_stonecutting"));
            var back = recipes.byKey(key("half_slab/" + slabPath + "_from_half_slabs"));
            boolean ownSaw = Identifier.parse(m.slab()).getNamespace().equals("simpleblocks");
            if (sawing.isPresent() != saw) problems.add("saw " + m.id() + " present=" + sawing.isPresent());
            if (!ownSaw && cut.isPresent() == saw) problems.add("stonecutter " + m.id() + " present=" + cut.isPresent());
            if (back.isEmpty()) problems.add("no way back from " + m.id());
            cut.ifPresent(r -> {
                if (r.value() instanceof net.minecraft.world.item.crafting.SingleItemRecipe single) {
                    ItemStack out = single.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(m.slab())))));
                    if (out.getCount() != 4 || !out.is(block.asItem())) problems.add("stonecutter yield of " + m.id() + ": " + out);
                }
            });
            if (m.base() != null && !saw && !ownSaw) {
                var fromBlock = recipes.byKey(key("half_slab/" + m.id() + "_from_" + Identifier.parse(m.base()).getPath() + "_stonecutting"));
                fromBlock.ifPresent(r -> {
                    if (r.value() instanceof net.minecraft.world.item.crafting.SingleItemRecipe single) {
                        ItemStack out = single.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(m.base())))));
                        if (out.getCount() != 8) problems.add("block yield of " + m.id() + ": " + out.getCount());
                    }
                });
            }
        }
        h.assertTrue(problems.isEmpty(), "recipes (Simple Blocks " + (saw ? "loaded" : "absent") + "): " + problems);
        h.succeed();
    }

    static void woodAndWoolBurn(GameTestHelper h) {
        int oak = com.simplelib.api.shape.ShapeMaterials.igniteOdds(half(Blocks.OAK_SLAB).defaultBlockState());
        int stone = com.simplelib.api.shape.ShapeMaterials.igniteOdds(half(Blocks.STONE_SLAB).defaultBlockState());
        int wool = com.simplelib.api.shape.ShapeMaterials.igniteOdds(half(BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace("white_wool_slab"))).defaultBlockState());
        int side = com.simplelib.api.shape.ShapeMaterials.igniteOdds(half(Blocks.OAK_SLAB).side().defaultBlockState());
        h.assertTrue(oak > 0 && side > 0 && wool > 0 && stone == 0, "oak (" + oak + "/" + side + ") and wool (" + wool + ") burn, stone does not (" + stone + ")");
        h.assertTrue(com.simplelib.api.shape.ShapeMaterials.igniteOdds(half(Blocks.CRIMSON_SLAB).defaultBlockState()) == 0, "Nether wood does not burn");
        h.succeed();
    }

    static void creativeTabAndSwitch(GameTestHelper h) {
        h.assertValueEqual(SlabRegistry.tabStacks().size(), SimpleSlabs.on() ? SlabRegistry.ITEMS.size() : 0, "tab: every half slab item");
        h.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(SimpleSlabs.id("main")), "own creative tab");
        h.succeed();
    }

    // ===================================================================================== helpers

    static HalfSlabBlock half(Block slab) {
        String path = BuiltInRegistries.BLOCK.getKey(slab).getPath();
        return (HalfSlabBlock) BuiltInRegistries.BLOCK.getValue(SimpleSlabs.id(path.substring(0, path.length() - "_slab".length()) + "_half_slab"));
    }

    static HalfSlabBlock byId(String path) {
        return BuiltInRegistries.BLOCK.getOptional(SimpleSlabs.id(path)).filter(b -> b instanceof HalfSlabBlock).map(b -> (HalfSlabBlock) b).orElse(null);
    }

    static Item item(Block slab) {
        return half(slab).asItem();
    }

    static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, SimpleSlabs.id(path));
    }

    /** A click on face {@code face} of the block at {@code pos} (relative), at the given point inside that block (0..1). */
    static UseOnContext click(GameTestHelper h, ServerPlayer player, ItemStack stack, BlockPos pos, Direction face, double x, double y, double z) {
        BlockPos abs = h.absolutePos(pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(new Vec3(abs.getX() + x, abs.getY() + y, abs.getZ() + z), face, abs, false));
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

    static void clearDrops(GameTestHelper h) {
        h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CELL)).inflate(3.0)).forEach(ItemEntity::discard);
    }
}
