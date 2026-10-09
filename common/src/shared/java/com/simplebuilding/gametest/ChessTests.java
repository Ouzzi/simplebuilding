package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.custom.CheckerOctetBlock;
import com.simplebuilding.blocks.custom.ChessPiecesBlock;
import com.simplebuilding.blocks.entity.custom.ChessPiecesBlockEntity;
import com.simplebuilding.chess.ChessColor;
import com.simplebuilding.chess.ChessItems;
import com.simplebuilding.chess.ChessPiece;
import com.simplebuilding.items.custom.CheckerOctetItem;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * Schach (docs/ai/PLAN-SCHACH-2026-10-06.md): Achtelbloecke im Sub-Raster nach Trefferpunkt, Wasser bis 8/8, Figuren auf
 * Vierteln, Tauschen, Aufnehmen, Drehen, Drops und die Steinmetz-/Werkbank-Rezepte. Loader-neutral; ohne
 * {@link McVersion#CHESS} bestehen die Tests ohne Pruefung.
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

    private static int mask(GameTestHelper helper, BlockPos pos) {
        BlockState state = helper.getBlockState(pos);
        return state.is(ModBlocks.CHECKER_OCTET) ? CheckerOctetBlock.mask(state) : -1;
    }

    /** Owner N18: octets (chess and wood) stack to the engine maximum of 99 (asked for 128; saved stacks allow at most 99). */
    public static void octetsStackToTheEngineMaximum(GameTestHelper helper) {
        if (!McVersion.CHESS) {
            helper.succeed();
            return;
        }
        for (ChessColor color : ChessColor.values()) {
            helper.assertValueEqual(new ItemStack(ChessItems.octet(color)).getMaxStackSize(), 99, color.id() + " octet stack size");
        }
        // Die Material-Achtel des Vorschlaghammers (Holz, Queue Nachtrag 24) stapeln genauso.
        for (net.minecraft.world.item.Item octet : com.simplebuilding.items.ModItems.WOOD_OCTETS) {
            helper.assertValueEqual(new ItemStack(octet).getMaxStackSize(), 99, octet + " stack size");
        }
        helper.succeed();
    }

    /**
     * Ein Achtel landet ein Viertel Block vor dem Trefferpunkt: auf einer Blockoberseite in der angeklickten Ecke, auf
     * der Oberseite bzw. Seite eines Achtels daneben (auch in der Nachbarzelle); fremde Farben und belegte Achtel lehnen
     * ab; 8/8 ist ein voller Wuerfel; Schleichen + leere Hand nimmt das angeklickte Achtel heraus.
     */
    public static void octetsFillTheSubGridByHitPoint(GameTestHelper helper) {
        if (!McVersion.CHESS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, false);
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos cell = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        ItemStack lapis = new ItemStack(ChessItems.octet(ChessColor.LAPIS), 10);
        // Oberseite des Steins, Nordwest-Viertel -> unteres Nordwest-Achtel (Index 0)
        helper.assertTrue(use(helper, player, lapis, floor, new Vec3(1.25, 2.0, 1.25), Direction.UP).consumesAction(), "first octet refused");
        helper.assertValueEqual(mask(helper, cell), 0x01, "mask after the first octet");
        helper.assertValueEqual(helper.getBlockState(cell).getValue(CheckerOctetBlock.COLOR), ChessColor.LAPIS, "octet colour");
        helper.assertValueEqual(lapis.getCount(), 9, "octets left");
        // Oberseite des Achtels -> oberes Nordwest-Achtel (Index 2)
        use(helper, player, lapis, cell, new Vec3(1.25, 2.5, 1.25), Direction.UP);
        helper.assertValueEqual(mask(helper, cell), 0x05, "mask after stacking an octet on top");
        // Ostseite des Achtels -> unteres Nordost-Achtel (Index 1)
        use(helper, player, lapis, cell, new Vec3(1.5, 2.25, 1.25), Direction.EAST);
        helper.assertValueEqual(mask(helper, cell), 0x07, "mask after an octet beside it");
        // Ostseite des Nordost-Achtels -> Nachbarzelle, dort unten Nordwest
        use(helper, player, lapis, cell, new Vec3(2.0, 2.25, 1.25), Direction.EAST);
        helper.assertValueEqual(mask(helper, cell.east()), 0x01, "octet in the neighbouring cell");
        // fremde Farbe in derselben Zelle, belegtes Achtel
        ItemStack purpur = new ItemStack(ChessItems.octet(ChessColor.PURPUR), 4);
        helper.assertFalse(use(helper, player, purpur, cell, new Vec3(1.75, 2.5, 1.25), Direction.UP).consumesAction(), "purpur joined a lapis cell");
        helper.assertValueEqual(purpur.getCount(), 4, "purpur octets left");
        helper.assertValueEqual(mask(helper, cell), 0x07, "mask after the refused colour");
        // voll: ganzer Wuerfel
        BlockState full = CheckerOctetBlock.withMask(helper.getBlockState(cell), CheckerOctetBlock.FULL);
        helper.assertTrue(Block.isShapeFullBlock(full.getShape(helper.getLevel(), helper.absolutePos(cell))), "8/8 is not a full cube");
        // Schleichen + leere Hand nimmt das obere Nordwest-Achtel heraus
        ServerPlayer sneaker = player(helper, true);
        sneaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult taken = helper.getBlockState(cell).useWithoutItem(helper.getLevel(), sneaker,
                hit(helper, cell, new Vec3(1.25, 3.0, 1.25), Direction.UP));
        helper.assertTrue(taken.consumesAction(), "sneak + empty hand took nothing: " + taken);
        helper.assertValueEqual(mask(helper, cell), 0x03, "mask after taking the upper octet");
        helper.assertTrue(sneaker.getMainHandItem().is(ChessItems.octet(ChessColor.LAPIS)), "the octet is not in the hand: " + sneaker.getMainHandItem());
        helper.succeed();
    }

    /**
     * In Wasser gesetzt haelt die Zelle Wasser, bis das achte Achtel sie fuellt; voll nimmt sie keins mehr an. Abbauen
     * droppt jedes gesetzte Achtel; die Lichtstufe kommt aus der Farbe (Astralit 5).
     */
    public static void octetsHoldWaterUntilFull(GameTestHelper helper) {
        if (!McVersion.CHESS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, false);
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos cell = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(cell, Blocks.WATER);
        CheckerOctetItem item = (CheckerOctetItem) ChessItems.octet(ChessColor.ASTRALIT);
        helper.assertTrue(use(helper, player, new ItemStack(item, 8), floor, new Vec3(2.25, 2.0, 2.25), Direction.UP).consumesAction(),
                "octet in water refused");
        BlockState state = helper.getBlockState(cell);
        helper.assertTrue(state.getValue(CheckerOctetBlock.WATERLOGGED), "octet in water is not waterlogged");
        helper.assertTrue(state.getFluidState().is(Fluids.WATER), "no water in the cell");
        helper.assertValueEqual(state.getLightEmission(), 5, "astralit octet light");
        for (int index = 1; index < 8; index++) {
            BlockState next = item.placed(helper.getLevel(), helper.absolutePos(cell), index, player);
            helper.assertTrue(next != null, "octet " + index + " refused");
            helper.setBlock(cell, next);
            if (index < 7) {
                helper.assertTrue(next.getValue(CheckerOctetBlock.WATERLOGGED), "water lost at " + (index + 1) + "/8");
            }
        }
        BlockState full = helper.getBlockState(cell);
        helper.assertValueEqual(CheckerOctetBlock.count(full), 8, "octets in the full cell");
        helper.assertFalse(full.getValue(CheckerOctetBlock.WATERLOGGED), "a full cell still holds water");
        helper.assertTrue(full.getFluidState().isEmpty(), "a full cell still has fluid");
        helper.assertFalse(((CheckerOctetBlock) ModBlocks.CHECKER_OCTET).canPlaceLiquid(null, helper.getLevel(), helper.absolutePos(cell), full, Fluids.WATER),
                "a full cell takes water");
        helper.assertTrue(item.placed(helper.getLevel(), helper.absolutePos(cell), 3, player) == null, "a full cell took a ninth octet");
        // Abbauen droppt alle acht
        helper.getLevel().destroyBlock(helper.absolutePos(cell), true, player);
        helper.assertValueEqual(dropped(helper, item), 8, "octets dropped");
        // eine halbe Zelle (3/8) droppt drei
        helper.setBlock(cell, CheckerOctetBlock.withMask(ModBlocks.CHECKER_OCTET.defaultBlockState().setValue(CheckerOctetBlock.COLOR, ChessColor.QUARTZ), 0x13));
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(cell), helper.getLevel(), helper.absolutePos(cell), null);
        helper.assertValueEqual(drops.size() == 1 ? drops.getFirst().getCount() + " " + BuiltInRegistries.ITEM.getKey(drops.getFirst().getItem()).getPath() : drops.toString(),
                "3 quartz_octet", "drops of a 3/8 quartz cell");
        helper.succeed();
    }

    /**
     * Figuren stehen auf Vierteln (vier je Block, ein fuenftes Feld gibt es nicht); Schleichen + Figur auf eine Figur
     * tauscht (die alte in die Hand, bei mehreren ins Inventar); Schleichen + leere Hand nimmt auf, die leere Hand dreht;
     * die letzte aufgenommene Figur raeumt den Block; ohne Boden keine Figur; Abbauen droppt alle Figuren.
     */
    public static void piecesStandOnQuartersAndSwap(GameTestHelper helper) {
        if (!McVersion.CHESS) {
            helper.succeed();
            return;
        }
        ServerPlayer player = player(helper, false);
        BlockPos floor = new BlockPos(1, 1, 1);
        BlockPos cell = floor.above();
        helper.setBlock(floor, ModBlocks.LAPIS_QUARTZ_CHECKER);
        Item pawn = ChessItems.piece(ChessColor.LAPIS, ChessPiece.PAWN, false);
        ItemStack pawns = new ItemStack(pawn, 5);
        double[][] quarters = {{1.25, 1.25}, {1.75, 1.25}, {1.25, 1.75}, {1.75, 1.75}};
        for (int slot = 0; slot < 4; slot++) {
            InteractionResult result = use(helper, player, pawns, floor, new Vec3(quarters[slot][0], 2.0, quarters[slot][1]), Direction.UP);
            helper.assertTrue(result.consumesAction(), "pawn " + slot + " refused: " + result);
        }
        helper.assertTrue(helper.getBlockState(cell).is(ModBlocks.CHESS_PIECES), "no piece cell: " + helper.getBlockState(cell));
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
        helper.assertFalse(ModBlocks.CHESS_PIECES.defaultBlockState().canSurvive(helper.getLevel(), helper.absolutePos(new BlockPos(5, 4, 5))),
                "a piece stands on air");
        // im Wasser wassergefuellt
        BlockPos wetFloor = new BlockPos(4, 1, 1);
        helper.setBlock(wetFloor, Blocks.STONE);
        helper.setBlock(wetFloor.above(), Blocks.WATER);
        use(helper, player, new ItemStack(pawn), wetFloor, new Vec3(4.25, 2.0, 1.25), Direction.UP);
        helper.assertTrue(helper.getBlockState(wetFloor.above()).is(ModBlocks.CHESS_PIECES)
                && helper.getBlockState(wetFloor.above()).getValue(ChessPiecesBlock.WATERLOGGED), "piece in water not waterlogged");
        helper.succeed();
    }

    /**
     * Rezepte: der Steinmetz schneidet aus jedem Schachbrett (Quarz: Quarzblock) 8 Achtel seiner Farbe, aus jedem Achtel
     * jede der zwoelf Figuren seiner Farbe, aus jedem Schachbrett Treppe (1) und Stufe (2); die Werkbank macht aus sechs
     * Schachbrettern vier Treppen und aus drei sechs Stufen.
     */
    public static void chessRecipesCutFromCheckersAndOctets(GameTestHelper helper) {
        if (!McVersion.CHESS) {
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        ContextMap context = SlotDisplayContext.fromLevel(level);
        Set<String> cuts = new TreeSet<>();
        for (RecipeHolder<?> holder : level.getServer().getRecipeManager().getRecipes()) {
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
        for (ChessColor color : ChessColor.values()) {
            Item octet = ChessItems.octet(color);
            expect(cuts, missing, path(color.octetSource().asItem()) + " -> 8 " + path(octet));
            for (boolean flat : new boolean[]{false, true}) {
                for (ChessPiece piece : ChessPiece.values()) {
                    expect(cuts, missing, path(octet) + " -> 1 " + path(ChessItems.piece(color, piece, flat)));
                }
            }
        }
        for (ModBlocks.CheckerShapes shapes : ModBlocks.CHECKER_SHAPES) {
            expect(cuts, missing, path(shapes.checker().asItem()) + " -> 1 " + path(shapes.stairs().asItem()));
            expect(cuts, missing, path(shapes.checker().asItem()) + " -> 2 " + path(shapes.slab().asItem()));
            ItemStack c = new ItemStack(shapes.checker());
            ItemStack e = ItemStack.EMPTY;
            craft(helper, CraftingInput.of(3, 3, List.of(c, e, e, c, c, e, c, c, c)), "4 " + path(shapes.stairs().asItem()), missing);
            craft(helper, CraftingInput.of(3, 1, List.of(c, c, c)), "6 " + path(shapes.slab().asItem()), missing);
        }
        helper.assertValueEqual(ModBlocks.CHECKER_SHAPES.size(), 12, "checkers with stairs and slab");
        helper.assertValueEqual(ChessItems.pieces().size(), 13 * 12, "chess piece items");
        helper.assertTrue(missing.isEmpty(), "missing chess recipes: " + missing);
        helper.succeed();
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
