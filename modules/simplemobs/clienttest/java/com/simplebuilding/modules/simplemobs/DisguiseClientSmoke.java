package com.simplebuilding.modules.simplemobs;

import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.CameraType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Deceiver stage 2: worn cloak, player as cow, player as another player (skin + armor), disguised Deceiver, mirror. */
public final class DisguiseClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(DisguiseRegistry.CLOAK));
                p.setYRot(0f);
                p.setYHeadRot(0f);
                var level = server.getLevel(Level.OVERWORLD);
                var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
                cow.snapTo(p.getX() + 4, p.getY(), p.getZ() + 4, 0f, 0f);
                cow.setNoAi(true);
                level.addFreshEntity(cow);
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-disguise-cloak");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                var cow = server.getLevel(Level.OVERWORLD).getEntities(EntityTypes.COW, e -> true).getFirst();
                if (!Disguise.morph(p, cow)) throw new AssertionError("no cow disguise");
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-disguise-cow");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                Disguise.apply(p, CloakLogic.playerSpec(UUID.fromString("ec561538-f3fd-461d-aff5-086b22154bce"), "Alex"), new ItemStack[] {
                        new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.GOLDEN_CHESTPLATE), ItemStack.EMPTY, ItemStack.EMPTY,
                        new ItemStack(Items.IRON_SWORD), ItemStack.EMPTY});
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-disguise-player");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                Disguise.revert(p);
                var level = server.getLevel(Level.OVERWORLD);
                var d = MobsRegistry.DECEIVER.create(level, EntitySpawnReason.COMMAND);
                d.snapTo(p.getX() - 2, p.getY(), p.getZ() + 3, 180f, 0f);
                d.setNoAi(true);
                level.addFreshEntity(d);
                d.disguiseAs(EntityTypes.ZOMBIE);
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-disguise-deceiver");
            world.getServer().runOnServer(server -> {
                ServerPlayer p = server.getPlayerList().getPlayers().getFirst();
                p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(DisguiseRegistry.MIRROR));
            });
            context.runOnClient(mc -> mc.options.setCameraType(CameraType.FIRST_PERSON));
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-disguise-mirror");
        }
    }
}
