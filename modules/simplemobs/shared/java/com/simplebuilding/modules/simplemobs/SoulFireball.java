package com.simplebuilding.modules.simplemobs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Soulfire Blaze projectile: a small fireball with a blue look (a fire charge drawn with the
 * {@code simplemobs:soul_fire_charge} item model, no extra item), a bit more damage and burn time; on soul sand or
 * soul soil the fire it lights is soul fire (Vanilla's {@link BaseFireBlock#getState}).
 */
public class SoulFireball extends Fireball {
    public static final Identifier LOOK = Identifier.fromNamespaceAndPath(MobsRegistry.ID, "soul_fire_charge");

    public SoulFireball(EntityType<? extends SoulFireball> type, Level level) {
        super(type, level);
    }

    public SoulFireball(Level level, LivingEntity mob, Vec3 direction) {
        super(EndNetherMobs.SOUL_FIREBALL, mob, direction, level);
        ItemStack look = new ItemStack(Items.FIRE_CHARGE);
        look.set(DataComponents.ITEM_MODEL, LOOK);
        this.setItem(look);
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (!(this.level() instanceof ServerLevel level)) return;
        Entity target = hitResult.getEntity();
        int before = target.getRemainingFireTicks();
        target.igniteForSeconds(EndNetherLogic.SOUL_FIRE_SECONDS);
        DamageSource source = this.damageSources().fireball(this, this.getOwner());
        if (!target.hurtServer(level, source, EndNetherLogic.SOUL_FIREBALL_DAMAGE)) target.setRemainingFireTicks(before);
        else EnchantmentHelper.doPostAttackEffects(level, target, source);
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (this.getOwner() instanceof Mob && !level.getGameRules().get(GameRules.MOB_GRIEFING)) return;
        BlockPos pos = hitResult.getBlockPos().relative(hitResult.getDirection());
        if (level.isEmptyBlock(pos)) level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) this.discard();
    }
}
