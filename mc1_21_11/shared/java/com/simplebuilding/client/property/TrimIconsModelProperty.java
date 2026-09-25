package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.items.VisibleTrimIcons;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplebuilding:visible_trim_icons}: ob ein Ruestungs-Icon das Besatzmuster zeigt
 * ({@link VisibleTrimIcons#VISIBLE}) oder wie in Vanilla nur den Materialfleck (jeder andere Wert
 * faellt auf den {@code fallback} der Item-Definition). Logik in {@link VisibleTrimIcons}.
 */
public record TrimIconsModelProperty() implements SelectItemModelProperty<String> {

    public static SelectItemModelProperty.Type<TrimIconsModelProperty, String> PROPERTY_TYPE;

    public static final MapCodec<TrimIconsModelProperty> CODEC = MapCodec.unit(new TrimIconsModelProperty());

    @Override
    public Type<? extends SelectItemModelProperty<String>, String> type() {
        return PROPERTY_TYPE;
    }

    @Override
    public String get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return VisibleTrimIcons.key(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}
