package com.simplebuilding.forge.gametest;

import com.simplebuilding.gametest.GameTestSpec;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.networking.OctantScrollPayload;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.network.ForgePayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Tests for Forge-only code - the part of the mod no shared catalogue test can reach.
 *
 * <p>Declared in {@code tools/testrunner/run.py} under {@code LOADER_ONLY_TESTS}, so the runner
 * expects them on the Forge target and nowhere else.
 */
public final class ForgeOnlyGameTests {

    private static final List<GameTestSpec> ALL = List.of(
            GameTestSpec.named("forge_network_game_test_serverbound_payloads_are_marked_handled",
                    ForgeOnlyGameTests::serverboundPayloadsAreMarkedHandled).build());

    private ForgeOnlyGameTests() {
    }

    public static void forEach(BiConsumer<String, GameTestSpec> sink) {
        for (GameTestSpec spec : ALL) {
            sink.accept(spec.name(), spec);
        }
    }

    /**
     * Regression for a67aac8: Forge 65 hands a payload that its channel did not mark as handled on
     * to vanilla, which queues it for the main thread and decodes the already read buffer a second
     * time - an IndexOutOfBounds on world join. The payload goes through the same entry point the
     * server's packet listener calls ({@code ForgeHooks.onCustomPayload}); it has to come back
     * handled, and it has to have reached the shared handler (the octant's corner moved one block
     * south, the direction a fresh player faces).
     */
    private static void serverboundPayloadsAreMarkedHandled(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        try {
            ItemStack octant = new ItemStack(ModItems.OCTANT);
            CompoundTag corners = new CompoundTag();
            corners.putIntArray("Pos1", new int[]{9, 1, 19});
            corners.putIntArray("Pos2", new int[]{11, 1, 19});
            octant.set(DataComponents.CUSTOM_DATA, CustomData.of(corners));
            player.getInventory().setSelectedSlot(0);
            player.getInventory().setItem(0, octant);
            player.setYRot(0.0f);
            player.setXRot(0.0f);

            // What arrives from the wire: the payload's bytes in a ForgePayload, exactly as Forge's
            // channel receives it - not the decoded record, which the channel refuses as empty.
            RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
            OctantScrollPayload.CODEC.encode(data, new OctantScrollPayload(1, false, true, false));
            boolean handled = ForgeHooks.onCustomPayload(ForgePayload.create(OctantScrollPayload.ID.id(), data),
                    player.connection.getConnection());
            helper.assertTrue(handled, "the Forge channel did not mark the serverbound OctantScrollPayload as "
                    + "handled - vanilla would decode it a second time");
            helper.assertTrue(data.readableBytes() == 0, "the channel left " + data.readableBytes()
                    + " bytes of the OctantScrollPayload unread");

            int[] pos1 = player.getMainHandItem().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                    .copyTag().getIntArray("Pos1").orElse(new int[0]);
            helper.assertTrue(pos1.length == 3 && pos1[0] == 9 && pos1[1] == 1 && pos1[2] == 20,
                    "the payload was marked handled but did not reach the shared handler: Pos1 is "
                            + java.util.Arrays.toString(pos1) + " instead of [9, 1, 20]");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }
}
