package com.simplebuilding.gametest;

import com.simplebuilding.command.SimplebuildingCommand;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * Fixes of the core audit ({@code AUDIT.md} in the repository root, simplebuilding-8, 12.11.2026): one test per fix, named
 * after the finding it guards.
 */
public final class CoreAuditTests {
    private CoreAuditTests() {
    }

    /**
     * simplebuilding-P2-1: the {@code /simplebuilding ...} commands (server config set/reset, kill boats/carts, world spawn)
     * need an operator player of level 2 like Vanilla's {@code /gamerule} and SimpleLib's config command. Before, any entry
     * in the op list passed ({@code PlayerList#isOp}), also level 1, which Vanilla only lets bypass spawn protection.
     * O8 audit 7 / O10: sources without a player (server console, RCON, functions, command blocks) pass with permission
     * level 2 like every other mod's {@code /<mod> config}; before, the console was locked out of the whole command.
     */
    public static void operatorOnlyNeedsGamemasterLevel(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var server = helper.getLevel().getServer();
        var players = server.getPlayerList();
        boolean wasOp = players.isOp(player.nameAndId());
        try {
            players.deop(player.nameAndId());
            helper.assertFalse(SimplebuildingCommand.OPERATOR_ONLY.test(player.createCommandSourceStack()), "a player without op passes OPERATOR_ONLY");
            players.op(player.nameAndId(), Optional.of(LevelBasedPermissionSet.MODERATOR), Optional.empty());
            helper.assertTrue(players.isOp(player.nameAndId()), "the level-1 op did not reach the op list");
            helper.assertFalse(SimplebuildingCommand.OPERATOR_ONLY.test(player.createCommandSourceStack()),
                    "a level-1 operator (spawn protection bypass only) passes OPERATOR_ONLY and may set the server config");
            players.op(player.nameAndId(), Optional.of(LevelBasedPermissionSet.GAMEMASTER), Optional.empty());
            helper.assertTrue(SimplebuildingCommand.OPERATOR_ONLY.test(player.createCommandSourceStack()), "a level-2 operator is refused");
            helper.assertTrue(SimplebuildingCommand.OPERATOR_ONLY.test(server.createCommandSourceStack()), "the server console is refused (O8 audit 7: it may use /simplebuilding)");
            helper.assertFalse(SimplebuildingCommand.OPERATOR_ONLY.test(server.createCommandSourceStack().withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.MODERATOR)),
                    "a level-1 source without a player passes OPERATOR_ONLY");
        } finally {
            players.deop(player.nameAndId());
            if (wasOp) {
                players.op(player.nameAndId());
            }
            players.remove(player);
        }
        helper.succeed();
    }

    /**
     * simplebuilding-P2-2: a survival player breaks only the looked-at part of a small-parts pile (N37); the other parts are
     * put back by {@code destroy}, and Vanilla's {@code playerDestroy} then called {@code spawnAfterBreak} at the same spot,
     * which found the restored pile and let its eggs hatch - the eggs stayed. Break the pebble next to an egg, put the
     * pebble back, repeat: free chickens forever. Now the eggs that stay never hatch (200 breaks; before about 25 chicks).
     * The player looks into the air, so the target is the last part (the pebble), see {@code PlacedSmallParts#lookedAtPart}.
     */
    public static void brokenPilePartLeavesTheOtherEggsWhole(GameTestHelper helper) {
        if (!com.simplebuilding.version.McVersion.SMALL_PLACEABLES) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var level = helper.getLevel();
        BlockPos floor = new BlockPos(2, 1, 2), spot = floor.above();
        BlockPos abs = helper.absolutePos(spot);
        AABB around = new AABB(abs).inflate(3);
        try {
            player.setGameMode(GameType.SURVIVAL);
            player.getInventory().clearContent();
            player.snapTo(abs.getX() + 2.5, abs.getY(), abs.getZ() + 0.5, 0.0F, -90.0F);
            helper.setBlock(floor, Blocks.STONE);
            int chicks = 0;
            for (int i = 0; i < 200; i++) {
                helper.setBlock(spot, ModBlocks.PLACED_SMALL_PARTS);
                helper.getBlockEntity(spot, PlacedSmallPartsBlockEntity.class).setParts(List.of(new ItemStack(Items.EGG), new ItemStack(ModItems.STONE_PEBBLE)));
                helper.assertTrue(player.gameMode.destroyBlock(abs), "the pebble was not broken (round " + i + ")");
                helper.assertTrue(helper.getBlockState(spot).is(ModBlocks.PLACED_SMALL_PARTS)
                                && helper.getBlockEntity(spot, PlacedSmallPartsBlockEntity.class).parts().size() == 1
                                && helper.getBlockEntity(spot, PlacedSmallPartsBlockEntity.class).parts().get(0).is(Items.EGG),
                        "the egg does not stay alone on the spot (round " + i + ")");
                var hatched = level.getEntitiesOfClass(Chicken.class, around);
                chicks += hatched.size();
                hatched.forEach(c -> c.discard());
            }
            helper.assertValueEqual(chicks, 0, "chicks hatched from eggs that stayed on the pile");
        } finally {
            helper.setBlock(spot, Blocks.AIR);
            level.getEntitiesOfClass(ItemEntity.class, around).forEach(e -> e.discard());
            level.getEntitiesOfClass(Chicken.class, around).forEach(c -> c.discard());
            level.getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
