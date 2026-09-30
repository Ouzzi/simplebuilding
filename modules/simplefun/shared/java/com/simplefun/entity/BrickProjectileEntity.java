package com.simplefun.entity;

import com.simplefun.SimplefunCommon;
import com.simplefun.registry.ModEntities;
import com.simplefun.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class BrickProjectileEntity extends ThrowableItemProjectile {
    private static final java.util.Map<Player,Long> NEXT_THROW = new java.util.WeakHashMap<>();

    private static final ItemStack BRICK_PARTICLE_STACK = new ItemStack(Items.BRICK);
    private static final ItemStack SNOWBALL_PARTICLE_STACK = new ItemStack(Items.SNOWBALL);

    public BrickProjectileEntity(EntityType<? extends BrickProjectileEntity> type, Level level) {
        super(type, level);
    }

    public BrickProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.BRICK_PROJECTILE, owner, level, new ItemStack(Items.BRICK));
    }

    @Override public void tick(){super.tick();if(!level().isClientSide() && tickCount>com.simplefun.config.SimplefunConfig.MAX_PROJECTILE_TICKS)discard();}
    @Override
    protected Item getDefaultItem() {
        return Items.BRICK;
    }

    /**
     * Shared throw logic for throwable bricks (ItemMixin) and the Brick Snowball.
     */
    public static void throwFrom(Level level, Player player, ItemStack stack) {
        if (!(level instanceof ServerLevel) || stack.isEmpty()) return;
        var cfg=SimplefunCommon.getConfig().fun;
        if (stack.is(ModItems.BRICK_SNOWBALL) ? !cfg.enableBrickSnowball : !cfg.enableThrowableBricks) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;
        long now=level.getGameTime();
        if(now<NEXT_THROW.getOrDefault(player,Long.MIN_VALUE))return;
        NEXT_THROW.put(player,now+com.simplefun.config.SimplefunConfig.THROW_COOLDOWN);
        player.getCooldowns().addCooldown(stack, com.simplefun.config.SimplefunConfig.THROW_COOLDOWN);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide()) {
            BrickProjectileEntity projectile = new BrickProjectileEntity(level, player);
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
            level.addFreshEntity(projectile);
        }

        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        // 3 = impact
        if (id != 3) return;
        ItemStack stack = this.getItem();
        spawnImpactParticles(stack, 8);
        if (stack.is(ModItems.BRICK_SNOWBALL)) {
            spawnImpactParticles(BRICK_PARTICLE_STACK, 8);
            spawnImpactParticles(SNOWBALL_PARTICLE_STACK, 8);
        }
    }

    private void spawnImpactParticles(ItemStack stack, int count) {
        for (int i = 0; i < count; ++i) {
            this.level().addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, stack.getItem()),
                    this.getX(), this.getY(), this.getZ(),
                    ((double) this.random.nextFloat() - 0.5D) * 0.08D,
                    ((double) this.random.nextFloat() - 0.5D) * 0.08D,
                    ((double) this.random.nextFloat() - 0.5D) * 0.08D
            );
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.level() instanceof ServerLevel serverLevel) {
            float damage = SimplefunCommon.getConfig().fun.brickDamage;
            if (this.getItem().is(ModItems.BRICK_SNOWBALL)) {
                damage = SimplefunCommon.getConfig().fun.brickSnowballDamage;
            }
            entity.hurtServer(serverLevel, this.damageSources().thrown(this, this.getOwner()), damage);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide()) return;

        BlockPos pos = result.getBlockPos();
        BlockState state = this.level().getBlockState(pos);
        boolean canBreak = SimplefunCommon.getConfig().fun.throwableBricksBreakBlocks;

        if (canBreak && level().getServer().isSingleplayer() && shouldBreakBlock(state) && this.getOwner() instanceof net.minecraft.server.level.ServerPlayer player && player.mayInteract((ServerLevel)level(), pos) && level().getWorldBorder().isWithinBounds(pos)) {
            this.level().destroyBlock(pos, true, this.getOwner());
            this.playSound(SoundEvents.GLASS_BREAK, 1.0f, 1.0f);
        } else if (this.getItem().is(ModItems.BRICK_SNOWBALL)) {
            this.playSound(SoundEvents.SNOW_BREAK, 1.0f, 0.8f);
        } else {
            this.playSound(SoundEvents.STONE_HIT, 1.0f, 1.5f);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide()) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }

    private boolean shouldBreakBlock(BlockState state) {
        if (state.is(Blocks.TINTED_GLASS)) return false;
        return state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassBlock || state.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock;
    }
}
