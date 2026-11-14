package com.simplechess.test;

import com.simplechess.ChessColor;
import com.simplechess.block.CheckerOctetBlock;
import com.simplechess.item.CheckerOctetItem;
import com.simplechess.registry.ChessItems;
import com.simplechess.registry.ChessOctets;
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
 * The chess octets (optional integration with Simple Octets, b-chess47): placed in the sub-grid by hit point, mixed with other
 * colours and materials, holding water until full, dropping per octet. Names Simple Octets' classes - the module game test calls
 * it only while {@code simpleoctets} is loaded.
 */
public final class ChessOctetTests {
    private ChessOctetTests() {
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
        return state.is(ChessOctets.cell()) || state.is(com.simpleoctets.registry.OctetRegistry.MIXED_OCTET) ? CheckerOctetBlock.mask(state) : -1;
    }

    /** Owner N18: chess octets stack to the engine maximum of 99 (asked for 128; saved stacks allow at most 99). */
    public static void octetsStackToTheEngineMaximum(GameTestHelper helper) {
        helper.assertTrue(!ChessItems.octets().isEmpty(), "no chess octets with Simple Octets");
        for (Item octet : ChessItems.octets()) {
            helper.assertValueEqual(new ItemStack(octet).getMaxStackSize(), 99, octet + " stack size");
        }
        helper.succeed();
    }
    /**
     * Ein Achtel landet ein Viertel Block vor dem Trefferpunkt: auf einer Blockoberseite in der angeklickten Ecke, auf
     * der Oberseite bzw. Seite eines Achtels daneben (auch in der Nachbarzelle); fremde Farben mischen (N48), belegte
     * Achtel lehnen ab; 8/8 ist ein voller Wuerfel; Schleichen + leere Hand nimmt das angeklickte Achtel heraus.
     */
    public static void octetsFillTheSubGridByHitPoint(GameTestHelper helper) {
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
        // fremde Farbe in derselben Zelle mischt (N48), ein belegtes Achtel lehnt ab
        ItemStack purpur = new ItemStack(ChessItems.octet(ChessColor.PURPUR), 4);
        helper.assertTrue(use(helper, player, purpur, cell, new Vec3(1.75, 2.5, 1.25), Direction.UP).consumesAction(), "purpur did not join the lapis cell");
        helper.assertTrue(helper.getBlockState(cell).is(com.simpleoctets.registry.OctetRegistry.MIXED_OCTET), "lapis + purpur is not a mixed cell");
        helper.assertValueEqual(mask(helper, cell), 0x0F, "mask after the purpur octet");
        helper.assertFalse(use(helper, player, purpur, cell, new Vec3(1.75, 2.5, 1.25), Direction.UP).consumesAction(), "an occupied octet took purpur");
        helper.assertValueEqual(purpur.getCount(), 3, "purpur octets left");
        // voll: ganzer Wuerfel
        BlockState full = CheckerOctetBlock.withMask(helper.getBlockState(cell), CheckerOctetBlock.FULL);
        helper.assertTrue(Block.isShapeFullBlock(full.getShape(helper.getLevel(), helper.absolutePos(cell))), "8/8 is not a full cube");
        // Schleichen + leere Hand nimmt das obere Nordwest-Achtel heraus
        ServerPlayer sneaker = player(helper, true);
        sneaker.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        InteractionResult taken = helper.getBlockState(cell).useWithoutItem(helper.getLevel(), sneaker,
                hit(helper, cell, new Vec3(1.25, 3.0, 1.25), Direction.UP));
        helper.assertTrue(taken.consumesAction(), "sneak + empty hand took nothing: " + taken);
        helper.assertValueEqual(mask(helper, cell), 0x0B, "mask after taking the upper octet");
        helper.assertTrue(sneaker.getMainHandItem().is(ChessItems.octet(ChessColor.LAPIS)), "the octet is not in the hand: " + sneaker.getMainHandItem());
        helper.succeed();
    }
    /**
     * In Wasser gesetzt haelt die Zelle Wasser, bis das achte Achtel sie fuellt; voll nimmt sie keins mehr an. Abbauen
     * droppt jedes gesetzte Achtel; die Lichtstufe kommt aus der Farbe (Astralit 5).
     */
    public static void octetsHoldWaterUntilFull(GameTestHelper helper) {
        ServerPlayer player = player(helper, false);
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos cell = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(cell, Blocks.WATER);
        // Astralit (light 5) is a palette colour: only with SimpleBuilding, otherwise lapis (no light).
        ChessColor lit = ChessColor.ASTRALIT.available() ? ChessColor.ASTRALIT : ChessColor.LAPIS;
        CheckerOctetItem item = (CheckerOctetItem) ChessItems.octet(lit);
        helper.assertTrue(use(helper, player, new ItemStack(item, 8), floor, new Vec3(2.25, 2.0, 2.25), Direction.UP).consumesAction(),
                "octet in water refused");
        BlockState state = helper.getBlockState(cell);
        helper.assertTrue(state.getValue(CheckerOctetBlock.WATERLOGGED), "octet in water is not waterlogged");
        helper.assertTrue(state.getFluidState().is(Fluids.WATER), "no water in the cell");
        helper.assertValueEqual(state.getLightEmission(), lit.light(), lit.id() + " octet light");
        for (int index = 1; index < 8; index++) {
            BlockState[] parts = item.placed(helper.getLevel(), helper.absolutePos(cell), index, player);
            helper.assertTrue(parts != null, "octet " + index + " refused");
            com.simpleoctets.block.OctetCells.set(helper.getLevel(), helper.absolutePos(cell), parts,
                    com.simpleoctets.block.OctetCells.wet(helper.getBlockState(cell)), Block.UPDATE_ALL);
            BlockState next = helper.getBlockState(cell);
            if (index < 7) {
                helper.assertTrue(next.getValue(CheckerOctetBlock.WATERLOGGED), "water lost at " + (index + 1) + "/8");
            }
        }
        BlockState full = helper.getBlockState(cell);
        helper.assertValueEqual(CheckerOctetBlock.count(full), 8, "octets in the full cell");
        helper.assertFalse(full.getValue(CheckerOctetBlock.WATERLOGGED), "a full cell still holds water");
        helper.assertTrue(full.getFluidState().isEmpty(), "a full cell still has fluid");
        helper.assertFalse(((CheckerOctetBlock) ChessOctets.cell()).canPlaceLiquid(null, helper.getLevel(), helper.absolutePos(cell), full, Fluids.WATER),
                "a full cell takes water");
        helper.assertTrue(item.placed(helper.getLevel(), helper.absolutePos(cell), 3, player) == null, "a full cell took a ninth octet");
        // Abbauen droppt alle acht
        helper.getLevel().destroyBlock(helper.absolutePos(cell), true, player);
        helper.assertValueEqual(dropped(helper, item), 8, "octets dropped");
        // eine halbe Zelle (3/8) droppt drei
        helper.setBlock(cell, CheckerOctetBlock.withMask(ChessOctets.cell().defaultBlockState().setValue(CheckerOctetBlock.COLOR, ChessColor.QUARTZ), 0x13));
        List<ItemStack> drops = Block.getDrops(helper.getBlockState(cell), helper.getLevel(), helper.absolutePos(cell), null);
        helper.assertValueEqual(drops.size() == 1 ? drops.getFirst().getCount() + " " + BuiltInRegistries.ITEM.getKey(drops.getFirst().getItem()).getPath() : drops.toString(),
                "3 quartz_octet", "drops of a 3/8 quartz cell");
        helper.succeed();
    }
}
