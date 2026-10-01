package com.simplebuilding.fletching;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** Ein Item fuer alle Pfeile vom Befiederungstisch; die Teile stehen in {@code simplebuilding:arrow_parts}. */
public class CraftedArrowItem extends ArrowItem {
    public CraftedArrowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack ammo, LivingEntity shooter, ItemStack weapon) {
        return new CraftedArrow(level, shooter, ammo.copyWithCount(1), weapon);
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        CraftedArrow arrow = new CraftedArrow(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1), null);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        return arrow;
    }

    /** "Eisenpfeil" usw.: der Name folgt der Spitze. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.simplebuilding.crafted_arrow.tip." + ArrowParts.of(stack).tip().getSerializedName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
        ArrowParts.Parts parts = ArrowParts.of(stack);
        lines.accept(Component.translatable("item.simplebuilding.crafted_arrow.shaft." + parts.shaft().getSerializedName()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.simplebuilding.crafted_arrow.fletching." + parts.fletching().getSerializedName()).withStyle(ChatFormatting.GRAY));
    }
}
