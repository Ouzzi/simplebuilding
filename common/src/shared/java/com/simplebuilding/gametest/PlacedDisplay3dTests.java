package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.custom.PlacedTemplateBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.PlacedPlate;
import com.simplebuilding.util.PlacedSmallParts;
import com.simplebuilding.util.PlacedSolids;
import com.simplebuilding.util.PlacedTemplates;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Item display 3D (owner 2026-10-15, D1-D7): smithing templates and pottery sherds lie as 2 px thick stepped models, all
 * Vanilla discs as round 1 px discs (alone on a spot a real stack), and a hammer switches each of them to the flat plate.
 * Owner condition: no face fights about a plane ("kein Z-Fighting"), also not in piles and stacks - checked on the faces of
 * the shipped model JSON, placed with the very transforms the renderer uses ({@link ModelFaces}). Loader-neutral; without
 * the line's features the tests pass.
 */
public final class PlacedDisplay3dTests {
    private PlacedDisplay3dTests() {
    }

    private static boolean enabled(GameTestHelper helper) {
        if (!McVersion.SMALL_PLACEABLES || ModBlocks.PLACED_SMALL_PARTS == null) {
            helper.succeed();
            return false;
        }
        return true;
    }

    private static ServerPlayer player(GameTestHelper helper, boolean sneak) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(sneak);
        return player;
    }

    private static InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(true);
        BlockPos abs = helper.absolutePos(on);
        Vec3 hit = Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, abs, false)));
    }

    /** A right click with {@code stack} on the block, as the game does it (block first, then the item). */
    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.setShiftKeyDown(false);
        BlockPos abs = helper.absolutePos(rel);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atBottomCenterOf(abs).add(0.0, 0.05, 0.0), Direction.UP, abs, false));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static double extent(VoxelShape shape, Direction.Axis axis) {
        return shape.max(axis) - shape.min(axis);
    }

    // =====================================================================================
    // Which items, which models
    // =====================================================================================

    /**
     * All 23 Vanilla sherds and all 22 Vanilla discs are placeable and have a model (item definition, model, and for discs the
     * top view built at build time); the sherds of other mods in the Vanilla tag are placeable as flat plates. Every disc and
     * sherd gets a 3D kind, trims the SLAB kind. The Vanilla tags in the data match the lists.
     */
    public static void everySherdAndVanillaDiscIsPlaceableWithAModel(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            int sherds = 0;
            int discs = 0;
            for (Item item : BuiltInRegistries.ITEM) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                ItemStack stack = new ItemStack(item);
                boolean sherd = stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                        Identifier.withDefaultNamespace("decorated_pot_sherds")));
                if (sherd) {
                    helper.assertTrue(PlacedTemplates.isPlaceableSmall(stack), id + " (a sherd) is not placeable");
                }
                boolean vanillaDisc = id.getNamespace().equals("minecraft") && id.getPath().startsWith("music_disc_");
                if (vanillaDisc) {
                    helper.assertTrue(PlacedTemplates.isPlaceableSmall(stack), id + " (a disc) is not placeable");
                }
                if (id.getNamespace().equals("minecraft") && (sherd && id.getPath().endsWith("_pottery_sherd") || vanillaDisc)) {
                    boolean isDisc = vanillaDisc;
                    helper.assertTrue(PlacedSolids.shape(stack) == (isDisc ? PlacedSolids.Shape.DISC : PlacedSolids.Shape.SLAB), id + " has no 3D model kind");
                    helper.assertTrue(PlacedSmallParts.kind(stack) == (isDisc ? PlacedSmallParts.Kind.DISC : PlacedSmallParts.Kind.SLAB), id + " is no 3D part");
                    helper.assertTrue(PlacedSmallParts.kind(stack).switchable(), id + " cannot be switched to flat");
                    helper.assertTrue(PlacedSmallParts.kind(stack, true) == PlacedSmallParts.Kind.PLATE, id + " is not a plate when switched");
                    resources(helper, "simplebuilding", id.getPath(), isDisc);
                    if (isDisc) {
                        discs++;
                    } else {
                        sherds++;
                    }
                }
            }
            helper.assertValueEqual(sherds, 23, "Vanilla sherds with a model");
            helper.assertValueEqual(discs, 22, "Vanilla discs with a model");
            // the mod's own discs (all sides and tracks)
            for (String song : List.of("voidline", "driftwood", "daybreak", "brimstone")) {
                for (String track : List.of("", "_b_side", "_track_3", "_track_4")) {
                    Item mod = item("simplebuilding:music_disc_" + song + track);
                    if (mod == Items.AIR) {
                        continue;     // tracks 3 and 4 exist only once the owner imported music for them
                    }
                    helper.assertTrue(PlacedSolids.shape(mod) == PlacedSolids.Shape.DISC, song + track + " has no disc model");
                    resources(helper, "simplebuilding", "music_disc_" + song + track, true);
                }
            }
            // trims and the mod's upgrade templates are slabs; a flat plate stays for everything else
            for (Item slab : List.of(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.ENDERITE_UPGRADE_TEMPLATE, ModItems.BASIC_UPGRADE_TEMPLATE)) {
                helper.assertTrue(PlacedSolids.shape(slab) == PlacedSolids.Shape.SLAB, slab + " has no slab model");
                resources(helper, "simplebuilding", path(slab), false);
            }
            helper.assertTrue(PlacedSolids.shape(Items.PAPER) == null && PlacedSolids.shape(Items.FLINT) == null && PlacedSolids.shape(Items.LAPIS_LAZULI) == null
                    && PlacedSolids.shape(Items.AMETHYST_SHARD) == null && PlacedSolids.shape(Items.GOLD_NUGGET) == null, "another item got a 3D model");
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    private static void resources(GameTestHelper helper, String ns, String path, boolean disc) {
        helper.assertTrue(ModelFaces.read(ns, "items/placed_" + path + ".json") != null, "missing item definition placed_" + path);
        var model = ModelFaces.read(ns, "models/block/placed_" + path + ".json");
        helper.assertTrue(model != null, "missing model placed_" + path);
        helper.assertTrue(ModelFaces.elements(ns + ":block/placed_" + path) != null, "placed_" + path + " has no parent with cuboids");
        if (disc) {
            helper.assertTrue(PlacedPlate.class.getResource("/assets/" + ns + "/textures/block/placed_disc_" + path + ".png") != null,
                    "missing top view texture placed_disc_" + path + ".png");
        }
    }

    // =====================================================================================
    // Placing, shape, hammer
    // =====================================================================================

    /**
     * Each art placed on the floor (sneak + right click): the 2 px slab is 2 px thick, the disc 1 px, all in every attachment and
     * facing; a hammer right click switches it to the flat plate (about 1.4 px, one durability) and back; a pickaxe does not; the
     * flat state is kept in the block state, the stored stack stays the item.
     */
    public static void eachArtLiesIn3dAndTheHammerSwitchesIt(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerLevel level = helper.getLevel();
            ServerPlayer player = player(helper, true);
            helper.runBeforeTestEnd(() -> level.getServer().getPlayerList().remove(player));
            List<Item> arts = List.of(Items.ANGLER_POTTERY_SHERD, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, ModItems.ENDERITE_UPGRADE_TEMPLATE,
                    Items.MUSIC_DISC_13, Items.MUSIC_DISC_11, item("simplebuilding:music_disc_voidline"));
            int x = 1;
            for (Item art : arts) {
                boolean disc = PlacedSolids.shape(art) == PlacedSolids.Shape.DISC;
                boolean template = PlacedTemplates.isPlaceableTemplate(new ItemStack(art));   // upgrade templates: own block; sherds, discs: a pile of one
                double thick = (disc ? 1.0 : 2.0) / 16.0;
                BlockPos floor = new BlockPos(x, 1, 1);
                x += 2;
                helper.setBlock(floor, Blocks.STONE);
                helper.assertTrue(place(helper, player, new ItemStack(art), floor).consumesAction(), art + " was not laid down");
                BlockPos rel = floor.above();
                BlockPos abs = helper.absolutePos(rel);
                var flatProperty = template ? PlacedTemplateBlock.FLAT : PlacedSmallPartsBlock.FLAT;
                helper.assertTrue(level.getBlockState(abs).is(template ? ModBlocks.PLACED_SMITHING_TEMPLATE : ModBlocks.PLACED_SMALL_PARTS),
                        art + " was placed as " + level.getBlockState(abs));
                ItemStack stored = template ? PlacedTemplates.templateAt(level, abs) : ((PlacedSmallPartsBlockEntity) level.getBlockEntity(abs)).parts().get(0);
                helper.assertTrue(stored.is(art), art + " is not stored but " + stored);
                helper.assertFalse(level.getBlockState(abs).getValue(flatProperty), art + " lies flat from the start");
                VoxelShape solid = level.getBlockState(abs).getShape(level, abs);
                helper.assertTrue(Math.abs(extent(solid, Direction.Axis.Y) - thick) < 0.004, art + " is " + extent(solid, Direction.Axis.Y) * 16 + " px thick");

                ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
                click(helper, player, rel, pickaxe);
                helper.assertFalse(level.getBlockState(abs).getValue(flatProperty), art + ": a pickaxe switched it");
                ItemStack hammer = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
                click(helper, player, rel, hammer);
                helper.assertTrue(level.getBlockState(abs).getValue(flatProperty), art + ": 3D -> flat did not switch");
                helper.assertValueEqual(hammer.getDamageValue(), 1, art + " hammer damage");
                VoxelShape flat = level.getBlockState(abs).getShape(level, abs);
                helper.assertTrue(extent(flat, Direction.Axis.Y) < 1.6 / 16.0 && extent(flat, Direction.Axis.Y) > 1.0 / 16.0, art + " flat is " + extent(flat, Direction.Axis.Y) * 16 + " px thick");
                stored = template ? PlacedTemplates.templateAt(level, abs) : ((PlacedSmallPartsBlockEntity) level.getBlockEntity(abs)).parts().get(0);
                helper.assertTrue(stored.is(art), art + " lost its item while switched");
                click(helper, player, rel, hammer);
                helper.assertFalse(level.getBlockState(abs).getValue(flatProperty), art + ": flat -> 3D did not switch back");
                helper.assertTrue(Math.abs(extent(level.getBlockState(abs).getShape(level, abs), Direction.Axis.Y) - extent(solid, Direction.Axis.Y)) < 1.0E-6, art + " shape did not come back");

                // The template block lies on the floor, wall or ceiling: the thickness along the normal is the same everywhere.
                if (template) {
                    for (AttachFace face : AttachFace.values()) {
                        for (Direction facing : Direction.Plane.HORIZONTAL) {
                            VoxelShape s = PlacedPlate.shape(art, face, facing, false);
                            Direction.Axis axis = face == AttachFace.WALL ? facing.getAxis() : Direction.Axis.Y;
                            helper.assertTrue(!s.isEmpty() && Math.abs(extent(s, axis) - thick) < 0.004, art + " " + face + " " + facing + " is " + extent(s, axis) * 16 + " px thick");
                        }
                    }
                }
            }
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    /**
     * Discs alone on a spot make a real stack (D5): up to four, each exactly 1 px higher than the one below; with another kind of part
     * (or switched flat) they sit side by side in their own cells and the pile is no stack; slabs lie side by side too.
     */
    public static void discsStackForRealAndMixedPilesStaySeparate(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        var features = com.simplebuilding.config.ServerTuning.get().features;
        boolean vanilla = features.placeVanillaItems;
        String disabled = features.placeDisabledItems;
        try {
            features.placeVanillaItems = true;
            features.placeDisabledItems = "";
            ServerPlayer player = player(helper, true);
            BlockPos floor = new BlockPos(1, 1, 1);
            helper.setBlock(floor, Blocks.STONE);
            ItemStack discs = new ItemStack(Items.MUSIC_DISC_CAT, 4);
            helper.assertTrue(place(helper, player, discs, floor).consumesAction(), "the first disc was not laid down");
            for (int n = 2; n <= 4; n++) {
                helper.assertTrue(place(helper, player, discs, floor.above()).consumesAction(), "disc " + n + " was refused");
                PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(floor.above()));
                helper.assertValueEqual(pile.parts().size(), n, "discs on the pile");
                helper.assertTrue(PlacedSmallParts.ingotStack(pile.parts()), n + " discs do not stack");
                helper.assertTrue(Math.abs(pile.shape().max(Direction.Axis.Y) - n / 16.0) < 1.0E-4, n + " discs are " + pile.shape().max(Direction.Axis.Y) * 16 + " px high");
            }
            for (int i = 0; i < 4; i++) {
                helper.assertTrue(PlacedSmallParts.kind(new ItemStack(Items.MUSIC_DISC_CAT)) == PlacedSmallParts.Kind.DISC, "a disc is no DISC");
            }
            // disc + slab + ingot: no stack, three cells, nothing overlaps
            BlockPos mixed = new BlockPos(3, 1, 1);
            helper.setBlock(mixed, Blocks.STONE);
            List<ItemStack> parts = List.of(new ItemStack(Items.MUSIC_DISC_PIGSTEP), new ItemStack(Items.ANGLER_POTTERY_SHERD), new ItemStack(Items.IRON_INGOT));
            helper.setBlock(mixed.above(), ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
            PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(mixed.above()));
            pile.setParts(parts);
            helper.assertFalse(PlacedSmallParts.ingotStack(pile.parts()), "disc, sherd and ingot stack");
            helper.assertTrue(pile.shape().max(Direction.Axis.Y) < 4.0 / 16.0, "the mixed pile is " + pile.shape().max(Direction.Axis.Y) * 16 + " px high");
            // a disc and an ingot do not stack together (different families), a sherd on a disc stays beside it
            helper.assertFalse(PlacedSmallParts.ingotStack(List.of(new ItemStack(Items.MUSIC_DISC_CAT), new ItemStack(Items.IRON_INGOT))), "a disc stacks with an ingot");
            helper.assertFalse(PlacedSmallParts.ingotStack(List.of(new ItemStack(Items.MUSIC_DISC_CAT), new ItemStack(Items.MUSIC_DISC_CAT)), true), "flat discs stack");
        } finally {
            features.placeVanillaItems = vanilla;
            features.placeDisabledItems = disabled;
        }
        helper.succeed();
    }

    // =====================================================================================
    // No Z-fighting
    // =====================================================================================

    private static final Map<String, List<ModelFaces.Face>> MODEL_FACES = new HashMap<>();

    /** The faces of the model of {@code item} (as the renderer draws a stand-in stack), in model px. */
    private static List<ModelFaces.Face> modelFaces(Item item) {
        String name = "placed_" + path(item);
        return MODEL_FACES.computeIfAbsent(name, n -> {
            var elements = ModelFaces.elements("simplebuilding:block/" + n);
            if (elements == null) {
                throw new IllegalStateException("no model " + n);
            }
            return ModelFaces.faces(elements, n);
        });
    }

    /** A model point (px, 0..16) to a block point (px) through {@code m}, which expects the model centered on the origin. */
    private static java.util.function.UnaryOperator<double[]> centered(Matrix4f m) {
        return p -> {
            Vector3f v = m.transformPosition((float) (p[0] / 16.0 - 0.5), (float) (p[1] / 16.0 - 0.5), (float) (p[2] / 16.0 - 0.5), new Vector3f());
            return new double[]{v.x * 16.0, v.y * 16.0, v.z * 16.0};
        };
    }

    /**
     * Faces of one part of a pile or stack in block px (the transforms of the renderer, {@link PlacedSmallParts#place}), for every
     * mix of 1..4 parts: no two faces, within one part or across parts, share a plane over an area; no face of a slab or disc lies in
     * the plane of the support. Then the same for a single part in every attachment and facing
     * ({@link PlacedPlate#transform}, {@link PlacedPlate#discPlace}).
     */
    public static void noFacesFightInPartsPilesAndStacks(GameTestHelper helper) {
        if (!enabled(helper)) {
            return;
        }
        List<ItemStack> reps = List.of(new ItemStack(Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE), new ItemStack(Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), new ItemStack(Items.ANGLER_POTTERY_SHERD), new ItemStack(Items.MUSIC_DISC_13),
                new ItemStack(Items.MUSIC_DISC_11), new ItemStack(Items.IRON_INGOT));
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
            for (int count = 1; count <= PlacedSmallParts.MAX_PARTS; count++) {
                int[] pick = new int[count];
                do {
                    List<ItemStack> parts = new ArrayList<>();
                    for (int p : pick) {
                        parts.add(reps.get(p));
                    }
                    boolean stack = PlacedSmallParts.ingotStack(parts, false);
                    List<List<ModelFaces.Face>> placed = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        ItemStack part = parts.get(i);
                        Matrix4f m = new Matrix4f();
                        PlacedSmallParts.place(new PlacedSmallParts.MatrixOps(m), facing, count, i, PlacedSmallParts.kind(part), stack);
                        List<ModelFaces.Face> faces = ModelFaces.map(modelFaces(part.getItem()), centered(m));
                        placed.add(faces);
                        PlacedSmallParts.Kind kind = PlacedSmallParts.kind(part);
                        if (kind == PlacedSmallParts.Kind.SLAB || kind == PlacedSmallParts.Kind.DISC) {
                            for (ModelFaces.Face f : faces) {
                                boolean flatOnSupport = true;
                                for (double[] c : f.corners()) {
                                    flatOnSupport &= Math.abs(c[1]) < ModelFaces.PLANE_TOLERANCE;
                                }
                                if (flatOnSupport) {
                                    problems.add(describe(parts, i, facing) + ": " + f.label() + " lies in the plane of the support");
                                }
                            }
                        }
                    }
                    for (int i = 0; i < count; i++) {
                        for (int j = i; j < count; j++) {
                            if (i == j && count > 1 && parts.get(i).getItem() == parts.get(0).getItem() && i > 0) {
                                continue;     // the same part was checked as part 0
                            }
                            if (parts.get(i).is(Items.IRON_INGOT) && (i == j || parts.get(j).is(Items.IRON_INGOT))) {
                                continue;     // ingots: older models, their own tests; cross-part with slabs and discs below
                            }
                            for (String c : ModelFaces.conflicts(placed.get(i), placed.get(j), i == j)) {
                                problems.add(describe(parts, i, facing) + (i == j ? "" : " x " + j) + ": " + c);
                            }
                        }
                    }
                    checked++;
                } while (next(pick, reps.size()));
            }
        }
        // single parts on every attachment and facing
        for (ItemStack rep : reps) {
            PlacedSolids.Shape shape = PlacedSolids.shape(rep);
            if (shape == null) {
                continue;
            }
            for (AttachFace face : AttachFace.values()) {
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    Matrix4f m;
                    java.util.function.UnaryOperator<double[]> toBlock;
                    if (shape == PlacedSolids.Shape.DISC) {
                        m = new Matrix4f();
                        PlacedPlate.discPlace(new PlacedSmallParts.MatrixOps(m), face, facing);
                        toBlock = centered(m);
                    } else {
                        Matrix4f t = PlacedPlate.transform(face, facing, PlacedSolids.MODEL_THICKNESS, 1.0F);
                        toBlock = p -> {
                            Vector3f v = t.transformPosition((float) (p[0] / 16.0), (float) (p[1] / 16.0), (float) (p[2] / 16.0), new Vector3f());
                            return new double[]{v.x * 16.0, v.y * 16.0, v.z * 16.0};
                        };
                    }
                    List<ModelFaces.Face> faces = ModelFaces.map(modelFaces(rep.getItem()), toBlock);
                    for (String c : ModelFaces.conflicts(faces, faces, true)) {
                        problems.add(path(rep.getItem()) + " " + face + " " + facing + ": " + c);
                    }
                    for (ModelFaces.Face f : faces) {
                        for (double[] c : f.corners()) {
                            helper.assertTrue(c[0] > -0.01 && c[0] < 16.01 && c[1] > -0.01 && c[1] < 16.01 && c[2] > -0.01 && c[2] < 16.01,
                                    path(rep.getItem()) + " " + face + " " + facing + " leaves the block: " + java.util.Arrays.toString(c));
                        }
                    }
                }
            }
        }
        helper.assertTrue(checked > 2000, "too few piles checked: " + checked);
        helper.assertTrue(problems.isEmpty(), problems.size() + " coplanar faces, first: " + problems.subList(0, Math.min(5, problems.size())));
        helper.succeed();
    }

    private static String describe(List<ItemStack> parts, int index, Direction facing) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack p : parts) {
            sb.append(sb.length() == 0 ? "" : "+").append(path(p.getItem()));
        }
        return sb + " (part " + index + ", " + facing + ")";
    }

    private static boolean next(int[] pick, int base) {
        for (int i = pick.length - 1; i >= 0; i--) {
            if (++pick[i] < base) {
                return true;
            }
            pick[i] = 0;
        }
        return false;
    }
}
