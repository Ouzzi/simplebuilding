package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.enchantment.VanillaBookTextures;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplebuilding:enchant_type}: welches Buch {@code minecraft:enchanted_book} zeigt. Die
 * Auswahl selbst (Mod-Buecher vor Vanilla-Buechern, beide per Client-Option abschaltbar) steht in
 * {@link VanillaBookTextures#select}, damit die Servertests sie ohne Client-Klassen pruefen koennen.
 */
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
        return VanillaBookTextures.select(enchants, VanillaBookTextures.modEnabled(), VanillaBookTextures.enabled());
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}
