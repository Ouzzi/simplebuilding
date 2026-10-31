package com.simplebuilding.modules.simplemobmanners;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

/**
 * Water bucket that releases its frog (variant, name, age restored) instead of the plain Vanilla spawn. The item model shows the
 * frog variant (items/frog_bucket.json selects on {@code minecraft:frog/variant}); the tooltip names the frog and its variant (IG31-23).
 */
public final class FrogBucketItem extends MobBucketItem {
    public FrogBucketItem(Properties properties) {
        super(net.minecraft.world.entity.EntityTypes.FROG, Fluids.WATER, SoundEvents.BUCKET_EMPTY_TADPOLE, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, display, lines, flag);
        tooltip(stack, lines);
    }

    /** Grey lines "Frog: <name>" (when named) and "Variant: <variant>" (when the bucket carries one). */
    public static void tooltip(ItemStack stack, java.util.function.Consumer<Component> lines) {
        Component name = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name != null) lines.accept(Component.translatable("tooltip.simplemobmanners.frog_bucket.name", name).withStyle(ChatFormatting.GRAY));
        var variant = stack.get(net.minecraft.core.component.DataComponents.FROG_VARIANT);
        if (variant != null) {
            variant.unwrapKey().ifPresent(key -> lines.accept(Component.translatable("tooltip.simplemobmanners.frog_bucket.variant",
                    Component.translatableWithFallback("tooltip.simplemobmanners.frog_bucket.variant." + key.identifier().getPath(), key.identifier().getPath()))
                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public void checkExtraContent(@Nullable LivingEntity user, Level level, ItemStack stack, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            FrogBucket.release(serverLevel, stack, pos);
            level.gameEvent(user, GameEvent.ENTITY_PLACE, pos);
        }
    }
}
