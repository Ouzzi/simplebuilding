package com.simplebuilding.modules.simplemobmanners;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/** Water bucket that releases its frog (variant, name, age restored) instead of the plain Vanilla spawn. */
public final class FrogBucketItem extends MobBucketItem {
    public FrogBucketItem(Properties properties) {
        super(net.minecraft.world.entity.EntityType.FROG, Fluids.WATER, SoundEvents.BUCKET_EMPTY_TADPOLE, properties);
    }

    @Override
    public void checkExtraContent(@Nullable LivingEntity user, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            FrogBucket.release(serverLevel, stack, pos);
            level.gameEvent(user, GameEvent.ENTITY_PLACE, pos);
        }
    }
}
