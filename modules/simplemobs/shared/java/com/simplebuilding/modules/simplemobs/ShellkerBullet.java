package com.simplebuilding.modules.simplemobs;

import com.google.common.base.MoreObjects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Shellker's homing bullet: Vanilla's Shulker bullet flight (axis steps toward the target), 1.5x faster, and it
 * gives High Gravity instead of Levitation. Own class because the Vanilla bullet keeps speed and target private.
 */
public class ShellkerBullet extends Projectile {
    private @Nullable EntityReference<Entity> finalTarget;
    private @Nullable Direction currentMoveDirection;
    private int flightSteps;
    private double targetDeltaX, targetDeltaY, targetDeltaZ;

    public ShellkerBullet(EntityType<? extends ShellkerBullet> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public ShellkerBullet(Level level, LivingEntity owner, Entity target, Direction.Axis invalidStartAxis) {
        this(EndNetherMobs.SHELLKER_BULLET, level);
        this.setOwner(owner);
        Vec3 position = owner.getBoundingBox().getCenter();
        this.snapTo(position.x, position.y, position.z, this.getYRot(), this.getXRot());
        this.finalTarget = EntityReference.of(target);
        this.currentMoveDirection = Direction.UP;
        this.selectNextMoveDirection(invalidStartAxis, target);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.finalTarget != null) output.store("Target", UUIDUtil.CODEC, this.finalTarget.getUUID());
        output.storeNullable("Dir", Direction.LEGACY_ID_CODEC, this.currentMoveDirection);
        output.putInt("Steps", this.flightSteps);
        output.putDouble("TXD", this.targetDeltaX);
        output.putDouble("TYD", this.targetDeltaY);
        output.putDouble("TZD", this.targetDeltaZ);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.flightSteps = input.getIntOr("Steps", 0);
        this.targetDeltaX = input.getDoubleOr("TXD", 0.0);
        this.targetDeltaY = input.getDoubleOr("TYD", 0.0);
        this.targetDeltaZ = input.getDoubleOr("TZD", 0.0);
        this.currentMoveDirection = input.read("Dir", Direction.LEGACY_ID_CODEC).orElse(null);
        this.finalTarget = EntityReference.read(input, "Target");
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    private void selectNextMoveDirection(Direction.@Nullable Axis avoidAxis, @Nullable Entity target) {
        double yOffset = 0.5;
        BlockPos targetPos;
        if (target == null) {
            targetPos = this.blockPosition().below();
        } else {
            yOffset = target.getBbHeight() * 0.5;
            targetPos = BlockPos.containing(target.getX(), target.getY() + yOffset, target.getZ());
        }
        double targetX = targetPos.getX() + 0.5, targetY = targetPos.getY() + yOffset, targetZ = targetPos.getZ() + 0.5;
        Direction selection = null;
        if (!targetPos.closerToCenterThan(this.position(), 2.0)) {
            BlockPos current = this.blockPosition();
            List<Direction> options = new ArrayList<>();
            if (avoidAxis != Direction.Axis.X) {
                if (current.getX() < targetPos.getX() && this.level().isEmptyBlock(current.east())) options.add(Direction.EAST);
                else if (current.getX() > targetPos.getX() && this.level().isEmptyBlock(current.west())) options.add(Direction.WEST);
            }
            if (avoidAxis != Direction.Axis.Y) {
                if (current.getY() < targetPos.getY() && this.level().isEmptyBlock(current.above())) options.add(Direction.UP);
                else if (current.getY() > targetPos.getY() && this.level().isEmptyBlock(current.below())) options.add(Direction.DOWN);
            }
            if (avoidAxis != Direction.Axis.Z) {
                if (current.getZ() < targetPos.getZ() && this.level().isEmptyBlock(current.south())) options.add(Direction.SOUTH);
                else if (current.getZ() > targetPos.getZ() && this.level().isEmptyBlock(current.north())) options.add(Direction.NORTH);
            }
            selection = Direction.getRandom(this.random);
            if (options.isEmpty()) {
                for (int attempts = 5; !this.level().isEmptyBlock(current.relative(selection)) && attempts > 0; attempts--) selection = Direction.getRandom(this.random);
            } else {
                selection = options.get(this.random.nextInt(options.size()));
            }
            targetX = this.getX() + selection.getStepX();
            targetY = this.getY() + selection.getStepY();
            targetZ = this.getZ() + selection.getStepZ();
        }
        this.currentMoveDirection = selection;
        double xa = targetX - this.getX(), ya = targetY - this.getY(), za = targetZ - this.getZ();
        double distance = Math.sqrt(xa * xa + ya * ya + za * za);
        if (distance == 0.0) {
            this.targetDeltaX = this.targetDeltaY = this.targetDeltaZ = 0.0;
        } else {
            this.targetDeltaX = xa / distance * EndNetherLogic.BULLET_SPEED;
            this.targetDeltaY = ya / distance * EndNetherLogic.BULLET_SPEED;
            this.targetDeltaZ = za / distance * EndNetherLogic.BULLET_SPEED;
        }
        this.needsSync = true;
        this.flightSteps = 10 + this.random.nextInt(5) * 10;
    }

    @Override
    public void checkDespawn() {
        if (this.level().getDifficulty() == Difficulty.PEACEFUL) this.discard();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        super.tick();
        Entity target = !this.level().isClientSide() ? EntityReference.getEntity(this.finalTarget, this.level()) : null;
        HitResult hitResult = null;
        if (!this.level().isClientSide()) {
            if (target == null) this.finalTarget = null;
            if (target == null || !target.isAlive() || target instanceof Player && target.isSpectator()) {
                this.applyGravity();
            } else {
                this.targetDeltaX = Mth.clamp(this.targetDeltaX * 1.025, -1.0, 1.0);
                this.targetDeltaY = Mth.clamp(this.targetDeltaY * 1.025, -1.0, 1.0);
                this.targetDeltaZ = Mth.clamp(this.targetDeltaZ * 1.025, -1.0, 1.0);
                Vec3 m = this.getDeltaMovement();
                this.setDeltaMovement(m.add((this.targetDeltaX - m.x) * 0.2, (this.targetDeltaY - m.y) * 0.2, (this.targetDeltaZ - m.z) * 0.2));
            }
            hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        }
        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.position().add(movement));
        this.applyEffectsFromBlocks();
        if (this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) this.handlePortal();
        if (hitResult != null && this.isAlive() && hitResult.getType() != HitResult.Type.MISS) this.hitTargetOrDeflectSelf(hitResult);
        ProjectileUtil.rotateTowardsMovement(this, 0.5F);
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getX() - movement.x, this.getY() - movement.y + 0.15, this.getZ() - movement.z, 0.0, 0.0, 0.0);
        } else if (target != null) {
            if (this.flightSteps > 0) {
                this.flightSteps--;
                if (this.flightSteps == 0) this.selectNextMoveDirection(this.currentMoveDirection == null ? null : this.currentMoveDirection.getAxis(), target);
            }
            if (this.currentMoveDirection != null) {
                BlockPos current = this.blockPosition();
                Direction.Axis axis = this.currentMoveDirection.getAxis();
                if (this.level().loadedAndEntityCanStandOn(current.relative(this.currentMoveDirection), this)) {
                    this.selectNextMoveDirection(axis, target);
                } else {
                    BlockPos tp = target.blockPosition();
                    if (axis == Direction.Axis.X && current.getX() == tp.getX() || axis == Direction.Axis.Z && current.getZ() == tp.getZ()
                            || axis == Direction.Axis.Y && current.getY() == tp.getY()) {
                        this.selectNextMoveDirection(axis, target);
                    }
                }
            }
        }
    }

    @Override
    protected boolean isAffectedByBlocks() {
        return !this.isRemoved();
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && !entity.noPhysics;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0;
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();
        LivingEntity livingOwner = owner instanceof LivingEntity l ? l : null;
        DamageSource damageSource = this.damageSources().mobProjectile(this, livingOwner);
        if (target.hurtOrSimulate(damageSource, 4.0F)) {
            if (this.level() instanceof ServerLevel serverLevel) EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(EndNetherMobs.gravity(), EndNetherLogic.BULLET_EFFECT_TICKS), MoreObjects.firstNonNull(owner, this));
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        ((ServerLevel) this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.0);
        this.playSound(SoundEvents.SHULKER_BULLET_HIT, 1.0F, 0.8F);
    }

    @Override
    protected void onRedirectProjectile(Projectile hitProjectile) {
        hitProjectile.deflect(ProjectileDeflection.MOMENTUM_DEFLECT, this, this.owner, true, 1.0);
    }

    private void destroy() {
        this.discard();
        this.level().gameEvent(GameEvent.ENTITY_DAMAGE, this.position(), GameEvent.Context.of(this));
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        this.destroy();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean hurtClient(DamageSource source) {
        return true;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.playSound(SoundEvents.SHULKER_BULLET_HURT, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 15, 0.2, 0.2, 0.2, 0.0);
        this.destroy();
        return true;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(packet.getMovement());
    }
}
