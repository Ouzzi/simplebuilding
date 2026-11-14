package com.simplechess.test;

import com.simplechess.ChessColor;
import com.simplechess.ChessPiece;
import com.simplechess.SimpleChess;
import com.simplechess.block.ChessPiecesBlock;
import com.simplechess.block.ChessPiecesBlockEntity;
import com.simplechess.registry.ChessItems;
import com.simplechess.registry.ChessRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Simple Chess (docs/ai/PLAN-SCHACH-2026-10-06.md, moved from SimpleBuilding in b-chess47): pieces on quarters, swapping, picking
 * up, turning, drops, the recipes, the palette colours with and without their source, and the id moves. Free of Simple Octets'
 * classes (its octet tests: {@link ChessOctetTests}, only while it is loaded); loader neutral.
 */
public final class ChessTests {
    private ChessTests() {
    }

    private static ServerPlayer player(GameTestHelper helper, boolean sneaking) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 corner = helper.absoluteVec(new Vec3(7.5, 1.0, 7.5));
        player.setPos(corner.x, corner.y, corner.z);
        player.setShiftKeyDown(sneaking);
        return player;
    }

    /** Rechtsklick mit {@code stack} auf {@code on} (relativ), Trefferpunkt {@code hit} relativ zur Testflaeche. */
    private static InteractionResult use(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos on, Vec3 hit, Direction face) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult result = new BlockHitResult(helper.absoluteVec(hit), face, helper.absolutePos(on), false);
        return stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, result));
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos on, Vec3 hit, Direction face) {
        return new BlockHitResult(helper.absoluteVec(hit), face, helper.absolutePos(on), false);
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB box = new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(8.0, 6.0, 8.0);
        int count = 0;
        for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, box, e -> e.getItem().is(item))) {
            count += entity.getItem().getCount();
        }
        return count;
    }

    /**
     * Figuren stehen auf Vierteln (vier je Block, ein fuenftes Feld gibt es nicht); Schleichen + Figur auf eine Figur
     * tauscht (die alte in die Hand, bei mehreren ins Inventar); Schleichen + leere Hand nimmt auf, die leere Hand dreht;
     * die letzte aufgenommene Figur raeumt den Block; ohne Boden keine Figur; Abbauen droppt alle Figuren.
     */
    public static void piecesStandOnQuartersAndSwap(GameTestHelper helper) {
        ServerPlayer player = player(helper, false);
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos cell = floor.above();
        helper.setBlock(floor, ChessRegistry.checker(ChessColor.LAPIS));
        Item pawn = ChessItems.piece(ChessColor.LAPIS, ChessPiece.PAWN, false);
        ItemStack pawns = new ItemStack(pawn, 5);
        double[][] quarters = {{1.25, 1.25}, {1.75, 1.25}, {1.25, 1.75}, {1.75, 1.75}};
        for (int slot = 0; slot < 4; slot++) {
            InteractionResult result = use(helper, player, pawns, floor, new Vec3(quarters[slot][0], 2.0, quarters[slot][1]), Direction.UP);
            helper.assertTrue(result.consumesAction(), "pawn " + slot + " refused: " + result);
        }
        helper.assertTrue(helper.getBlockState(cell).is(ChessRegistry.CHESS_PIECES), "no piece cell: " + helper.getBlockState(cell));
        ChessPiecesBlockEntity be = (ChessPiecesBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(cell));
        helper.assertTrue(be != null, "no piece block entity");
        helper.assertValueEqual(be.count(), 4, "pieces on the block");
        helper.assertValueEqual(pawns.getCount(), 1, "pawns left");
        helper.assertFalse(use(helper, player, pawns, floor, new Vec3(1.25, 2.0, 1.25), Direction.UP).consumesAction(), "a fifth piece stood");
        helper.assertValueEqual(pawns.getCount(), 1, "pawns left after the refused fifth");
        // Tauschen: Stapel von 1 -> die alte Figur in die Hand
        ServerPlayer sneaker = player(helper, true);
        Item king = ChessItems.piece(ChessColor.QUARTZ, ChessPiece.KING, false);
        use(helper, sneaker, new ItemStack(king), cell, new Vec3(1.25, 2.3, 1.25), Direction.UP);
        helper.assertTrue(be.piece(0).is(king), "slot 0 after the swap: " + be.piece(0));
        helper.assertTrue(sneaker.getMainHandItem().is(pawn), "the swapped pawn is not in the hand: " + sneaker.getMainHandItem());
        // Stapel von 2 -> die alte Figur ins Inventar, eine Dame bleibt in der Hand
        Item queen = ChessItems.piece(ChessColor.QUARTZ, ChessPiece.QUEEN, true);
        ItemStack queens = new ItemStack(queen, 2);
        use(helper, sneaker, queens, cell, new Vec3(1.75, 2.1, 1.25), Direction.UP);
        helper.assertTrue(be.piece(1).is(queen), "slot 1 after the swap: " + be.piece(1));
        helper.assertValueEqual(queens.getCount(), 1, "queens left in the hand");
        helper.assertTrue(sneaker.getInventory().countItem(pawn) >= 1, "the second swapped pawn is not in the inventory");
        // ohne Schleichen auf eine belegte Figur: nichts
        helper.assertFalse(use(helper, player, new ItemStack(king), cell, new Vec3(1.25, 2.3, 1.75), Direction.UP).consumesAction(),
                "a piece replaced another without sneaking");
        // leere Hand dreht, Schleichen + leere Hand nimmt auf
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        int before = be.rotation(3);
        helper.getBlockState(cell).useWithoutItem(helper.getLevel(), player, hit(helper, cell, new Vec3(1.75, 2.3, 1.75), Direction.UP));
        helper.assertValueEqual(be.rotation(3), (before + 1) & 3, "rotation after an empty-hand click");
        sneaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(cell).useWithoutItem(helper.getLevel(), sneaker, hit(helper, cell, new Vec3(1.25, 2.3, 1.75), Direction.UP));
        helper.assertTrue(be.piece(2).isEmpty(), "slot 2 still holds " + be.piece(2));
        helper.assertTrue(sneaker.getMainHandItem().is(pawn), "the picked-up pawn is not in the hand: " + sneaker.getMainHandItem());
        // Abbauen droppt die drei uebrigen Figuren
        helper.getLevel().destroyBlock(helper.absolutePos(cell), true, player);
        helper.assertValueEqual(dropped(helper, king) + dropped(helper, queen) + dropped(helper, pawn), 3, "pieces dropped");
        // letzte Figur aufgenommen -> der Block ist weg
        use(helper, player, new ItemStack(pawn), floor, new Vec3(1.25, 2.0, 1.25), Direction.UP);
        sneaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(cell).useWithoutItem(helper.getLevel(), sneaker, hit(helper, cell, new Vec3(1.25, 2.3, 1.25), Direction.UP));
        helper.assertTrue(helper.getBlockState(cell).isAir(), "an empty piece cell stayed: " + helper.getBlockState(cell));
        // ohne tragfaehigen Boden keine Figur
        helper.assertFalse(ChessRegistry.CHESS_PIECES.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(5, 4, 5))),
                "a piece stands on air");
        // im Wasser wassergefuellt
        BlockPos wetFloor = new BlockPos(4, 1, 1);
        helper.setBlock(wetFloor, Blocks.STONE);
        helper.setBlock(wetFloor.above(), Blocks.WATER);
        use(helper, player, new ItemStack(pawn), wetFloor, new Vec3(4.25, 2.0, 1.25), Direction.UP);
        helper.assertTrue(helper.getBlockState(wetFloor.above()).is(ChessRegistry.CHESS_PIECES)
                && helper.getBlockState(wetFloor.above()).getValue(ChessPiecesBlock.WATERLOGGED), "piece in water not waterlogged");
        helper.succeed();
    }
    /**
     * WI26-2: Abbauen durch einen Spieler (echter {@code ServerPlayerGameMode#destroyBlock}) nimmt nur die angeschaute
     * Figur, die uebrigen bleiben mit Drehung stehen; die letzte Figur nimmt die Zelle mit.
     */
    public static void piecesBreakOnlyTheLookedAtPiece(GameTestHelper helper) {
        ServerPlayer player = player(helper, false);
        Vec3 stand = helper.absoluteVec(new Vec3(1.5, 3.0, 0.2));
        player.setPos(stand.x, stand.y, stand.z);
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos cell = floor.above();
        helper.setBlock(floor, ChessRegistry.checker(ChessColor.LAPIS));
        Item pawn = ChessItems.piece(ChessColor.LAPIS, ChessPiece.PAWN, false);
        double[][] quarters = {{1.25, 1.25}, {1.75, 1.25}, {1.25, 1.75}, {1.75, 1.75}};
        for (int slot : new int[] {0, 1, 3}) {
            use(helper, player, new ItemStack(pawn), floor, new Vec3(quarters[slot][0], 2.0, quarters[slot][1]), Direction.UP);
        }
        ChessPiecesBlockEntity be = (ChessPiecesBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(cell));
        helper.assertTrue(be != null && be.count() == 3, "three pieces set");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        helper.getBlockState(cell).useWithoutItem(helper.getLevel(), player, hit(helper, cell, new Vec3(1.75, 2.1, 1.25), Direction.UP));
        int rotation = be.rotation(1);
        helper.assertTrue(rotation != 0, "slot 1 turned");
        // mine slot 3 (looked at)
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(1.75, 2.1, 1.75)));
        helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(cell)), "mining refused");
        ChessPiecesBlockEntity after = (ChessPiecesBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(cell));
        helper.assertTrue(helper.getBlockState(cell).is(ChessRegistry.CHESS_PIECES) && after != null, "the cell was removed: " + helper.getBlockState(cell));
        helper.assertValueEqual(after.count(), 2, "pieces left after mining one");
        helper.assertTrue(after.piece(3).isEmpty() && !after.piece(0).isEmpty() && !after.piece(1).isEmpty(), "wrong piece mined");
        helper.assertValueEqual(after.rotation(1), rotation, "rotation kept");
        helper.assertValueEqual(dropped(helper, pawn), 1, "only the looked-at piece dropped");
        // mine slot 0, then the last one (slot 1) takes the cell
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(1.25, 2.1, 1.25)));
        player.gameMode.destroyBlock(helper.absolutePos(cell));
        ChessPiecesBlockEntity last = (ChessPiecesBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(cell));
        helper.assertTrue(last != null && last.count() == 1 && !last.piece(1).isEmpty(), "one piece should be left");
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(new Vec3(1.75, 2.1, 1.25)));
        player.gameMode.destroyBlock(helper.absolutePos(cell));
        helper.assertTrue(helper.getBlockState(cell).isAir(), "the last piece left a cell: " + helper.getBlockState(cell));
        helper.assertValueEqual(dropped(helper, pawn), 3, "all three pieces dropped one by one");
        helper.succeed();
    }

    /**
     * Recipes: with Simple Octets the shape saw (without Simple Blocks the stonecutter) cuts 8 octets of a colour from its checker
     * (quartz: quartz block) and the stonecutter any piece of the colour from one octet; without Simple Octets the stonecutter
     * cuts 8 pieces straight from the checker. Every checker: stonecutter 1 stairs / 2 slabs, crafting 6 -> 4 stairs, 3 -> 6 slabs.
     */
    public static void chessRecipesCutFromCheckersAndOctets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Set<String> cuts = new TreeSet<>();
        // Octets are exclusive to the Shapecutter of Simple Blocks (docs/ai/PRINZIPIEN.md A4b): its simpleblocks:sawing
        // recipes have no recipe-book display, so their cuts come from the input and the assembled result.
        Set<String> sawCuts = new TreeSet<>();
        boolean saw = SimpleChess.isLoaded("simpleblocks");
        boolean octets = SimpleChess.isLoaded("simpleoctets");
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
            if (holder.value() instanceof net.minecraft.world.item.crafting.SingleItemRecipe single
                    && "simpleblocks:sawing".equals(String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(single.getType())))) {
                single.input().items().forEach(in -> {
                    ItemStack out = single.assemble(new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(in.value())));
                    sawCuts.add(path(in.value()) + " -> " + out.getCount() + " " + path(out.getItem()));
                });
            }
            if (!(holder.value() instanceof StonecutterRecipe)) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                if (display instanceof StonecutterRecipeDisplay cut) {
                    for (ItemStack in : cut.input().resolveForStacks(context)) {
                        for (ItemStack out : cut.result().resolveForStacks(context)) {
                            cuts.add(path(in.getItem()) + " -> " + out.getCount() + " " + path(out.getItem()));
                        }
                    }
                }
            }
        }
        List<String> missing = new ArrayList<>();
        int colours = 0;
        for (ChessColor color : ChessColor.values()) {
            if (!color.available()) continue;
            colours++;
            String source = path(color.octetSource().asItem());
            Item octet = ChessItems.octet(color);
            helper.assertTrue(octets == (octet != null), color.id() + " octet registered " + (octet != null) + " with Simple Octets " + octets);
            if (octet != null) {
                String octetCut = source + " -> 8 " + path(octet);
                expect(saw ? sawCuts : cuts, missing, octetCut);
                if (saw && cuts.contains(octetCut)) missing.add("stonecutter must not cut octets with Simple Blocks: " + octetCut);
            }
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) {
                    String result = path(ChessItems.piece(color, piece, flat));
                    expect(cuts, missing, octet != null ? path(octet) + " -> 1 " + result : source + " -> 8 " + result);
                    if (octet != null && cuts.contains(source + " -> 8 " + result)) missing.add("direct piece cut with Simple Octets: " + result);
                }
            }
        }
        for (ChessRegistry.CheckerShapes shapes : ChessRegistry.shapes()) {
            expect(cuts, missing, path(shapes.checker().asItem()) + " -> 1 " + path(shapes.stairs().asItem()));
            expect(cuts, missing, path(shapes.checker().asItem()) + " -> 2 " + path(shapes.slab().asItem()));
            ItemStack c = new ItemStack(shapes.checker());
            ItemStack e = ItemStack.EMPTY;
            craft(helper, CraftingInput.of(3, 3, List.of(c, e, e, c, c, e, c, c, c)), "4 " + path(shapes.stairs().asItem()), missing);
            craft(helper, CraftingInput.of(3, 1, List.of(c, c, c)), "6 " + path(shapes.slab().asItem()), missing);
        }
        helper.assertValueEqual(ChessRegistry.shapes().size(), colours - 1, "checkers with stairs and slab");
        helper.assertValueEqual(ChessItems.pieces().size(), colours * 12, "chess piece items");
        helper.assertTrue(missing.isEmpty(), "missing chess recipes: " + missing);
        helper.succeed();
    }

    /**
     * The palette colours (Astralit, Nihilit, Ender Quartz, plain and polished) exist exactly while their source
     * ({@link ChessColor#PALETTE_SOURCE}) is loaded; the six Vanilla-made checkers and quartz always. Checkers and shapes are
     * mined with a pickaxe, drop themselves and sit in the Vanilla stairs/slabs tags; Astralit glows with 5.
     */
    public static void paletteColoursFollowTheirSource(GameTestHelper helper) {
        boolean source = SimpleChess.isLoaded(ChessColor.PALETTE_SOURCE);
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        List<String> problems = new ArrayList<>();
        for (ChessColor color : ChessColor.values()) {
            boolean expected = !color.palette() || source;
            if (color.available() != expected) problems.add(color.id() + " available " + color.available());
            boolean registered = BuiltInRegistries.ITEM.containsKey(SimpleChess.id(ChessItems.pieceId(color, ChessPiece.KING, false)));
            if (registered != expected) problems.add(color.id() + " king registered " + registered);
            if (color.checkerId() == null) continue;
            Block checker = ChessRegistry.checker(color);
            if ((checker != null) != expected) problems.add(color.id() + " checker registered " + (checker != null));
            if (checker == null) continue;
            BlockState state = checker.defaultBlockState();
            if (!pickaxe.isCorrectToolForDrops(state)) problems.add(color.checkerId() + " not mined with a pickaxe");
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)), null, null, pickaxe);
            if (drops.size() != 1 || !drops.getFirst().is(checker.asItem())) problems.add(color.checkerId() + " drops " + drops);
            if (state.getLightEmission() != color.light()) problems.add(color.checkerId() + " light " + state.getLightEmission());
        }
        for (ChessRegistry.CheckerShapes shapes : ChessRegistry.shapes()) {
            if (!shapes.stairs().defaultBlockState().is(net.minecraft.tags.BlockTags.STAIRS)) problems.add(shapes.stairs() + " not in #stairs");
            if (!shapes.slab().defaultBlockState().is(net.minecraft.tags.BlockTags.SLABS)) problems.add(shapes.slab() + " not in #slabs");
            if (!pickaxe.isCorrectToolForDrops(shapes.slab().defaultBlockState())) problems.add(shapes.slab() + " not mined with a pickaxe");
        }
        helper.assertTrue(problems.isEmpty(), "palette colours: " + problems);
        helper.succeed();
    }

    /**
     * Migration (SimpleLib id moves): the old {@code simplebuilding:} ids of the checkers, shapes, pieces, octets, the pieces cell
     * (block and block entity) and the recipes resolve to {@code simplechess:} - a saved stack {@code {id:"simplebuilding:..."}}
     * loads as the moved item. Only ids this world registers are checked (palette colours and octets come with their source).
     */
    public static void oldSimplebuildingIdsMigrate(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleChess.movedItems()) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleChess.id(path))) continue;
            Item expected = BuiltInRegistries.ITEM.getValue(SimpleChess.id(path));
            Item old = BuiltInRegistries.ITEM.getValue(SimpleChess.old(path));
            if (old != expected) problems.add("item " + path + " -> " + BuiltInRegistries.ITEM.getKey(old));
        }
        for (String path : SimpleChess.movedBlocks()) {
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleChess.id(path))) continue;
            Block old = BuiltInRegistries.BLOCK.getValue(SimpleChess.old(path));
            if (old != BuiltInRegistries.BLOCK.getValue(SimpleChess.id(path))) problems.add("block " + path + " -> " + BuiltInRegistries.BLOCK.getKey(old));
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleChess.old("chess_pieces")) != ChessRegistry.CHESS_PIECES_BE) {
            problems.add("block entity chess_pieces does not move");
        }
        // A saved stack with the old id (as in a chest of an old world) loads as the moved piece.
        Item king = ChessItems.piece(ChessColor.QUARTZ, ChessPiece.KING, false);
        ItemStack loaded = ItemStack.CODEC.parse(helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),
                parseTag("{id:\"simplebuilding:quartz_chess_king\",count:3}")).result().orElse(ItemStack.EMPTY);
        if (!loaded.is(king) || loaded.getCount() != 3) problems.add("saved old stack loads as " + loaded);
        Identifier recipe = com.simplelib.api.IdMoves.target(net.minecraft.core.registries.Registries.RECIPE, SimpleChess.old("lapis_quartz_checker"));
        if (!SimpleChess.id("lapis_quartz_checker").equals(recipe)) problems.add("recipe lapis_quartz_checker -> " + recipe);
        helper.assertTrue(problems.isEmpty(), "id moves: " + problems);
        helper.succeed();
    }

    private static net.minecraft.nbt.CompoundTag parseTag(String snbt) {
        try {
            return net.minecraft.nbt.TagParser.parseCompoundFully(snbt);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void expect(Set<String> cuts, List<String> missing, String cut) {
        if (!cuts.contains(cut)) {
            missing.add(cut);
        }
    }

    private static void craft(GameTestHelper helper, CraftingInput grid, String expected, List<String> missing) {
        Optional<RecipeHolder<CraftingRecipe>> match = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, helper.getLevel());
        String got = match.map(m -> {
            ItemStack result = m.value().assemble(grid);
            return result.getCount() + " " + path(result.getItem());
        }).orElse("nothing");
        if (!got.equals(expected)) {
            missing.add("crafting " + expected + " (got " + got + ")");
        }
    }

    private static String path(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
