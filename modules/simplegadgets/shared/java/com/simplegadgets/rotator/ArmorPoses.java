package com.simplegadgets.rotator;

import com.simplegadgets.GadgetProtection;
import com.simplegadgets.SimpleGadgets;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TypedEntityData;

/**
 * The Transposer on an armor stand (BA03-5, owner 2026-11-03): right-click = next of 13 poses (Bedrock's list), sneak + right-click =
 * previous. A stand broken by a player keeps its pose and its arms in the dropped item (Vanilla's own {@code ENTITY_DATA} component,
 * which Vanilla's {@code ArmorStandItem} already applies when the item is placed; the lore line names the pose).
 */
public final class ArmorPoses {
    /** One pose: id (lang key {@code tooltip.simplegadgets.pose.<id>}) and the six rotations. */
    public record Pose(String id, ArmorStand.ArmorStandPose rotations) {
        public Component label() {
            return Component.translatable("tooltip.simplegadgets.pose." + id);
        }
    }

    private static Pose pose(String id, float[] head, float[] body, float[] leftArm, float[] rightArm, float[] leftLeg, float[] rightLeg) {
        return new Pose(id, new ArmorStand.ArmorStandPose(rot(head), rot(body), rot(leftArm), rot(rightArm), rot(leftLeg), rot(rightLeg)));
    }

    private static Rotations rot(float[] v) {
        return new Rotations(v[0], v[1], v[2]);
    }

    private static float[] v(float x, float y, float z) {
        return new float[] {x, y, z};
    }

    private static final float[] ZERO = v(0, 0, 0);

    /** Bedrock's thirteen poses in its order; "default" is Vanilla's stance (with arms shown). */
    public static final List<Pose> POSES = List.of(
            new Pose("default", ArmorStand.ArmorStandPose.DEFAULT),
            pose("none", ZERO, ZERO, ZERO, ZERO, ZERO, ZERO),
            pose("solemn", v(0, 0, 0), ZERO, v(-30, 40, 0), v(-30, -40, 0), v(0, 0, 0), v(0, 0, 0)),
            pose("athena", v(-5, 0, 0), ZERO, v(0, 0, -10), v(-70, 0, 20), v(0, 0, -2), v(0, 0, 2)),
            pose("brandish", v(-5, 0, 0), ZERO, v(15, 0, -25), v(-100, 0, 10), v(-15, 0, -2), v(20, 0, 2)),
            pose("honor", v(0, 0, 0), ZERO, v(0, 0, -10), v(-60, -50, 0), v(0, 0, -2), v(0, 0, 2)),
            pose("entertain", v(-10, 0, 0), ZERO, v(0, 0, -90), v(0, 0, 90), v(0, 0, -5), v(0, 0, 5)),
            pose("salute", v(0, 0, 0), ZERO, v(0, 0, -10), v(-110, 25, 20), v(0, 0, -2), v(0, 0, 2)),
            pose("riposte", v(0, 20, 0), v(0, 10, 0), v(0, 0, -30), v(-90, 0, 0), v(10, 0, -2), v(-20, 0, 2)),
            pose("zombie", v(10, 0, 0), ZERO, v(-90, 0, 0), v(-90, 0, 0), v(0, 0, -1), v(0, 0, 1)),
            pose("cancan_a", v(0, 0, 0), ZERO, v(0, 0, -60), v(0, 0, 60), v(-80, 0, -1), v(0, 0, 1)),
            pose("cancan_b", v(0, 0, 0), ZERO, v(0, 0, -60), v(0, 0, 60), v(0, 0, -1), v(-80, 0, 1)),
            pose("hero", v(-10, 0, 0), ZERO, v(10, 0, -40), v(10, 0, 40), v(0, 0, -12), v(0, 0, 12)));

    private ArmorPoses() {}

