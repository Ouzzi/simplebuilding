package com.simplebuilding.tweaks.item;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.mixin.FireBlockInvoker;
import com.simplebuilding.tweaks.network.TweaksNetwork;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.SoulFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Was der Strahl der Amethystlinse auf dem Server an Bloecken bewirkt, solange er auf demselben
 * Block (und derselben Seite) ruht. Jede Wirkung braucht eine Verweildauer; waehrenddessen steigt
 * Rauch auf, am Ende gibt es Partikel und ein Geraeusch und die Ladung sinkt um
 * {@link LaserPointerItem#EFFECT_COST}.
 *
 * <ul>
 *   <li>Eis schmilzt zu Wasser (im Nether verdampft es, wie in Vanilla); Packeis wird zu Eis und
 *       Blaueis zu Packeis - eine Stufe je Durchgang statt Wasser aus einem Block, der in Vanilla
 *       nie schmilzt.</li>
 *   <li>Schneeschichten, Schneebloecke und Pulverschnee schmelzen weg.</li>
 *   <li>Brennbare Bloecke (Zuendwert des Feuers &gt; 0: Wolle, Bretter, Laub, Staemme ...) fangen
 *       nach laengerem Strahlen Feuer auf der angestrahlten Seite - nur, wo Feuer sich nach der
 *       Spielregel {@code fire_spread_radius_around_player} ausbreiten darf.</li>
 *   <li>Seelensand/-erde bekommt oben Seelenfeuer; Lagerfeuer, Seelenlagerfeuer, Kerzen und
 *       Kerzenkuchen werden angezuendet.</li>
 *   <li>Nasse Schwaemme trocknen (die "coole" Zusatzwirkung, wie ein Schwamm im Nether).</li>
 *   <li>TNT wird nach derselben Verweildauer wie beim Anzuenden ({@link #IGNITE_TICKS}) gezuendet
 *       (Besitzer 2026-09-27; vorher nie) - wie mit dem Feuerzeug: der Spieler gilt als Zuender, die
 *       Spielregel {@code tnt_explodes} wird beachtet (aus: nichts passiert, keine Ladung weg), die
 *       Feuerausbreitungs-Regel dagegen nicht, denn es entsteht kein Feuer. Mob-Griefing betrifft
 *       nur Mobs und bleibt aussen vor.</li>
 *   <li>Nie: Netherportale (anders als Feuerzeug - Feuer in einem leeren Portalrahmen wird nicht
 *       gesetzt).</li>
 * </ul>
 *
 * <p>Schutz: der Spieler muss den Block beruehren duerfen ({@code mayInteract}: Spawnschutz,
 * Weltgrenze) und dort bauen ({@code mayUseItemAt}: Abenteuermodus), fuer Feuer auch am Feuerplatz.
 * Das gilt fuer TNT genauso.
 *
 * <p><b>Entfernung</b> (Besitzer 2026-09-27): die Verweildauer waechst maessig mit dem Abstand
 * zwischen Auge und Trefferpunkt, {@link #dwellTicks}: Basis x (1 + 0,096 x max(0, d - 5)^0,773).
 * Bis 5 Bloecke gilt die Basis (Anzuenden 3 s), bei 10 Bloecken etwa 1 s mehr (4 s), bei 200
 * Bloecken rund 20 s. Wirkungen reichen bis zur Reichweite der Linse (Config {@code range}, auf dem
 * Server zusaetzlich auf die Sichtweite begrenzt, damit der Strahl keine Chunks laedt); mehr als
 * {@code range + }{@link TweaksNetwork#LASER_RANGE_SLACK} Bloecke entfernt wirkt nichts.
 *
 * <p><b>Lebewesen</b> (Spieler und Mobs) fangen Feuer ({@value #ENTITY_BURN_SECONDS} s), brauchen dafuer aber
 * doppelt so lange wie ein brennbarer Block im selben Abstand ({@link #beamAtEntity}). Nicht:
 * feuerfeste, unverwundbare, nasse (Wasser oder Regen) Wesen, Spieler im Kreativ- oder
 * Zuschauermodus (unverwundbar), andere Spieler nur mit PvP ({@code Player#canHarmPlayer}: Spielregel
 * {@code pvp}, Server-Einstellung, Team-Freundfeuer). Die Suche nach Lebewesen reicht
 * {@value #ENTITY_RANGE} Bloecke (Kosten der Entity-Suche).
 *
 * <p><b>Klaenge am Trefferpunkt</b> (fuer Spieler in der Naehe hoerbar): ein leises Summen alle
 * {@value #HUM_PERIOD} Ticks, solange der Strahl irgendetwas trifft; waehrend eine Wirkung
 * vorbereitet wird, alle {@value #HEAT_SOUND_PERIOD} Ticks deutlich hoerbar ein Zischen (Schmelzen,
 * Trocknen) bzw. Knistern (alles, was brennt oder zuendet, auch Lebewesen); der Abschlussklang
 * jeder Wirkung bleibt. Vanilla-Klaenge, hoch gestimmt. {@link #setSoundHook} zaehlt fuer Tests mit.
 */
public final class LaserBeam {

    public static final int MELT_TICKS = 40;
    public static final int IGNITE_TICKS = 60;
    public static final int SOUL_FIRE_TICKS = 40;
    public static final int LIGHT_TICKS = 20;
    public static final int DRY_TICKS = 100;

    public enum Effect {
        MELT(MELT_TICKS), IGNITE(IGNITE_TICKS), SOUL_FIRE(SOUL_FIRE_TICKS), LIGHT(LIGHT_TICKS), DRY(DRY_TICKS),
        /** TNT zuenden: dieselbe Verweildauer wie Brennbares anzuenden. */
        PRIME_TNT(IGNITE_TICKS);

        public final int ticks;

        Effect(int ticks) {
            this.ticks = ticks;
        }
    }

    /** Bis zu diesem Abstand (Auge - Trefferpunkt) gilt die Basis-Verweildauer. */
    public static final double NEAR_DISTANCE = 5.0;
    /** Kurve der Verweildauer: Basis x (1 + DWELL_SCALE x max(0, d - NEAR_DISTANCE)^DWELL_EXPONENT). */
    public static final double DWELL_SCALE = 0.096;
    public static final double DWELL_EXPONENT = 0.773;
    /** Lebewesen brauchen so viel laenger als ein Block im selben Abstand. */
    public static final int ENTITY_DWELL_FACTOR = 2;
    public static final int ENTITY_BURN_SECONDS = 4;
    /** So weit sucht der Server Lebewesen im Strahl. */
    public static final int ENTITY_RANGE = 64;
    public static final int HUM_PERIOD = 20;
    public static final int HEAT_SOUND_PERIOD = 8;

    /** Die Klaenge am Trefferpunkt. */
    public enum Sound {
        /** leises Summen, solange der Strahl etwas trifft */
        HUM,
        /** Zischen, waehrend Eis/Schnee schmilzt oder ein Schwamm trocknet */
        SIZZLE,
        /** Knistern, waehrend etwas brennen oder zuenden wird */
        CRACKLE
    }

    private record Dwell(BlockPos pos, Direction face, int ticks, long lastTick) {
    }

    private record EntityDwell(int entityId, int ticks, long lastTick) {
    }

    private static final Map<ServerPlayer, Dwell> DWELLS = new WeakHashMap<>();
    private static final Map<ServerPlayer, EntityDwell> ENTITY_DWELLS = new WeakHashMap<>();
    private static @Nullable BiConsumer<Sound, Vec3> soundHook;

    private LaserBeam() {
    }

    /** Vergisst die Verweildauer eines Spielers (Strahl losgelassen oder woanders hin). */
    public static void reset(ServerPlayer player) {
        DWELLS.remove(player);
        ENTITY_DWELLS.remove(player);
    }

    /** Nur fuer Tests: meldet jeden Klang am Trefferpunkt; null schaltet ab. */
    public static void setSoundHook(@Nullable BiConsumer<Sound, Vec3> hook) {
        soundHook = hook;
    }

    /**
     * Verweildauer einer Wirkung mit Basis {@code base} Ticks im Abstand {@code distance}:
     * {@code base x (1 + 0,096 x max(0, d - 5)^0,773)}, gerundet. Passt auf die Vorgaben des
     * Besitzers: 3 s bis 5 Bloecke, etwa +1 s bis 10 Bloecke, rund 20 s bei 200 Bloecken.
     */
    public static int dwellTicks(int base, double distance) {
        double beyond = Math.max(0.0, distance - NEAR_DISTANCE);
        return (int) Math.round(base * (1.0 + DWELL_SCALE * Math.pow(beyond, DWELL_EXPONENT)));
    }

    /** Abstand vom Auge des Spielers zum Trefferpunkt. */
    public static double distance(ServerPlayer player, Vec3 at) {
        return player.getEyePosition().distanceTo(at);
    }

    /** Weiter als Reichweite + Spielraum wirkt nichts (gleiche Grenze wie beim Weiterleiten des Punkts). */
    private static boolean inRange(ServerPlayer player, Vec3 at) {
        double max = SimpleTweaks.config().laserPointer.range + TweaksNetwork.LASER_RANGE_SLACK;
        return player.getEyePosition().distanceToSqr(at) <= max * max;
    }

    /** Das leise Summen am Trefferpunkt; der Aufrufer sorgt fuer den Takt ({@link #HUM_PERIOD}). */
    public static void hum(ServerPlayer player, Vec3 at) {
        play(player.level(), Sound.HUM, at);
    }

    private static void play(ServerLevel level, Sound sound, Vec3 at) {
        BiConsumer<Sound, Vec3> hook = soundHook;
        if (hook != null) {
            hook.accept(sound, at);
        }
        float jitter = level.getRandom().nextFloat() * 0.2f;
        switch (sound) {
            case HUM -> level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 0.25f, 1.9f + jitter * 0.5f);
            case SIZZLE -> level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 1.7f + jitter);
            case CRACKLE -> {
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.7f, 1.2f + jitter);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.6f, 1.4f + jitter);
            }
        }
    }

    private static Sound heatSound(Effect effect) {
        return effect == Effect.MELT || effect == Effect.DRY ? Sound.SIZZLE : Sound.CRACKLE;
    }

    /**
     * Ein Tick Strahl auf {@code hit}. Zaehlt die Verweildauer hoch und loest die Wirkung aus,
     * sobald sie erreicht ist.
     *
     * @return die ausgeloeste Wirkung, sonst null
     */
    public static @Nullable Effect beamAt(ServerPlayer player, ItemStack stack, BlockHitResult hit) {
        ServerLevel level = player.level();
        BlockPos pos = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockState state = level.getBlockState(pos);
        Vec3 at = hit.getLocation();
        Effect effect = LaserPointerItem.isEmpty(stack) || !inRange(player, at) ? null : effectFor(level, pos, state, face);
        if (effect == null || !allowed(player, level, pos, face, stack, effect)) {
            DWELLS.remove(player);
            return null;
        }
        long now = level.getGameTime();
        Dwell dwell = DWELLS.get(player);
        int ticks = dwell != null && dwell.pos().equals(pos) && dwell.face() == face && now - dwell.lastTick() <= 2
                ? dwell.ticks() + 1 : 1;
        if (ticks < dwellTicks(effect.ticks, distance(player, at))) {
            DWELLS.put(player, new Dwell(pos.immutable(), face, ticks, now));
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.03, 0.03, 0.03, 0.0);
            }
            if (ticks % HEAT_SOUND_PERIOD == 1) {
                play(level, heatSound(effect), at);
            }
            return null;
        }
        DWELLS.remove(player);
        if (!apply(player, level, pos, state, face, at, effect)) {
            return null;
        }
        com.simplebuilding.advancement.ModTriggers.feature(player, com.simplebuilding.advancement.ModTriggers.LENS_BEAM);
        LaserPointerItem.drain(player, stack, LaserPointerItem.EFFECT_COST);
        return effect;
    }

    /**
     * Ein Tick Strahl auf ein Lebewesen: nach {@link #ENTITY_DWELL_FACTOR}-facher Verweildauer eines
     * brennbaren Blocks im selben Abstand brennt es {@value #ENTITY_BURN_SECONDS} s; kostet
     * {@link LaserPointerItem#EFFECT_COST} Ladung.
     *
     * @return true, wenn es in diesem Tick angezuendet wurde
     */
    public static boolean beamAtEntity(ServerPlayer player, ItemStack stack, EntityHitResult hit) {
        ServerLevel level = player.level();
        Entity entity = hit.getEntity();
        Vec3 at = hit.getLocation();
        if (LaserPointerItem.isEmpty(stack) || !inRange(player, at) || !(entity instanceof LivingEntity target)
                || !canIgnite(player, target)) {
            ENTITY_DWELLS.remove(player);
            return false;
        }
        long now = level.getGameTime();
        EntityDwell dwell = ENTITY_DWELLS.get(player);
        int ticks = dwell != null && dwell.entityId() == target.getId() && now - dwell.lastTick() <= 2 ? dwell.ticks() + 1 : 1;
        if (ticks < ENTITY_DWELL_FACTOR * dwellTicks(IGNITE_TICKS, distance(player, at))) {
            ENTITY_DWELLS.put(player, new EntityDwell(target.getId(), ticks, now));
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
            }
            if (ticks % HEAT_SOUND_PERIOD == 1) {
                play(level, Sound.CRACKLE, at);
            }
            return false;
        }
        ENTITY_DWELLS.remove(player);
        target.igniteForSeconds(ENTITY_BURN_SECONDS);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 6, 0.15, 0.2, 0.15, 0.01);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
        LaserPointerItem.drain(player, stack, LaserPointerItem.EFFECT_COST);
        return true;
    }

    /**
     * Ob der Strahl dieses Lebewesen anzuenden darf: nicht feuerfest, nicht unverwundbar, nicht nass
     * (Wasser, Regen); ein Spieler nicht im Kreativ-/Zuschauermodus und nur, wenn der Strahlende ihm
     * schaden darf (PvP-Regel, Server-Einstellung, Team-Freundfeuer).
     */
    public static boolean canIgnite(ServerPlayer player, LivingEntity target) {
        // Der Wasser-Merker des Entities wird erst im naechsten Tick aktualisiert; der Block zaehlt sofort.
        boolean wet = target.isInWaterOrRain() || target.level().getFluidState(target.blockPosition()).is(FluidTags.WATER);
        if (target == player || !target.isAlive() || target.fireImmune() || target.isInvulnerable() || wet) {
            return false;
        }
        if (target instanceof Player other) {
            return !other.isSpectator() && !other.getAbilities().invulnerable && player.canHarmPlayer(other);
        }
        return true;
    }

    /** Welche Wirkung der Strahl auf diesem Block haette (ohne Schutz-/Regelpruefung). */
    public static @Nullable Effect effectFor(ServerLevel level, BlockPos pos, BlockState state, Direction face) {
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            return Effect.MELT;
        }
        if (CampfireBlock.canLight(state) || CandleBlock.canLight(state) || CandleCakeBlock.canLight(state)) {
            return Effect.LIGHT;
        }
        if (state.is(Blocks.WET_SPONGE)) {
            return Effect.DRY;
        }
        if (state.is(Blocks.TNT)) {
            return Effect.PRIME_TNT;
        }
        BlockPos firePos = pos.relative(face);
        if (!level.getBlockState(firePos).isAir()) {
            return null;
        }
        if (face == Direction.UP && SoulFireBlock.canSurviveOnBlock(state)) {
            return Effect.SOUL_FIRE;
        }
        if (isFlammable(state)) {
            return Effect.IGNITE;
        }
        return null;
    }

    /** Brennbar wie fuer das Feuer selbst: Zuendwert aus der Feuer-Tabelle (FireBlock#setFlammable). */
    public static boolean isFlammable(BlockState state) {
        return ((FireBlockInvoker) Blocks.FIRE).simplebuilding$getIgniteOdds(state) > 0;
    }

    private static boolean allowed(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face, ItemStack stack, Effect effect) {
        if (!player.mayInteract(level, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        if (effect == Effect.PRIME_TNT) {
            // Wie TntBlock#prime: mit abgeschaltetem TNT gar nicht erst verweilen und nichts abbuchen.
            return level.getGameRules().get(GameRules.TNT_EXPLODES);
        }
        if (effect == Effect.IGNITE || effect == Effect.SOUL_FIRE) {
            BlockPos firePos = pos.relative(face);
            if (!player.mayInteract(level, firePos) || !player.mayUseItemAt(firePos, face, stack)) {
                return false;
            }
        }
        // Brennbares faengt nur Feuer, wo sich Feuer ausbreiten darf (Spielregel); Seelenfeuer
        // breitet sich nie aus und zuendet wie ein Feuerzeug auch ohne die Regel.
        return effect != Effect.IGNITE || level.canSpreadFireAround(pos.relative(face));
    }

    private static boolean apply(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, Direction face, Vec3 at, Effect effect) {
        switch (effect) {
            case MELT -> {
                BlockState melted = meltedState(level, pos, state);
                if (melted.isAir()) {
                    level.removeBlock(pos, false);
                } else {
                    level.setBlockAndUpdate(pos, melted);
                    if (melted.is(Blocks.WATER)) {
                        level.neighborChanged(pos, Blocks.WATER, null);
                    }
                }
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 4, 0.1, 0.1, 0.1, 0.01);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4f, 1.6f + level.getRandom().nextFloat() * 0.3f);
            }
            case LIGHT -> {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
            case DRY -> {
                level.setBlock(pos, Blocks.SPONGE.defaultBlockState(), Block.UPDATE_ALL);
                level.levelEvent(2009, pos, 0);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                level.playSound(null, pos, SoundEvents.WET_SPONGE_DRIES, SoundSource.BLOCKS, 1.0f, (1.0f + level.getRandom().nextFloat() * 0.2f) * 0.7f);
            }
            case PRIME_TNT -> {
                // Wie TntBlock#prime mit dem Spieler als Zuender (die Methode mit Quelle ist privat).
                if (!level.getGameRules().get(GameRules.TNT_EXPLODES)) {
                    return false;
                }
                PrimedTnt tnt = new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
                level.addFreshEntity(tnt);
                level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.gameEvent(player, GameEvent.PRIME_FUSE, pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
            }
            case IGNITE, SOUL_FIRE -> {
                BlockPos firePos = pos.relative(face);
                // Nie ein Netherportal: BaseFireBlock#onPlace wuerde einen leeren Rahmen sofort fuellen.
                if (PortalShape.findEmptyPortalShape(level, firePos, Direction.Axis.X).isPresent()) {
                    return false;
                }
                BlockState fire = BaseFireBlock.getState(level, firePos);
                if (!level.getBlockState(firePos).isAir() || !fire.canSurvive(level, firePos)
                        || (effect == Effect.SOUL_FIRE) != fire.is(Blocks.SOUL_FIRE)) {
                    return false;
                }
                level.setBlock(firePos, fire, Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
                level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 3, 0.05, 0.05, 0.05, 0.01);
                level.playSound(null, firePos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, level.getRandom().nextFloat() * 0.4f + 0.8f);
            }
        }
        return true;
    }

    /** Eis/Frosteis -> Wasser (verdampft, wo Wasser verdampft), Packeis -> Eis, Blaueis -> Packeis, Schnee -> Luft. */
    public static BlockState meltedState(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) {
            return Blocks.PACKED_ICE.defaultBlockState();
        }
        if (state.is(Blocks.PACKED_ICE)) {
            return Blocks.ICE.defaultBlockState();
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE)) {
            return level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, pos)
                    ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }
}
