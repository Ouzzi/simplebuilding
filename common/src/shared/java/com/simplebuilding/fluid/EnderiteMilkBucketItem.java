package com.simplebuilding.fluid;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.cow.AbstractCow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Milk in the Enderite bucket (queue N32): like water it comes as a half (1/2) and a full (2/2) item. Milking a cow,
 * goat or mooshroom with the empty bucket gives the half one, milking again the full one. Drinking uses one filling
 * (Vanilla's milk effect: clears all status effects; the stack turns into the next smaller bucket through
 * {@code usingConvertsTo}). The milk cauldron of Simple Sandwiches reads the item tag
 * {@code simplesandwiches:milk_buckets} and the crafting remainder (no code dependency on that module).
 */
public class EnderiteMilkBucketItem extends Item {
    private final boolean full;

    public EnderiteMilkBucketItem(boolean full, Properties properties) {
        super(properties);
        this.full = full;
    }

    public boolean isFull() {
        return full;
    }

    /** Buckets of milk inside: 1 or 2. */
    public int amount() {
        return full ? 2 : 1;
    }

    /** The milk bucket after one more milking of {@code held} (empty -> half -> full); null when it cannot take more. */
    public static @Nullable Item milked(ItemStack held) {
        if (ModFluids.ENDERITE_MILK_BUCKET == null) return null;
        if (held.is(ModFluids.ENDERITE_BUCKET)) return ModFluids.ENDERITE_MILK_BUCKET;
        if (held.is(ModFluids.ENDERITE_MILK_BUCKET)) return ModFluids.FULL_ENDERITE_MILK_BUCKET;
        return null;
    }

    /** Cow, mooshroom and goat give milk (adults only), as with Vanilla's bucket. */
    public static boolean milkable(LivingEntity target) {
        return (target instanceof AbstractCow || target instanceof Goat) && !target.isBaby();
    }

    /** Milking {@code target} with {@code stack} (empty or half Enderite bucket); PASS when it does not apply. */
    public static InteractionResult milk(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        Item next = milked(stack);
        if (next == null || !milkable(target)) return InteractionResult.PASS;
        player.playSound(target instanceof Goat ? SoundEvents.GOAT_MILK : SoundEvents.COW_MILK, 1.0F, 1.0F);
        if (!player.level().isClientSide() && !player.hasInfiniteMaterials()) {
            // Player#interactOn clears the hand when the stack it held ends up empty, so a single bucket is replaced
            // without being shrunk first (ItemUtils.createFilledResult would shrink it).
            ItemStack filled = stack.transmuteCopy(next, 1);
            if (stack.getCount() == 1) {
                player.setItemInHand(hand, filled);
            } else {
                stack.shrink(1);
                if (!player.getInventory().add(filled)) player.drop(filled, false);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** The half bucket also fills up from the animal. */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return full ? InteractionResult.PASS : milk(stack, player, target, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("tooltip.simplebuilding.enderite_bucket.fill", amount(), 2).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable(full ? "tooltip.simplebuilding.enderite_milk_bucket.full" : "tooltip.simplebuilding.enderite_milk_bucket.half")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
