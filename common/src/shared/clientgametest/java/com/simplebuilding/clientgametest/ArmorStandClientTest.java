package com.simplebuilding.clientgametest;

import com.simplebuilding.dummy.TrainingDummy;
import com.simplebuilding.entity.ModEntities;
import com.simplebuilding.version.McVersion;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Armor stands (2026-10-09, docs/ai/PLAN-STAENDER-2026-10-09.md): one picture of a placed vanilla stand (arms), the
 * medium stand (leggings, boots), two small stands (with and without boots: the pegs), a plain training dummy and one
 * named after the local player (skin when the profile resolves, the plain dummy as the fallback otherwise).
 *
 * <p>The picture is documentary - what the stands look like is for a human. Asserted is only that the client has all
 * six entities with the right types, so a missing renderer registration (which crashes or skips the entity) fails here.
 */
public final class ArmorStandClientTest {
    private static final double Z = 18.5;

    private ArmorStandClientTest() {
    }

    public static void inWorld(Script script) {
        if (!McVersion.TRAINING_DUMMY) {
            return;
        }
        TestScene.build(script, "minecraft:stone", "creative");
        String[] name = new String[1];
        script.act("remember the local player's name", client -> name[0] = client.player.getGameProfile().name());
        onServer(script, "put six stands in front of the camera", server -> {
            ServerLevel level = server.overworld();
            ArmorStand vanilla = spawn(level, EntityTypes.ARMOR_STAND, 7.5);
            vanilla.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            vanilla.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            vanilla.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            ArmorStand medium = spawn(level, ModEntities.MEDIUM_ARMOR_STAND, 8.7);
            medium.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
            medium.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
            ArmorStand small = spawn(level, ModEntities.SMALL_ARMOR_STAND, 9.9);
            small.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS));
            spawn(level, ModEntities.SMALL_ARMOR_STAND, 10.9);
            spawn(level, ModEntities.TRAINING_DUMMY, 12.1);
            TrainingDummy named = spawn(level, ModEntities.TRAINING_DUMMY, 13.5);
            named.setCustomName(Component.literal(name[0]));
        });
        script.command("tp @a 10.5 0.0 14.5 0.0 12.0");
        script.awaitPackets();
        script.await("the client sees the six stands", 100, client -> stands(client).length == 6,
                client -> "the client sees " + stands(client).length + " armor stands");
        script.check("the client has the medium, the small and the named dummy",
                client -> java.util.Arrays.stream(stands(client)).anyMatch(s -> s.getType() == ModEntities.MEDIUM_ARMOR_STAND)
                        && java.util.Arrays.stream(stands(client)).filter(s -> s.getType() == ModEntities.SMALL_ARMOR_STAND).count() == 2
                        && java.util.Arrays.stream(stands(client)).anyMatch(s -> s.getCustomName() != null));
        script.idle("let the skin lookup and the stands settle", 60);
        script.shot("armor-stands");
        script.command("kill @e[type=!minecraft:player]", true);
        script.awaitPackets();
    }

    private static ArmorStand[] stands(net.minecraft.client.Minecraft client) {
        return client.level.getEntitiesOfClass(ArmorStand.class, new AABB(5.0, -1.0, 16.0, 16.0, 3.0, 20.0)).toArray(ArmorStand[]::new);
    }

    private static <T extends ArmorStand> T spawn(ServerLevel level, EntityType<T> type, double x) {
        T stand = type.create(level, EntitySpawnReason.COMMAND);
        stand.snapTo(x, 0.0, Z, 180.0F, 0.0F);
        stand.setYBodyRot(180.0F);
        stand.setYHeadRot(180.0F);
        stand.setNoGravity(true);
        level.addFreshEntity(stand);
        return stand;
    }

    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
