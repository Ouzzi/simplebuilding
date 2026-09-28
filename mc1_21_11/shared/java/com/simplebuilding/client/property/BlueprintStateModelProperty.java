package com.simplebuilding.client.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.custom.BlueprintItem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * {@code simplebuilding:blueprint_state}: {@code empty}, {@code edited} oder {@code signed} - welche
 * der drei Blaupausen-Texturen das Item zeigt. Die Entscheidung steht in
 * {@link BlueprintItem#modelState}, damit die Servertests sie ohne Client-Klassen pruefen koennen.
 * Registriert wie {@link EnchantmentModelProperty}: Fabric direkt am {@code ID_MAPPER} (auch fuer die
 * Datagen), NeoForge ueber {@code RegisterSelectItemModelPropertyEvent}, Forge per Mixin.
 */
public record BlueprintStateModelProperty() implements SelectItemModelProperty<String> {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "blueprint_state");

    public static final MapCodec<BlueprintStateModelProperty> CODEC = MapCodec.unit(new BlueprintStateModelProperty());

    public static final SelectItemModelProperty.Type<BlueprintStateModelProperty, String> PROPERTY_TYPE =
            SelectItemModelProperty.Type.create(CODEC, Codec.STRING);

    @Override
    public Type<? extends SelectItemModelProperty<String>, String> type() {
        return PROPERTY_TYPE;
    }

    @Override
    public String get(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed, ItemDisplayContext context) {
        return BlueprintItem.modelState(stack);
    }

    @Override
    public Codec<String> valueCodec() {
        return Codec.STRING;
    }
}
