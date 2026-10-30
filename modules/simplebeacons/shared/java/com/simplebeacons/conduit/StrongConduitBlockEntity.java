package com.simplebeacons.conduit;

import com.simplebeacons.BeaconsRegistry;
import com.simplebeacons.SimpleBeacons;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Block entity of every {@link ConduitKind}: Vanilla's conduit loop with {@link StrongConduitLogic}'s frames, range and effects. */
public class StrongConduitBlockEntity extends BlockEntity {
    public int tickCount;
    private float activeRotation;
    private boolean isActive;
    private boolean isHunting;
    private int radius;
    private final List<BlockPos> effectBlocks = new ArrayList<>();
    private @Nullable EntityReference<LivingEntity> destroyTarget;
    private long nextAmbientSoundActivation;

    public StrongConduitBlockEntity(BlockPos pos, BlockState state) {
        super(BeaconsRegistry.STRONG_CONDUIT_BE, pos, state);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.destroyTarget = EntityReference.read(input, "Target");
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        EntityReference.store(this.destroyTarget, output, "Target");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    /** The kind of the block this entity sits in (STRONG while the block is gone). */
    public ConduitKind kind() {
        return getBlockState().getBlock() instanceof StrongConduitBlock block ? block.kind() : ConduitKind.STRONG;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, StrongConduitBlockEntity entity) {
        entity.tickCount++;
        if (level.getGameTime() % 40L == 0L) {
            entity.refresh(level, pos);
        }
        if (entity.isActive) {
            entity.activeRotation++;
            entity.particles(level, pos);
        }
    }

    /**
     * IG31-2: the particles of an active conduit. Vanilla's nautilus stream from the frame blocks to the eye (all kinds), plus
     * the kind's aura rising around the block - the locator that makes a conduit easy to find under water.
     */
    private void particles(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        ConduitKind kind = kind();
        double bob = Mth.sin((this.tickCount + 35) * 0.1F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.3F;
        for (BlockPos frame : this.effectBlocks) {
            if (random.nextInt(50) == 0) {
                BlockPos delta = frame.subtract(pos);
                level.addParticle(kind.evil() ? ParticleTypes.SCULK_SOUL : ParticleTypes.NAUTILUS, pos.getX() + 0.5, pos.getY() + 1.5 + bob, pos.getZ() + 0.5,
                        -0.5F + random.nextFloat() + delta.getX(), -2.0F + random.nextFloat() + delta.getY(), -0.5F + random.nextFloat() + delta.getZ());
            }
        }
        if (this.tickCount % (kind.mighty() ? 2 : 4) == 0) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.45 + random.nextDouble() * 0.5;
            level.addParticle(kind.aura(), pos.getX() + 0.5 + Math.cos(angle) * radius, pos.getY() + 0.2 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.5 + Math.sin(angle) * radius, 0.0, 0.03 + random.nextDouble() * 0.03, 0.0);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, StrongConduitBlockEntity entity) {
        entity.tickCount++;
        long gameTime = level.getGameTime();
        if (gameTime % 40L == 0L) {
            boolean was = entity.isActive;
            entity.refresh(level, pos);
            if (entity.isActive != was) {
                SoundEvent event = entity.isActive ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
                level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (entity.isActive) {
                StrongConduitLogic.applyEffects(level, pos, entity.effectBlocks.size(), entity.kind());
                entity.attack((ServerLevel) level, pos, state);
            }
        }
        if (entity.isActive) {
            // IG31-2: louder and kind-specific hum (Vanilla 1.0), so a conduit is found by ear
            ConduitKind kind = entity.kind();
            if (gameTime % 80L == 0L) level.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, kind.volume(), kind.pitch());
            if (gameTime > entity.nextAmbientSoundActivation) {
                entity.nextAmbientSoundActivation = gameTime + 60L + level.getRandom().nextInt(40);
                level.playSound(null, pos, kind.ambient(), SoundSource.BLOCKS, kind.volume(), kind.pitch());
            }
        }
    }

    /** Scans the frame (both sides; the client needs it for the animation). */
    public void refresh(Level level, BlockPos pos) {
        ConduitKind kind = kind();
        this.radius = SimpleBeacons.on() ? StrongConduitLogic.scan(level, pos, this.effectBlocks, kind) : 0;
        if (this.radius == 0) this.effectBlocks.clear();
        this.isActive = this.radius > 0;
        this.isHunting = kind.killRange(this.effectBlocks.size(), this.radius) > 0;
    }

    private void attack(ServerLevel level, BlockPos pos, BlockState state) {
        int killRange = kind().killRange(this.effectBlocks.size(), this.radius);
        EntityReference<LivingEntity> target = null;
        if (killRange > 0) {
            LivingEntity current = EntityReference.getLivingEntity(this.destroyTarget, level);
            if (current != null && current.isAlive() && pos.closerThan(current.blockPosition(), killRange)) {
                target = this.destroyTarget;
            } else {
                List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(killRange),
                        e -> e instanceof Enemy && e.isInWaterOrRain());
                target = candidates.isEmpty() ? null : EntityReference.of(Util.getRandom(candidates, level.getRandom()));
            }
        }
        LivingEntity living = EntityReference.getLivingEntity(target, level);
        if (living != null) {
            level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
            living.hurtServer(level, level.damageSources().magic(), 4.0F);
        }
        if (!Objects.equals(target, this.destroyTarget)) {
            this.destroyTarget = target;
            level.sendBlockUpdated(pos, state, state, 2);
        }
    }

    public boolean isActive() {
        return this.isActive;
    }

    public boolean isHunting() {
        return this.isHunting;
    }

    /** Frame blocks found by the last scan. */
    public int frameBlocks() {
        return this.effectBlocks.size();
    }

    /** 2 (5x5x5 frame), 3 (7x7x7 frame) or 0 (inactive). */
    public int radius() {
        return this.radius;
    }

    public float getActiveRotation(float partialTicks) {
        return (this.activeRotation + partialTicks) * -0.0375F;
    }
}
