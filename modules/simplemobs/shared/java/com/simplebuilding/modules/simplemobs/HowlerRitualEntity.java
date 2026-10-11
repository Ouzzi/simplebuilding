package com.simplebuilding.modules.simplemobs;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Invisible marker that runs one manifestation (restart-safe, since it is saved with the chunk). Every tick it checks
 * the sensor, the light at the skull and the figure; any failure aborts and leaves every block in place. At the end the
 * figure is consumed and the Howler emerges at the foot of the figure.
 */
public class HowlerRitualEntity extends Entity {
    private BlockPos sensor = BlockPos.ZERO;
    private int age, duration = HowlerLogic.MANIFEST_TICKS;
    private @Nullable Integer forcedLight;

    public HowlerRitualEntity(EntityType<? extends HowlerRitualEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    void setup(BlockPos center, BlockPos sensor, int duration, @Nullable Integer forcedLight) {
        this.snapTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5, 0f, 0f);
        this.sensor = sensor.immutable();
        this.duration = duration;
        this.forcedLight = forcedLight;
    }

    public int age() { return age; }

    /** Test hook: pretend the light at the skull is {@code light} from now on. */
    void forceLight(@Nullable Integer light) { this.forcedLight = light; }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { }
    @Override public boolean hurtServer(ServerLevel level, DamageSource source, float damage) { return false; }
    @Override public boolean isPickable() { return false; }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        this.sensor = BlockPos.of(in.getLongOr("Sensor", 0L));
        this.age = in.getIntOr("Age", 0);
        this.duration = in.getIntOr("Duration", HowlerLogic.MANIFEST_TICKS);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        out.putLong("Sensor", sensor.asLong());
        out.putInt("Age", age);
        out.putInt("Duration", duration);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) return;
        BlockPos center = this.blockPosition();
        Optional<HowlerPattern.Match> figure = HowlerPattern.at(level, center);
        if (figure.isEmpty() || !HowlerRitual.sensorPresent(level, sensor)) {
            HowlerRitual.abortEffects(level, center, "fades");
            this.discard();
            return;
        }
        HowlerPattern.Match m = figure.get();
        if (!HowlerLogic.darkEnough(HowlerRitual.light(level, m, forcedLight), MobsConfig.howlerMaxLight)) {
            HowlerRitual.abortEffects(level, center, "too_bright");
            this.discard();
            return;
        }
        effects(level, m);
        if (++age >= duration) finish(level, m);
    }

    /** 0-1 s pulse and heartbeat, 1-3 s the jaw shrieks and the skull's eyes glow, 3-5 s the blocks dissolve into souls. */
    private void effects(ServerLevel level, HowlerPattern.Match m) {
        float t = age / (float) duration;
        Vec3 c = Vec3.atCenterOf(m.center()), head = Vec3.atCenterOf(m.head()), s = Vec3.atCenterOf(sensor);
        if (age % Math.max(4, 16 - age / 8) == 0) level.playSound(null, m.center(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.4f, 0.8f + t * 0.4f);
        if (t < 0.2f) {
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, s.x, s.y + 0.4, s.z, 2, 0.2, 0.2, 0.2, 0.02);
            for (boolean side : new boolean[] {false, true}) {
                Vec3 a = Vec3.atCenterOf(m.arm(side));
                level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, a.x, a.y + 0.6, a.z, 2, 0.25, 0.1, 0.25, 0.03);
            }
            if (age % 10 == 0) level.playSound(null, m.center(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 1.5f, 0.7f);
        } else if (t < 0.6f) {
            if (age % 8 == 0) level.sendParticles(new ShriekParticleOption(0), m.jaw().getX() + 0.5, m.jaw().getY() + 1.1, m.jaw().getZ() + 0.5, 1, 0, 0, 0, 0);
            if (age == (int) (duration * 0.2f)) level.playSound(null, m.jaw(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 2.0f, 0.7f);
            level.sendParticles(new DustParticleOptions(0x29DFEB, 0.8f), head.x, head.y - 0.1, head.z, 2, 0.18, 0.06, 0.18, 0.0);
        } else {
            double a = age * 0.45;
            for (int i = 0; i < 3; i++) {
                double r = 1.4 - t, ang = a + i * Math.PI * 2 / 3;
                level.sendParticles(ParticleTypes.SCULK_SOUL, c.x + Math.cos(ang) * r, c.y - 0.3 + (age % 20) * 0.08, c.z + Math.sin(ang) * r, 1, 0, 0, 0, 0.01);
            }
        }
    }

    private void finish(ServerLevel level, HowlerPattern.Match m) {
        for (BlockPos p : m.parts()) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        Vec3 c = Vec3.atCenterOf(m.center());
        level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y, c.z, 40, 0.8, 1.4, 0.8, 0.04);
        level.playSound(null, m.center(), SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 2.0f, 1.2f);
        BlockPos foot = m.foot();
        HowlerEntity howler = MobsRegistry.HOLLOW_HOWLER.create(level, EntitySpawnReason.TRIGGERED);
        if (howler != null) {
            float yaw = m.axis() == net.minecraft.core.Direction.Axis.X ? 0f : 90f;
            howler.snapTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, yaw, 0f);
            howler.setYHeadRot(yaw);
            howler.setYBodyRot(yaw);
            howler.startEmerging();
            howler.setPersistenceRequired();
            level.addFreshEntity(howler);
        }
        if (MobsConfig.howlerTransformSculk && level.getGameRules().get(GameRules.MOB_GRIEFING)) {
            int r = HowlerLogic.AFTERMATH_RADIUS;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                BlockPos ground = foot.offset(dx, -1, dz);
                if (level.getBlockState(ground).is(HowlerPattern.REPLACEABLE) && level.getBlockState(ground.above()).isAir()) {
                    level.setBlock(ground, Blocks.SCULK.defaultBlockState(), 3);
                }
            }
        }
        this.discard();
    }
}
