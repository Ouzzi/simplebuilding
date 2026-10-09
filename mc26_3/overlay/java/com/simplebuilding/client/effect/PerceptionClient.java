package com.simplebuilding.client.effect;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.effect.MirageTable;
import com.simplebuilding.effect.ModEffects;
import com.simplebuilding.mixin.client.WalkAnimationStateAccessor;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/**
 * What the perception effects (queue N20, {@code PerceptionEffect}) change on the client of the affected player
 * (26.3). Called from the client mixins, the same on Fabric, NeoForge and Forge:
 * <ul>
 *   <li>Shivering: {@link #crosshairOffset()} moves the crosshair, level I up to 1 px, level II up to 3 px, as the sum
 *       of two sines (smooth, never a jump);</li>
 *   <li>Mirage / Reverse Mirage: {@link #look(Entity)} hands the renderer a client-only stand-in of the type
 *       {@link MirageTable} picks, with the real mob's position, turn and walk;</li>
 *   <li>Faded: {@link #faded()} and {@link #menuOpen()} decide where the grey post effect {@link #FADED_EFFECT} runs.</li>
 * </ul>
 */
public final class PerceptionClient {
    public static final Identifier FADED_EFFECT = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "faded");
    private static final Map<Entity, Entity> STAND_INS = new WeakHashMap<>();

    private PerceptionClient() {
    }

    private static @Nullable MobEffectInstance own(@Nullable Holder<MobEffect> effect) {
        LocalPlayer player = Minecraft.getInstance().player;
        return effect == null || player == null ? null : player.getEffect(effect);
    }

    /** The crosshair offset in GUI pixels, or null without Shivering. */
    public static float @Nullable [] crosshairOffset() {
        MobEffectInstance shivering = own(ModEffects.SHIVERING);
        if (shivering == null) {
            return null;
        }
        float amplitude = shivering.getAmplifier() > 0 ? 3.0F : 1.0F;
        float t = (Util.getMillis() % 1_000_000L) / 1000.0F;
        float dx = (float) (0.6 * Math.sin(t * 23.0) + 0.4 * Math.sin(t * 37.0 + 1.3));
        float dy = (float) (0.6 * Math.sin(t * 29.0 + 2.1) + 0.4 * Math.sin(t * 41.0 + 0.4));
        return new float[] {amplitude * dx, amplitude * dy};
    }

    public static boolean faded() {
        return own(ModEffects.FADED) != null;
    }

    /**
     * A menu is open (Esc menu, options ...): then only the world turns grey and the menu keeps its colours. Inventories
     * and the chat count as part of the game and turn grey with it.
     */
    public static boolean menuOpen() {
        Screen screen = Minecraft.getInstance().gui.screen();
        return screen != null && screen.isPauseScreen() && !(screen instanceof AbstractContainerScreen<?>);
    }

    /** What to draw for {@code entity}: itself, or under Mirage / Reverse Mirage a stand-in of another type. */
    public static Entity look(Entity entity) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || entity == player || ModEffects.MIRAGE == null) {
            return entity;
        }
        boolean mirage = player.hasEffect(ModEffects.MIRAGE);
        boolean reverse = player.hasEffect(ModEffects.REVERSE_MIRAGE);
        if (!mirage && !reverse) {
            if (!STAND_INS.isEmpty()) STAND_INS.clear();
            return entity;
        }
        EntityType<?> type = MirageTable.disguise(entity, mirage, reverse);
        if (type == null) {
            return entity;
        }
        Entity standIn = STAND_INS.get(entity);
        if (standIn == null || standIn.getType() != type || standIn.level() != entity.level()) {
            standIn = type.create(entity.level(), EntitySpawnReason.LOAD);
            if (standIn == null) {
                return entity;
            }
            STAND_INS.put(entity, standIn);
        }
        follow(entity, standIn);
        return standIn;
    }

    private static void follow(Entity real, Entity standIn) {
        standIn.setPos(real.getX(), real.getY(), real.getZ());
        standIn.xo = real.xo;
        standIn.yo = real.yo;
        standIn.zo = real.zo;
        standIn.xOld = real.xOld;
        standIn.yOld = real.yOld;
        standIn.zOld = real.zOld;
        standIn.setYRot(real.getYRot());
        standIn.setXRot(real.getXRot());
        standIn.yRotO = real.yRotO;
        standIn.xRotO = real.xRotO;
        standIn.tickCount = real.tickCount;
        standIn.setOnGround(real.onGround());
        standIn.setInvisible(real.isInvisible());
        if (real instanceof LivingEntity from && standIn instanceof LivingEntity to) {
            to.yBodyRot = from.yBodyRot;
            to.yBodyRotO = from.yBodyRotO;
            to.yHeadRot = from.yHeadRot;
            to.yHeadRotO = from.yHeadRotO;
            to.hurtTime = from.hurtTime;
            to.deathTime = from.deathTime;
            WalkAnimationStateAccessor walkFrom = (WalkAnimationStateAccessor) from.walkAnimation;
            WalkAnimationStateAccessor walkTo = (WalkAnimationStateAccessor) to.walkAnimation;
            walkTo.simplebuilding$setSpeedOld(walkFrom.simplebuilding$speedOld());
            walkTo.simplebuilding$setSpeed(walkFrom.simplebuilding$speed());
            walkTo.simplebuilding$setPosition(walkFrom.simplebuilding$position());
            if (to instanceof Mob mob && from.isBaby() != to.isBaby()) {
                mob.setBaby(from.isBaby());
            }
        }
    }
}
