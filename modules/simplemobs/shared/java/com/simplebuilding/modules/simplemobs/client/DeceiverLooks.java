package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.DeceiverEntity;
import com.simplebuilding.modules.simplemobs.mixin.WalkAnimationAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * Owner N38: a disguised Deceiver is drawn as a stand-in of the mob it turned into. The stand-in never joins the
 * level; it follows the Deceiver's position, rotation, walk and hurt animation every frame.
 */
public final class DeceiverLooks {
    private static final Map<Entity, Entity> STAND_INS = new WeakHashMap<>();

    private DeceiverLooks() {}

    /** Leaving a world (REV2-B): the stand-ins hold the old client level (see {@link DisguiseLooks#clear}). */
    public static void clear() {
        STAND_INS.clear();
    }

    public static Entity look(Entity entity) {
        if (!(entity instanceof DeceiverEntity deceiver)) return entity;
        EntityType<?> type = deceiver.look();
        if (type == null) {
            STAND_INS.remove(entity);
            return entity;
        }
        Entity standIn = STAND_INS.get(entity);
        if (standIn == null || standIn.getType() != type || standIn.level() != entity.level()) {
            standIn = type.create(entity.level(), EntitySpawnReason.LOAD);
            if (standIn == null) return entity;
            STAND_INS.put(entity, standIn);
        }
        follow(deceiver, standIn);
        return standIn;
    }

    private static void follow(LivingEntity from, Entity standIn) {
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
        }
    }
}
