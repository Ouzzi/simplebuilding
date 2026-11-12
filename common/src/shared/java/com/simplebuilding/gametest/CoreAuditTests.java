package com.simplebuilding.gametest;

import com.simplebuilding.command.SimplebuildingCommand;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;

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
     * The console and command blocks stay outside (no player), as before.
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
            helper.assertFalse(SimplebuildingCommand.OPERATOR_ONLY.test(server.createCommandSourceStack()), "the console passes OPERATOR_ONLY");
        } finally {
            players.deop(player.nameAndId());
            if (wasOp) {
                players.op(player.nameAndId());
            }
            players.remove(player);
        }
        helper.succeed();
    }
}
