package com.simplebuilding.mixin.forge;

import com.mojang.serialization.Codec;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.client.property.EnchantmentModelProperty;
import com.simplebuilding.client.property.TrimIconsModelProperty;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge-Gegenstueck zu NeoForges {@code RegisterSelectItemModelPropertyEvent} und Fabrics direktem
 * {@code SelectItemModelProperties.ID_MAPPER.put}: Forge 65 hat kein Registrierungs-Event, und der
 * ID_MAPPER ist dort privat. {@code bootstrap()} laeuft im Minecraft-Konstruktor, also vor dem ersten
 * Laden der Item-Modelle - ohne diesen Eintrag scheitert {@code minecraft:enchanted_book} mit
 * "Unknown element id: simplebuilding:enchant_type" und alle Buecher zeigen die Fehltextur
 * (ebenso die Ruestung mit {@code simplebuilding:visible_trim_icons}).
 */
@Mixin(SelectItemModelProperties.class)
public abstract class SelectItemModelPropertiesMixin {

    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, SelectItemModelProperty.Type<?, ?>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void simplebuilding$registerEnchantType(CallbackInfo ci) {
        EnchantmentModelProperty.PROPERTY_TYPE =
                SelectItemModelProperty.Type.create(EnchantmentModelProperty.CODEC, Codec.STRING);
        ID_MAPPER.put(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "enchant_type"),
                EnchantmentModelProperty.PROPERTY_TYPE);
        TrimIconsModelProperty.PROPERTY_TYPE =
                SelectItemModelProperty.Type.create(TrimIconsModelProperty.CODEC, Codec.STRING);
        ID_MAPPER.put(Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "visible_trim_icons"),
                TrimIconsModelProperty.PROPERTY_TYPE);
    }
}
