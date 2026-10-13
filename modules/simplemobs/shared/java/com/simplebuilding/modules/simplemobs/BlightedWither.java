package com.simplebuilding.modules.simplemobs;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Blighted Wither (concept N26, owner 13.10.): the Vanilla Wither, only stronger. Summoned like the Wither, but with a
 * Block of Netherite under the foot of the T ({@link BlightedPattern}). 600 health, armor 8, three phases; every special
 * attack is announced by a sound and particles at the spot before it hits:
 * <ul>
 *   <li>Phase 1 (over half health): Vanilla skulls plus a twin volley - both side heads glow, then fire at once.</li>
 *   <li>Phase 2 (half health and below): the Vanilla shield (arrows bounce off), and every 20 s three Wither Skeletons rise
 *       where soul flames burned for 2 s before (never more than six near; they drop nothing).</li>
 *   <li>Phase 3 "Blight" (below 15 %): a nova - a ring of soul smoke marks 8 blocks for 2 s, who is still inside gets
 *       Wither II and is pushed away - and the wilt: plants and natural leaves around it wither (mobGriefing, config).</li>
 * </ul>
 * Drops a Wither Heart besides the Nether Star (one per kill, Looting does not change it).
 */
public class BlightedWither extends WitherBoss {
    private static final Identifier NO_LOOT = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "entities/summoned");

    public enum Attack { VOLLEY, MINIONS, NOVA, WILT }

    private @Nullable Attack pending;
    private int pendingTicks;
    private final List<Vec3> minionSpots = new ArrayList<>();
    private int volleyTimer = WitherLogic.VOLLEY_INTERVAL, minionTimer = WitherLogic.MINION_WARN, novaTimer = WitherLogic.NOVA_WARN, wiltTimer;
    private int lastPhase = 1;

    public BlightedWither(EntityType<? extends WitherBoss> type, Level level) {
        super(type, level);
        this.xpReward = WitherLogic.XP;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return WitherBoss.createAttributes()
                .add(Attributes.MAX_HEALTH, WitherLogic.HEALTH)
                .add(Attributes.ARMOR, WitherLogic.ARMOR);
    }

    public int phase() {
        return WitherLogic.phase(this.getHealth(), this.getMaxHealth());
    }

    public @Nullable Attack pending() {
        return pending;
    }

    public int pendingTicks() {
        return pendingTicks;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        boolean waking = this.getInvulnerableTicks() > 0;
        super.customServerAiStep(level);
        // Vanilla heals 10 every 10 ticks while waking (enough for 300 from a third); twice as much reaches 600.
        if (waking && this.tickCount % 10 == 0) this.heal(10.0F);
        if (!waking) tickAbilities(level);
    }

    /** Phase changes and the announced special attacks; called every server tick once the Wither is awake. */
    public void tickAbilities(ServerLevel level) {
        int phase = phase();
        if (phase > lastPhase) {
            lastPhase = phase;
            this.playSound(SoundEvents.WITHER_SPAWN, 2.0F, phase == 3 ? 0.6F : 0.8F);
            level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 2.0, this.getZ(), 40, 1.5, 1.5, 1.5, 0.05);
            if (phase == 3) wiltTimer = 0;
        }
        volleyTimer--;
        minionTimer--;
        novaTimer--;
        wiltTimer--;
        if (pending != null) {
            pendingTicks--;
            if (pendingTicks % 4 == 0) telegraph(level, pending);
            if (pendingTicks <= 0) {
                Attack attack = pending;
                pending = null;
                execute(level, attack);
            }
            return;
        }
        if (phase == 3 && wiltTimer <= 0) announce(level, Attack.WILT);
        else if (phase == 3 && novaTimer <= 0) announce(level, Attack.NOVA);
        else if (phase >= 2 && minionTimer <= 0) announce(level, Attack.MINIONS);
        else if (volleyTimer <= 0 && this.getTarget() != null) announce(level, Attack.VOLLEY);
    }

    /** Starts the warning of an attack; it hits when the warning ran out. */
    public void announce(ServerLevel level, Attack attack) {
        pending = attack;
        switch (attack) {
            case VOLLEY -> volleyTimer = WitherLogic.VOLLEY_INTERVAL;
            case MINIONS -> minionTimer = WitherLogic.MINION_INTERVAL;
            case NOVA -> novaTimer = WitherLogic.NOVA_INTERVAL;
            case WILT -> wiltTimer = WitherLogic.WILT_INTERVAL;
        }
        pendingTicks = switch (attack) {
            case VOLLEY -> WitherLogic.VOLLEY_WARN;
            case MINIONS -> WitherLogic.MINION_WARN;
            case NOVA -> WitherLogic.NOVA_WARN;
            case WILT -> WitherLogic.WILT_WARN;
        };
        switch (attack) {
            case VOLLEY -> this.playSound(SoundEvents.WITHER_AMBIENT, 1.5F, 1.6F);
            case MINIONS -> {
                minionSpots.clear();
                int n = WitherLogic.minionsToCall(level.getEntitiesOfClass(WitherSkeleton.class, this.getBoundingBox().inflate(32.0)).size());
                for (int i = 0; i < n; i++) {
                    double a = (Math.PI * 2 * i) / Math.max(1, n) + this.random.nextDouble();
                    Vec3 spot = ground(level, this.getX() + Math.cos(a) * 4.0, this.getZ() + Math.sin(a) * 4.0);
                    if (spot != null) minionSpots.add(spot);
                }
                this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 2.0F, 0.6F);
            }
            case NOVA -> this.playSound(SoundEvents.WARDEN_SONIC_CHARGE, 2.0F, 0.7F);
            case WILT -> this.playSound(SoundEvents.WITHER_AMBIENT, 2.0F, 0.5F);
        }
        telegraph(level, attack);
    }

    private void telegraph(ServerLevel level, Attack attack) {
        switch (attack) {
            case VOLLEY -> {
                for (int head = 1; head <= 2; head++) particle(level, ParticleTypes.SOUL_FIRE_FLAME, headX(head), headY(head), headZ(head), 6, 0.2);
            }
            case MINIONS -> {
                for (Vec3 s : minionSpots) particle(level, ParticleTypes.SOUL_FIRE_FLAME, s.x, s.y + 0.2, s.z, 6, 0.3);
            }
            case NOVA -> {
                for (int i = 0; i < 32; i++) {
                    double a = Math.PI * 2 * i / 32.0;
                    double x = this.getX() + Math.cos(a) * WitherLogic.NOVA_RADIUS, z = this.getZ() + Math.sin(a) * WitherLogic.NOVA_RADIUS;
                    particle(level, ParticleTypes.SOUL, x, this.getY() + 1.0, z, 1, 0.05);
                    Vec3 g = ground(level, x, z);
                    if (g != null) particle(level, ParticleTypes.SMOKE, x, g.y + 0.1, z, 1, 0.05);
                }
            }
            case WILT -> particle(level, ParticleTypes.WHITE_ASH, this.getX(), this.getY() + 1.0, this.getZ(), 60, WitherLogic.WILT_RADIUS / 2.0);
        }
    }

    private void execute(ServerLevel level, Attack attack) {
        switch (attack) {
            case VOLLEY -> {
                LivingEntity target = this.getTarget();
                if (target != null && target.isAlive()) volley(level, target);
            }
            case MINIONS -> {
                for (Vec3 s : minionSpots) summonMinion(level, s);
                minionSpots.clear();
            }
            case NOVA -> {
                nova(level);
            }
            case WILT -> {
                wilt(level);
            }
        }
    }

    /** Twin volley: one skull from each side head at the target, at the same time. Returns the skulls fired. */
    public int volley(ServerLevel level, LivingEntity target) {
        if (!this.isSilent()) level.levelEvent(null, 1024, this.blockPosition(), 0);
        int fired = 0;
        for (int head = 1; head <= WitherLogic.VOLLEY_SKULLS; head++) {
            double hx = headX(head), hy = headY(head), hz = headZ(head);
            Vec3 dir = new Vec3(target.getX() - hx, target.getY() + target.getEyeHeight() * 0.5 - hy, target.getZ() - hz);
            WitherSkull skull = new WitherSkull(level, this, dir.normalize());
            skull.setOwner(this);
            skull.setPos(hx, hy, hz);
            if (level.addFreshEntity(skull)) fired++;
        }
        return fired;
    }

    /** Raises one Wither Skeleton at a marked spot (no loot, no drops). */
    public @Nullable Entity summonMinion(ServerLevel level, Vec3 spot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("DeathLootTable", NO_LOOT.toString());
        var input = TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag);
        Entity e = EntityType.create(EntityTypes.WITHER_SKELETON, input, level, EntitySpawnReason.MOB_SUMMONED).orElse(null);
        if (e == null) return null;
        e.snapTo(spot.x, spot.y, spot.z, this.random.nextFloat() * 360f, 0f);
        if (e instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), EntitySpawnReason.MOB_SUMMONED, null);
            for (EquipmentSlot s : EquipmentSlot.values()) mob.setDropChance(s, 0.0f);
            if (this.getTarget() != null) mob.setTarget(this.getTarget());
        }
        level.addFreshEntity(e);
        level.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 0.5, spot.z, 12, 0.3, 0.6, 0.3, 0.02);
        return e;
    }

    /** Nova: everything hostile to the Wither within 8 blocks gets Wither II for 6 s and is pushed away. Returns the hits. */
    public int nova(ServerLevel level) {
        this.playSound(SoundEvents.WITHER_BREAK_BLOCK, 1.5F, 0.8F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 40, 3.0, 1.0, 3.0, 0.1);
        int hits = 0;
        double r = WitherLogic.NOVA_RADIUS;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), e -> e != this && e.isAlive())) {
            if (e.is(EntityTypeTags.WITHER_FRIENDS) || e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            if (e.distanceToSqr(this.getX(), e.getY(), this.getZ()) > r * r) continue;
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, WitherLogic.NOVA_WITHER_TICKS, WitherLogic.NOVA_WITHER_AMPLIFIER), this);
            Vec3 away = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 1.2, 0.4, away.z * 1.2);
            e.needsSync = true;
            hits++;
        }
        return hits;
    }

    /** Blight: plants turn into dead bushes, natural leaves fall, some grass dies. Never placed leaves or other blocks. */
    public int wilt(ServerLevel level) {
        if (!MobsConfig.witherWilt || !level.getGameRules().get(GameRules.MOB_GRIEFING)) return 0;
        int changed = 0, r = WitherLogic.WILT_RADIUS;
        BlockPos c = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r / 2, r))) {
            if (p.distSqr(c) > (double) r * r) continue;
            BlockState s = level.getBlockState(p);
            BlockState to = wilted(s, this.random.nextInt(5) == 0);
            if (to != null) {
                level.setBlock(p, to, 3);
                changed++;
            }
        }
        return changed;
    }

    /** What a block wilts into, or null when it stays. {@code grassDies} decides the one in five grass blocks. */
    public static @Nullable BlockState wilted(BlockState s, boolean grassDies) {
        if (s.is(BlockTags.LEAVES)) {
            return s.hasProperty(LeavesBlock.PERSISTENT) && !s.getValue(LeavesBlock.PERSISTENT) ? Blocks.AIR.defaultBlockState() : null;
        }
        if (s.is(Blocks.SHORT_GRASS) || s.is(Blocks.FERN) || s.is(BlockTags.SMALL_FLOWERS) && !s.is(Blocks.WITHER_ROSE)) {
            return Blocks.DEAD_BUSH.defaultBlockState();
        }
        if (s.is(Blocks.GRASS_BLOCK) && grassDies) return Blocks.COARSE_DIRT.defaultBlockState();
        return null;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        ItemEntity heart = this.spawnAtLocation(level, new net.minecraft.world.item.ItemStack(WitherMobs.HEART, WitherLogic.HEARTS));
        if (heart != null) heart.setExtendedLifetime();
    }

    /** Standing spot (block top with room for a skeleton) below the given column, searched downward from the Wither. */
    private @Nullable Vec3 ground(ServerLevel level, double x, double z) {
        BlockPos.MutableBlockPos p = BlockPos.containing(x, this.getY() + 2, z).mutable();
        for (int i = 0; i < 24 && p.getY() > level.getMinY(); i++, p.move(0, -1, 0)) {
            if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                    && level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                    && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()) {
                return new Vec3(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            }
        }
        return null;
    }

    private static void particle(ServerLevel level, ParticleOptions type, double x, double y, double z, int count, double spread) {
        level.sendParticles(type, x, y, z, count, spread, spread, spread, 0.01);
    }

    private double headX(int head) {
        float a = (this.yBodyRot + 180 * (head - 1)) * Mth.DEG_TO_RAD;
        return this.getX() + Mth.cos(a) * 1.3 * this.getScale();
    }

    private double headY(int head) {
        return this.getY() + (head <= 0 ? 3.0F : 2.2F) * this.getScale();
    }

    private double headZ(int head) {
        float a = (this.yBodyRot + 180 * (head - 1)) * Mth.DEG_TO_RAD;
        return this.getZ() + Mth.sin(a) * 1.3 * this.getScale();
    }
}
