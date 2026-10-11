package com.simplebuilding.clientgametest;

import com.simplebuilding.dummy.SmallArmorStand;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Armor stands (2026-10-09, docs/ai/PLAN-STAENDER-2026-10-09.md): one picture of a placed vanilla stand (arms), the
 * small stand (Nachtrag 29: post with a cross bar) with a chestplate, a helmet, leggings, boots and empty, a plain
 * training dummy and one named after the local player (skin when the profile resolves, the plain dummy otherwise), and
 * a second picture of small stands with horse, wolf and nautilus armor (compare <preview-dir>/refs-stands/2-4).
 *
 * <p>The picture is documentary - what the stands look like is for a human. Asserted is only that the client has all
 * stands with the right types and the body armor, so a missing renderer registration (which crashes or skips the entity) fails here.
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
        onServer(script, "put the stands in front of the camera", server -> {
            ServerLevel level = server.overworld();
            ArmorStand vanilla = spawn(level, EntityTypes.ARMOR_STAND, 3.5);
            vanilla.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            vanilla.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
            vanilla.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            // Kleiner Staender (Nachtrag 29): je genau ein Teil, wie auf den Referenzbildern des Besitzers.
            Item[] shown = {Items.DIAMOND_CHESTPLATE, Items.IRON_HELMET, Items.GOLDEN_LEGGINGS, Items.DIAMOND_BOOTS, Items.AIR};
            for (int i = 0; i < shown.length; i++) {
                SmallArmorStand small = spawn(level, ModEntities.SMALL_ARMOR_STAND, 5.0 + i * 1.2);
                if (shown[i] != Items.AIR) {
                    small.put(new ItemStack(shown[i]));
                }
            }
            spawn(level, ModEntities.TRAINING_DUMMY, 11.3);
            TrainingDummy named = spawn(level, ModEntities.TRAINING_DUMMY, 12.5);
            named.setCustomName(Component.literal(name[0]));
        });
        script.command("tp @a 8.5 0.5 12.0 0.0 15.0");
        script.awaitPackets();
        script.await("the client sees the stands", 100, client -> stands(client).length == 8,
                client -> "the client sees " + stands(client).length + " armor stands");
        script.check("the client has the small stands with their armor and the named dummy",
                client -> java.util.Arrays.stream(stands(client)).filter(s -> s.getType() == ModEntities.SMALL_ARMOR_STAND).count() == 5
                        && java.util.Arrays.stream(stands(client)).anyMatch(s -> s.getCustomName() != null));
        script.idle("let the skin lookup and the stands settle", 60);
        script.shot("armor-stands");
        script.command("kill @e[type=!minecraft:player]", true);
        script.awaitPackets();
        // Zweites Bild: Tier-Ruestungen in Tierform (Referenz Pferderuestung auf dem Pfosten).
        onServer(script, "put the animal armor stands in front of the camera", server -> {
            ServerLevel level = server.overworld();
            Item[] animals = {Items.DIAMOND_HORSE_ARMOR, Items.WOLF_ARMOR, Items.GOLDEN_NAUTILUS_ARMOR};
            for (int i = 0; i < animals.length; i++) {
                spawn(level, ModEntities.SMALL_ARMOR_STAND, 5.0 + i * 3.0).put(new ItemStack(animals[i]));
            }
        });
        script.awaitPackets();
        script.await("the client sees the animal armor stands", 100, client -> java.util.Arrays.stream(stands(client))
                        .filter(s -> !s.getItemBySlot(EquipmentSlot.BODY).isEmpty()).count() == 3,
                client -> "the client sees " + stands(client).length + " armor stands");
        script.idle("let the animal armor stands settle", 20);
        script.shot("armor-stands-animals");
        script.command("kill @e[type=!minecraft:player]", true);
        script.awaitPackets();
    }

    private static ArmorStand[] stands(net.minecraft.client.Minecraft client) {
        return client.level.getEntitiesOfClass(ArmorStand.class, new AABB(2.0, -1.0, 16.0, 16.0, 3.0, 24.0)).toArray(ArmorStand[]::new);
    }

    private static <T extends ArmorStand> T spawn(ServerLevel level, EntityType<T> type, double x) {
        return spawn(level, type, x, Z);
    }

    private static <T extends ArmorStand> T spawn(ServerLevel level, EntityType<T> type, double x, double z) {
        T stand = type.create(level, EntitySpawnReason.COMMAND);
        stand.snapTo(x, 0.0, z, 180.0F, 0.0F);
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
