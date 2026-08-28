package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

public record EnchantmentModelProperty() implements SelectItemModelProperty<String> {

    public static SelectItemModelProperty.Type<EnchantmentModelProperty, String> PROPERTY_TYPE;

    public static final MapCodec<EnchantmentModelProperty> CODEC = MapCodec.unit(new EnchantmentModelProperty());

    @Override
    public Type<? extends SelectItemModelProperty<String>, String> type() {
        return PROPERTY_TYPE;
    }

    @Override
    public String get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        ItemEnchantments enchants = stack.get(DataComponents.STORED_ENCHANTMENTS);

        if (enchants == null) {
            enchants = stack.get(DataComponents.ENCHANTMENTS);
        }

        if (enchants == null) {
            return "none";
        }

        if (world == null && entity != null) {
            world = (ClientLevel) entity.level();
        }
        if (world == null) {
            return "none";
        }

        var reg = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.FAST_CHISELING)) > 0) return "fast_chiseling";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.CONSTRUCTORS_TOUCH)) > 0) return "constructors_touch";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.COLOR_PALETTE)) > 0) return "color_palette";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.MASTER_BUILDER)) > 0) return "master_builder";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.BREAK_THROUGH)) > 0) return "break_through";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.RADIUS)) > 0) return "radius";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.COVER)) > 0) return "cover";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.BRIDGE)) > 0) return "bridge";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.LINEAR)) > 0) return "linear";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.VEIN_MINER)) > 0) return "vein_miner";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.DEEP_POCKETS)) > 0) return "deep_pockets";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.STRIP_MINER)) > 0) return "strip_miner";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.VERSATILITY)) > 0) return "versatility";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.DRAWER)) > 0) return "drawer";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.KINETIC_PROTECTION)) > 0) return "kinetic_protection";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.DOUBLE_JUMP)) > 0) return "double_jump";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.OVERRIDE)) > 0) return "override";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.FUNNEL)) > 0) return "funnel";
        if (enchants.getLevel(reg.getOrThrow(ModEnchantments.RANGE)) > 0) return "range";

        return "none";
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}
