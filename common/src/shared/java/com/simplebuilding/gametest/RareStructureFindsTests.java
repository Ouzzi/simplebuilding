package com.simplebuilding.gametest;

import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.ModTags;
import com.simplebuilding.util.StorageRefs;
import com.simplebuilding.util.TransformTargets;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Seltene Strukturfunde (Besitzer 2026-10-02): seit Split-Stufe 8b-3 (docs/ai/PLAN-B-S8B3-STORAGE-2026-10-21.md) sind bessere
 * Truhen, seltene Shulker, die Stufen-Schalen und ihre Rezepte Simple Storages ({@code com.simplestorage.test.RareFindsTests}).
 * Hier bleibt das Aufwerten der abgelegten Schale auf SimpleBuildings Haeufchen; die Schalen per Id ({@link StorageRefs}).
 */
public final class RareStructureFindsTests {
    private RareStructureFindsTests() {
    }

    /** Schale ablegen, mit Klumpen rechtsklicken: genau eine Stufe, genau ein Klumpen; falscher Klumpen nichts; Hinweis gleich. */
    public static void placedShellsUpgradeOneTierPerNugget(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.RARE_STRUCTURE_FINDS) {
            com.simplebuilding.Simplebuilding.LOGGER.info("Skipping placedShellsUpgradeOneTierPerNugget: RARE_STRUCTURE_FINDS is disabled on this line");
            helper.succeed();
            return;
        }
        helper.assertTrue(new ItemStack(Items.SHULKER_SHELL).is(ModTags.Items.PLACEABLE_SMALL)
                && new ItemStack(StorageRefs.shulkerShell("netherite")).is(ModTags.Items.PLACEABLE_SMALL), "shells are not placeable");
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel.below(), Blocks.STONE);
        helper.setBlock(rel, ModBlocks.PLACED_SMALL_PARTS.defaultBlockState());
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(helper.absolutePos(rel));
        pile.setParts(List.of(new ItemStack(Items.SHULKER_SHELL), new ItemStack(Items.SHULKER_SHELL)));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.getAbilities().instabuild = false;
        player.setShiftKeyDown(false);
        helper.runBeforeTestEnd(() -> helper.getLevel().getServer().getPlayerList().remove(player));

        // Wrong nugget first: a netherite nugget does nothing to a vanilla shell, and the hand does not tilt.
        ItemStack netherite = new ItemStack(ModItems.NETHERITE_NUGGET, 3);
        click(helper, player, rel, netherite);
        helper.assertValueEqual(netherite.getCount(), 3, "a wrong nugget was used");
        helper.assertTrue(pile.parts().get(0).is(Items.SHULKER_SHELL) && pile.parts().get(1).is(Items.SHULKER_SHELL), "a wrong nugget changed a shell");

        ItemStack iron = new ItemStack(Items.IRON_NUGGET, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, iron);
        helper.assertTrue(TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "no hint for the iron nugget");
        click(helper, player, rel, iron);
        helper.assertValueEqual(iron.getCount(), 2, "iron nuggets after one upgrade");
        helper.assertTrue(pile.parts().get(0).is(Items.SHULKER_SHELL) && pile.parts().get(1).is(StorageRefs.shulkerShell("reinforced")),
                "the last placed shell should be reinforced: " + pile.parts());

        click(helper, player, rel, netherite);
        helper.assertValueEqual(netherite.getCount(), 2, "netherite nuggets after one upgrade");
        helper.assertTrue(pile.parts().get(1).is(StorageRefs.shulkerShell("netherite")), "reinforced -> netherite: " + pile.parts());

        ItemStack enderite = new ItemStack(ModItems.ENDERITE_NUGGET, 1);
        click(helper, player, rel, enderite);
        helper.assertTrue(enderite.isEmpty(), "enderite nugget not used");
        helper.assertTrue(pile.parts().get(1).is(StorageRefs.shulkerShell("enderite")), "netherite -> enderite: " + pile.parts());

        // An enderite shell is the top; another enderite nugget does nothing.
        ItemStack more = new ItemStack(ModItems.ENDERITE_NUGGET, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, more);
        helper.assertTrue(!TransformTargets.canTransformTarget(level, hit(helper, rel), player, InteractionHand.MAIN_HAND), "hint at the top tier");
        click(helper, player, rel, more);
        helper.assertValueEqual(more.getCount(), 1, "enderite nugget used on the top tier");
        helper.assertValueEqual(pile.parts().size(), 2, "pile size");
        helper.succeed();
    }

    private static BlockHitResult hit(GameTestHelper helper, BlockPos rel) {
        BlockPos absolute = helper.absolutePos(rel);
        return new BlockHitResult(Vec3.atBottomCenterOf(absolute).add(0.0, 0.05, 0.0), Direction.UP, absolute, false);
    }

    private static void click(GameTestHelper helper, ServerPlayer player, BlockPos rel, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Vec3 eye = helper.absoluteVec(new Vec3(rel.getX() + 0.5, rel.getY() + 1.0, rel.getZ() + 0.5));
        player.snapTo(eye.x, eye.y, eye.z, 0.0F, 90.0F);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit(helper, rel));
    }
}