    /** Index of the stand's current pose in {@link #POSES}, or -1 if it matches none (set by a command or another mod). */
    public static int indexOf(ArmorStand stand) {
        ArmorStand.ArmorStandPose now = stand.getArmorStandPose();
        for (int i = 0; i < POSES.size(); i++) {
            if (same(now, POSES.get(i).rotations())) return i;
        }
        return -1;
    }

    private static boolean same(ArmorStand.ArmorStandPose a, ArmorStand.ArmorStandPose b) {
        return near(a.head(), b.head()) && near(a.body(), b.body()) && near(a.leftArm(), b.leftArm())
                && near(a.rightArm(), b.rightArm()) && near(a.leftLeg(), b.leftLeg()) && near(a.rightLeg(), b.rightLeg());
    }

    private static boolean near(Rotations a, Rotations b) {
        return Math.abs(a.x() - b.x()) < 0.01f && Math.abs(a.y() - b.y()) < 0.01f && Math.abs(a.z() - b.z()) < 0.01f;
    }

    /** Puts the stand into pose {@code index} (wrapped) and shows its arms; returns the pose. */
    public static Pose apply(ArmorStand stand, int index) {
        Pose pose = POSES.get(Math.floorMod(index, POSES.size()));
        stand.setArmorStandPose(pose.rotations());
        stand.setShowArms(true);
        return pose;
    }

    /** Steps the stand to the next (or previous) pose. An unknown pose starts at the first (or last) entry. */
    public static Pose step(ArmorStand stand, boolean previous) {
        int now = indexOf(stand);
        int next = now < 0 ? (previous ? POSES.size() - 1 : 0) : now + (previous ? -1 : 1);
        return apply(stand, next);
    }

    /**
     * The Transposer right-clicked on an armor stand (called by the loader's entity-use event for the main hand). Costs one charge like a
     * turn; an empty Transposer only clicks. Sneaking steps back.
     */
    public static InteractionResult use(Player player, ArmorStand stand, ItemStack rotator) {
        if (player.isSpectator()) return InteractionResult.PASS;
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!GadgetProtection.mayAffectEntity(player, stand)) return InteractionResult.PASS;
        if (RotatorItem.isEmpty(rotator)) {
            player.level().playSound(null, stand.blockPosition(), SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.4f, 1.6f);
            return InteractionResult.FAIL;
        }
        Pose pose = step(stand, player.isShiftKeyDown());
        RotatorItem.drain(player, rotator, RotatorItem.useCost());
        player.level().playSound(null, stand.blockPosition(), SoundEvents.ARMOR_STAND_HIT, SoundSource.BLOCKS, 1.0f, 1.0f);
        player.sendOverlayMessage(pose.label());
        return InteractionResult.SUCCESS;
    }

    /**
     * The item an armor stand drops when a player breaks it: {@code drop} plus, if the stand stands in a pose or shows its arms, the
     * pose in Vanilla's entity data (so placing it again restores it) and a lore line naming the pose. Unchanged for a plain stand.
     */
    public static ItemStack keepPose(ArmorStand stand, ItemStack drop) {
        boolean arms = stand.showArms();
        ArmorStand.ArmorStandPose now = stand.getArmorStandPose();
        boolean posed = !same(now, ArmorStand.ArmorStandPose.DEFAULT);
        if (!SimpleGadgets.on() || !(arms || posed)) return drop;
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("ShowArms", arms);
        if (posed) {
            ArmorStand.ArmorStandPose.CODEC.encodeStart(NbtOps.INSTANCE, now).result().ifPresent(t -> tag.put("Pose", t));
        }
        drop.set(DataComponents.ENTITY_DATA, TypedEntityData.of(EntityTypes.ARMOR_STAND, tag));
        int index = indexOf(stand);
        Component label = index >= 0 ? POSES.get(index).label() : Component.translatable("tooltip.simplegadgets.pose.custom");
        drop.set(DataComponents.LORE, new ItemLore(List.of(label.copy().withStyle(ChatFormatting.GRAY))));
        return drop;
    }
}
