package com.simplebuilding.modules.simplerespawn;

import com.simplebuilding.modules.simplerespawn.network.DownedListPayload;
import com.simplebuilding.modules.simplerespawn.network.RespawnStatePayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Server side of Simple Respawn: who is downed, the bleed-out timer, reviving and the real (Vanilla) death.
 *
 * <p>The real death always runs {@link ServerPlayer#die} with the original damage source: drops, death message,
 * statistics and the loader death events stay Vanilla, so other mods (keep-inventory, graves, soulbound) see an
 * ordinary death. A logout while downed dies before the player file is saved (mixin on {@code PlayerList#remove}), and
 * the tag {@link #TAG} survives a crash, so neither a logout nor a crash keeps the items.
 */
public final class RespawnServer {
    /** Saved with the player (entity tag): downed when the server stopped without a clean logout. */
    public static final String TAG = "simplerespawn_downed";
    /** The client repeats an entity interaction every 4 ticks while right-click is held; a longer gap ends the revive. */
    public static final int HOLD_GAP = 10;
    static final Identifier SPEED_ID = SimpleRespawn.id("downed_speed");
    static final Identifier JUMP_ID = SimpleRespawn.id("downed_jump");
    public static final Identifier FIRST_AID = SimpleRespawn.id("first_aid");

    private static final Map<UUID, Downed> DOWNED = new ConcurrentHashMap<>();
    private static int ticks;
    private static boolean listDirty;

    /** State of one downed player; the revive fields belong to the helper currently holding right-click. */
    public static final class Downed {
        public int ticksLeft;
        public final int maxTicks;
        public final DamageSource source;
        public UUID reviver;
        public String reviverName = "";
        public int progress;
        int lastPing;
        float reviverHealth;
        /** Locator-bar color the player had before the red mark (restored when the state ends). */
        java.util.Optional<Integer> savedColor = java.util.Optional.empty();

        Downed(int ticks, DamageSource source) {
            this.ticksLeft = ticks;
            this.maxTicks = ticks;
            this.source = source;
        }
    }

    private RespawnServer() {}

    public static boolean isDowned(Player player) {
        return DOWNED.containsKey(player.getUUID());
    }

    public static Downed state(Player player) {
        return DOWNED.get(player.getUUID());
    }

    /** Forgets everything (server stop). */
    public static void reset() {
        DOWNED.clear();
        ticks = 0;
    }

    /** Knock down only when someone could help: alone a downed player could only wait for death. */
    public static boolean qualifies(boolean onlyWithOtherPlayers, int otherPlayers) {
        return !onlyWithOtherPlayers || otherPlayers > 0;
    }

    static int otherPlayers(ServerPlayer player) {
        int n = 0;
        for (ServerPlayer p : player.level().getServer().getPlayerList().getPlayers()) {
            if (p != player && !p.isSpectator()) n++;
        }
        return n;
    }

    /** Ticks of bleed-out time a hit of {@code damage} costs a downed player. */
    public static int ticksLost(float damage, double secondsPerDamage) {
        return (int) Math.ceil(Math.max(0f, damage) * secondsPerDamage * 20.0);
    }

    /**
     * Mixin hook: lethal damage got past the totem check. True keeps the player alive (knocked down, or still down with
     * less time left); false lets Vanilla continue with the death.
     */
    public static boolean interceptDeath(ServerPlayer player, DamageSource source, float damage) {
        Downed downed = DOWNED.get(player.getUUID());
        if (downed != null) {
            downed.ticksLeft -= ticksLost(damage, RespawnConfig.secondsLostPerDamage);
            if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || downed.ticksLeft <= 0) {
                clear(player);
                return false;
            }
            player.setHealth(1f);
            sendState(player, downed);
            return true;
        }
        if (!canKnockDown(player, source)) return false;
        knockDown(player, source);
        return true;
    }

    static boolean canKnockDown(ServerPlayer player, DamageSource source) {
        return SimpleRespawn.on() && player.connection != null && !player.isCreative() && !player.isSpectator()
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && qualifies(RespawnConfig.onlyWithOtherPlayers, otherPlayers(player));
    }

    /** Puts the player into the downed state (also the op command and the tests). */
    public static void knockDown(ServerPlayer player, DamageSource source) {
        Downed downed = new Downed(RespawnConfig.bleedOutSeconds * 20, source);
        DOWNED.put(player.getUUID(), downed);
        player.setHealth(1f);
        player.stopUsingItem();
        player.removeVehicle();
        player.closeContainer();
        player.setSprinting(false);
        player.addTag(TAG);
        applyModifiers(player);
        markLocator(player, downed);
        player.setPose(Pose.SWIMMING);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_BIG_FALL, SoundSource.PLAYERS, 1f, 0.7f);
        announce(player.level().getServer(), Component.translatable("chat.simplerespawn.downed", player.getDisplayName()).withStyle(ChatFormatting.RED));
        listDirty = true;
        syncList(player.level().getServer());
        sendState(player, downed);
    }

    /** Mixin hook ({@code Player#interactOn}): a helper's right-click on a downed body. True = the click was used. */
    public static boolean tryRevive(ServerPlayer helper, ServerPlayer target) {
        Downed downed = DOWNED.get(target.getUUID());
        if (downed == null) return false;
        if (!SimpleRespawn.on() || isDowned(helper) || helper.isSpectator()) return true;
        boolean active = downed.reviver != null && ticks - downed.lastPing <= HOLD_GAP;
        if (active && !downed.reviver.equals(helper.getUUID())) return true;
        if (!active || !downed.reviver.equals(helper.getUUID())) {
            downed.reviver = helper.getUUID();
            downed.reviverName = helper.getGameProfile().name();
            downed.progress = 0;
            downed.reviverHealth = helper.getHealth();
        }
        downed.lastPing = ticks;
        return true;
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        for (Map.Entry<UUID, Downed> entry : new ArrayList<>(DOWNED.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Downed downed = entry.getValue();
            if (player == null || player.isRemoved() || player.isDeadOrDying()) {
                DOWNED.remove(entry.getKey());
                if (player != null) restoreLocator(player, downed);
                listDirty = true;
                continue;
            }
            if (!SimpleRespawn.on() || player.isCreative() || player.isSpectator()) {
                clear(player);
                continue;
            }
            if (player.getHealth() > 1f) player.setHealth(1f);
            player.setSprinting(false);
            ServerPlayer helper = downed.reviver == null ? null : server.getPlayerList().getPlayer(downed.reviver);
            if (downed.reviver != null) {
                if (helperStillHelping(player, helper, downed)) {
                    downed.reviverHealth = helper.getHealth();
                    if (++downed.progress >= RespawnConfig.reviveSeconds * 20) {
                        revive(player, helper);
                        continue;
                    }
                } else {
                    stopHelping(downed, helper);
                    helper = null;
                }
            }
            // Bleeding pauses while someone is helping.
            if (downed.reviver == null && --downed.ticksLeft <= 0) {
                die(player);
                continue;
            }
            if (ticks % 5 == 0) {
                sendState(player, downed);
                if (helper != null) SimpleRespawn.send.accept(helper, new RespawnStatePayload(RespawnStatePayload.REVIVING, downed.ticksLeft,
                        downed.maxTicks, progress(downed), player.getGameProfile().name()));
            }
        }
        if (ticks % 20 == 0) {
            for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
                // Downed when the server went away without a logout (crash): the items must not survive that.
                if (player.entityTags().contains(TAG) && !isDowned(player) && player.isAlive()) die(player, player.damageSources().generic());
            }
        }
        if (listDirty || ticks % 20 == 0) syncList(server);
    }

    private static boolean helperStillHelping(ServerPlayer target, ServerPlayer helper, Downed downed) {
        double range = RespawnConfig.reviveRange;
        return helper != null && !helper.isRemoved() && helper.isAlive() && !isDowned(helper) && !helper.isSpectator()
                && helper.level() == target.level() && ticks - downed.lastPing <= HOLD_GAP
                && helper.distanceToSqr(target) <= range * range && helper.getHealth() >= downed.reviverHealth - 0.01f;
    }

    private static void stopHelping(Downed downed, ServerPlayer helper) {
        downed.reviver = null;
        downed.reviverName = "";
        downed.progress = 0;
        if (helper != null) SimpleRespawn.send.accept(helper, RespawnStatePayload.none());
    }

    static float progress(Downed downed) {
        return Math.min(1f, downed.progress / (float) (RespawnConfig.reviveSeconds * 20));
    }

    /** Back on the feet: configured health (3 hearts), empty food bar, a short resistance. */
    public static void revive(ServerPlayer player, ServerPlayer helper) {
        clear(player);
        player.setHealth(Math.min(player.getMaxHealth(), RespawnConfig.reviveHealth));
        player.getFoodData().setFoodLevel(RespawnConfig.reviveFood);
        player.getFoodData().setSaturation(0f);
        if (RespawnConfig.reviveResistanceSeconds > 0)
            player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, RespawnConfig.reviveResistanceSeconds * 20, 4));
        if (RespawnConfig.reviveWeaknessSeconds > 0)
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, RespawnConfig.reviveWeaknessSeconds * 20, 0));
        ServerLevel level = player.level();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 0.6, player.getZ(), 6, 0.4, 0.2, 0.4, 0.0);
        if (helper != null) {
            SimpleRespawn.send.accept(helper, RespawnStatePayload.none());
            var advancement = level.getServer().getAdvancements().get(FIRST_AID);
            if (advancement != null) helper.getAdvancements().award(advancement, "revived");
            announce(level.getServer(), Component.translatable("chat.simplerespawn.revived", player.getDisplayName(), helper.getDisplayName())
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    /** "Give up" and running out of time: the Vanilla death with the original cause. */
    public static void die(ServerPlayer player) {
        Downed downed = DOWNED.get(player.getUUID());
        die(player, downed == null ? player.damageSources().generic() : downed.source);
    }

    public static void die(ServerPlayer player, DamageSource source) {
        clear(player);
        // The combat log forgets a hit after a few seconds; the death message should still name the original cause.
        player.getCombatTracker().recordDamage(source, 0f);
        player.setHealth(0f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.PLAYERS, 1f, 1f);
        player.die(source);
    }

    /** Mixin hook ({@code PlayerList#remove}, before the player file is written): leaving while downed is death. */
    public static void onLeave(ServerPlayer player) {
        if (isDowned(player)) die(player);
    }

    /** Ends the downed state without healing (revive, death, game mode change). */
    public static void clear(ServerPlayer player) {
        Downed downed = DOWNED.remove(player.getUUID());
        player.removeTag(TAG);
        removeModifiers(player);
        restoreLocator(player, downed);
        listDirty = true;
        if (downed != null && downed.reviver != null) {
            ServerPlayer helper = player.level().getServer().getPlayerList().getPlayer(downed.reviver);
            if (helper != null) SimpleRespawn.send.accept(helper, RespawnStatePayload.none());
        }
        SimpleRespawn.send.accept(player, RespawnStatePayload.none());
        syncList(player.level().getServer());
    }

    /** The downed player shows red in the other players' locator bar (a new connection carries the new color). */
    static void markLocator(ServerPlayer player, Downed downed) {
        var icon = player.waypointIcon();
        downed.savedColor = icon.color;
        icon.color = java.util.Optional.of(SimpleRespawn.DOWNED_COLOR);
        player.level().getWaypointManager().remakeConnections(player);
    }

    /** Back to the color from before; with no record (a crash while downed) the red mark simply goes. */
    static void restoreLocator(ServerPlayer player, Downed downed) {
        var icon = player.waypointIcon();
        if (!icon.color.equals(java.util.Optional.of(SimpleRespawn.DOWNED_COLOR))) return;
        icon.color = downed == null ? java.util.Optional.empty() : downed.savedColor;
        player.level().getWaypointManager().remakeConnections(player);
    }

    static void applyModifiers(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, RespawnConfig.crawlSpeed - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.addOrUpdateTransientModifier(new AttributeModifier(JUMP_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    static void removeModifiers(ServerPlayer player) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jump != null) jump.removeModifier(JUMP_ID);
    }

    static void sendState(ServerPlayer player, Downed downed) {
        SimpleRespawn.send.accept(player, new RespawnStatePayload(RespawnStatePayload.DOWNED, downed.ticksLeft, downed.maxTicks,
                downed.reviver == null ? 0f : progress(downed), downed.reviverName));
    }

    static void syncList(MinecraftServer server) {
        if (server == null) return;
        listDirty = false;
        List<UUID> ids = new ArrayList<>();
        List<Integer> left = new ArrayList<>();
        DOWNED.forEach((id, downed) -> {
            ids.add(id);
            left.add(Math.max(0, downed.ticksLeft));
        });
        for (ServerPlayer p : server.getPlayerList().getPlayers()) SimpleRespawn.send.accept(p, new DownedListPayload(ids, left));
    }

    private static void announce(MinecraftServer server, Component message) {
        if (RespawnConfig.announce && server != null) server.getPlayerList().broadcastSystemMessage(message, false);
    }

    /** Mixin hook: actions of a downed player are refused on the server, whatever the client sends. */
    public static boolean blocksActions(Player player) {
        return player != null && !player.level().isClientSide() && isDowned(player);
    }

    /** Mixin hook ({@code LivingEntity#canBeSeenAsEnemy}): monsters do not target or keep chasing a downed player. */
    public static boolean hiddenFromMobs(Player player) {
        return RespawnConfig.mobsIgnoreDowned && !player.level().isClientSide() && isDowned(player);
    }
}
