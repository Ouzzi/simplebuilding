package com.simplebuilding.modules.simplemobs.client;

import com.mojang.authlib.GameProfile;
import com.simplebuilding.modules.simplemobs.CloakLogic;
import com.simplebuilding.modules.simplemobs.Disguise;
import com.simplebuilding.modules.simplemobs.mixin.AvatarAccessor;
import com.simplebuilding.modules.simplemobs.mixin.WalkAnimationAccessor;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Cloak of deception: a disguised player is drawn as a stand-in that never joins the level. A mob shape uses the mob's
 * own renderer; a player shape is a {@link RemotePlayer} with the imitated profile (skin from the player list) wearing
 * the gear snapshot taken when the disguise began.
 */
public final class DisguiseLooks {
    private static final Map<Entity, Entity> STAND_INS = new WeakHashMap<>();
    private static final Map<Entity, String> SPECS = new WeakHashMap<>();
    private static final byte ALL_LAYERS = 0x7f;

    private DisguiseLooks() {}

    public static Entity look(Entity entity) {
        if (!(entity instanceof Player player)) return entity;
        String spec = Disguise.spec(player);
        if (spec.isEmpty()) {
            if (STAND_INS.remove(entity) != null) SPECS.remove(entity);
            return entity;
        }
        Entity standIn = STAND_INS.get(entity);
        if (standIn == null || !spec.equals(SPECS.get(entity)) || standIn.level() != entity.level()) {
            standIn = create(player, spec);
            if (standIn == null) return entity;
            STAND_INS.put(entity, standIn);
            SPECS.put(entity, spec);
        }
        follow(player, standIn);
        return standIn;
    }

    private static Entity create(Player player, String spec) {
        if (CloakLogic.isPlayerSpec(spec)) {
            UUID id = CloakLogic.specUuid(spec);
            if (id == null || !(player.level() instanceof ClientLevel level)) return null;
            RemotePlayer standIn = new RemotePlayer(level, new GameProfile(id, CloakLogic.specName(spec)));
            Player original = level.getPlayerByUUID(id);
            var key = AvatarAccessor.simplemobs$customisation();
            standIn.getEntityData().set(key, original != null ? original.getEntityData().get(key) : ALL_LAYERS);
            return standIn;
        }
        EntityType<?> type = Disguise.morphType(player);
        return type == null ? null : type.create(player.level(), EntitySpawnReason.LOAD);
    }

    private static void follow(Player from, Entity standIn) {
        standIn.setId(from.getId());
        standIn.setPos(from.getX(), from.getY(), from.getZ());
        standIn.xo = from.xo;
        standIn.yo = from.yo;
        standIn.zo = from.zo;
        standIn.xOld = from.xOld;
        standIn.yOld = from.yOld;
        standIn.zOld = from.zOld;
        standIn.setYRot(from.getYRot());
        standIn.setXRot(from.getXRot());
        standIn.yRotO = from.yRotO;
        standIn.xRotO = from.xRotO;
        standIn.tickCount = from.tickCount;
        standIn.setOnGround(from.onGround());
        standIn.setInvisible(from.isInvisible());
        standIn.setShiftKeyDown(from.isShiftKeyDown());
        if (standIn instanceof Player) standIn.setPose(from.getPose());
        if (standIn instanceof LivingEntity to) {
            to.yBodyRot = from.yBodyRot;
            to.yBodyRotO = from.yBodyRotO;
            to.yHeadRot = from.yHeadRot;
            to.yHeadRotO = from.yHeadRotO;
            to.hurtTime = from.hurtTime;
            to.deathTime = from.deathTime;
            WalkAnimationAccessor walkFrom = (WalkAnimationAccessor) from.walkAnimation;
            WalkAnimationAccessor walkTo = (WalkAnimationAccessor) to.walkAnimation;
            walkTo.simplemobs$setSpeedOld(walkFrom.simplemobs$speedOld());
            walkTo.simplemobs$setSpeed(walkFrom.simplemobs$speed());
            walkTo.simplemobs$setPosition(walkFrom.simplemobs$position());
            if (to instanceof Player) {
                for (int i = 0; i < Disguise.GEAR_SLOTS.length; i++) {
                    ItemStack want = Disguise.gear(from, i);
                    if (!ItemStack.matches(want, to.getItemBySlot(Disguise.GEAR_SLOTS[i]))) to.setItemSlot(Disguise.GEAR_SLOTS[i], want.copy());
                }
            }
        }
    }
}
