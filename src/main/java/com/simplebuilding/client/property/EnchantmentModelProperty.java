package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.SimplebuildingClient;
import com.simplebuilding.enchantment.ModEnchantments;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.item.property.select.SelectProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record EnchantmentModelProperty() implements SelectProperty<String> {

    // Codec für die Property-Deklaration selbst (leer, da keine Felder im Record)
    public static final MapCodec<EnchantmentModelProperty> CODEC = MapCodec.unit(new EnchantmentModelProperty());

    @Override
    public Type<? extends SelectProperty<String>, String> getType() {
        return SimplebuildingClient.ENCHANTMENT_PROPERTY_TYPE;
    }

    @Override
    public String getValue(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        ItemEnchantmentsComponent enchants = stack.get(DataComponentTypes.STORED_ENCHANTMENTS);

        // Fallback für normale Items (nicht Enchanted Books)
        if (enchants == null) {
            enchants = stack.get(DataComponentTypes.ENCHANTMENTS);
        }

        if (enchants == null) return "none";

        if (world == null && entity != null) world = (ClientWorld) entity.getEntityWorld();
        if (world == null) return "none";

        var reg = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        // Deine Liste
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

    // WICHTIG: Das ist die Methode, die dein Source Code verlangt
    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}