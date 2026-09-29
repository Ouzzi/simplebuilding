package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.client.render.TrimPulseTextures;
import com.simplebuilding.util.GlowingTrimUtils;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Slice;

// MC 26.3 twin of common/src/mc26_2/java/.../EquipmentRendererMixin.java: trims are drawn through
// RenderTypes.armorTrim (paletted textures) instead of the armor trim atlas sheet. Same light rules
// (GlowingTrimUtils#trimLight); the saturation pulse swaps the paletted texture for a desaturated copy
// built from the same pattern and palette (TrimPulseTextures), taking the equipment's trim overrides
// into account exactly like EquipmentLayerRenderer$TrimTextureKey does.
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRendererMixin {

    /** Unveraenderte UVs: die entsaettigte Kopie ist eine eigene Textur und kein Atlas-Ausschnitt. */
    @Unique
    private static final UvMapping simplebuilding$wholeTexture = new UvMapping() {
        @Override
        public float getU(float offset) {
            return offset;
        }

        @Override
        public float getV(float offset) {
            return offset;
        }
    };

    @ModifyVariable(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            ),
            argsOnly = true,
            ordinal = 0
    )
    private int makeTrimGlow(int light, @Local(argsOnly = true) ItemStack stack) {
        // Injection-Punkt liegt bereits im "hasTrim"-Zweig von renderLayers; ueberschrieben wird nur
        // noch das Licht fuer den Trim-Submit (die Ruestungs-Layer sind schon submitted).
        return GlowingTrimUtils.trimLight(light, GlowingTrimUtils.getGlowLevel(stack),
                GlowingTrimUtils.isPulsating(stack), System.currentTimeMillis());
    }

    /** Pulsating ohne Glowing: der Besatz-Submit zeichnet die entsaettigte Kopie (siehe 26.2-Zwilling). */
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorTrim(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V", ordinal = 0)
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void simplebuilding$pulseTrim(OrderedSubmitNodeCollector collector, Model model, Object state, PoseStack poseStack,
                                          RenderType renderType, int light, int overlay, int color, UvMapping uvMapping,
                                          int outlineColor, Operation<Void> original,
                                          @Local(argsOnly = true) ItemStack stack,
                                          @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType,
                                          @Local EquipmentClientInfo equipmentInfo) {
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        Identifier texture = trim == null ? null : simplebuilding$desaturatedTrim(stack, trim, layerType, equipmentInfo);
        if (texture == null) {
            original.call(collector, model, state, poseStack, renderType, light, overlay, color, uvMapping, outlineColor);
            return;
        }
        RenderType pulsed = RenderTypes.armorTrim(texture, trim.pattern().value().decal());
        original.call(collector, model, state, poseStack, pulsed, light, overlay, color, simplebuilding$wholeTexture, outlineColor);
    }

    /**
     * Die entsaettigte Besatz-Ebene fuer diesen Augenblick, oder {@code null}. Muster und Palette wie in
     * EquipmentLayerRenderer$TrimTextureKey#getOrPrepareTexture (inklusive trim_overrides); Paletten liegen
     * unter {@code textures/palettes/<id>.png}, die Schluesselfarben in {@code minecraft:palettes/trim_base}.
     */
    @Unique
    private static Identifier simplebuilding$desaturatedTrim(ItemStack stack, ArmorTrim trim,
                                                             EquipmentClientInfo.LayerType layerType,
                                                             EquipmentClientInfo equipmentInfo) {
        if (!GlowingTrimUtils.isPulsating(stack) || GlowingTrimUtils.getGlowLevel(stack) > 0) {
            return null;
        }
        int step = GlowingTrimUtils.desaturationStep(System.currentTimeMillis());
        if (step <= 0) {
            return null;
        }
        Identifier textureId = trim.pattern().value().assetId();
        Identifier paletteId = trim.material().value().paletteId();
        for (EquipmentClientInfo.TrimOverride override : equipmentInfo.trimOverrides()) {
            if (override.predicate().matches(trim)) {
                textureId = override.textureId().orElse(textureId);
                paletteId = override.paletteId().orElse(null);
                break;
            }
        }
        String prefix = layerType.trimAssetPrefix();
        return TrimPulseTextures.texture(
                textureId.withPath(path -> "textures/" + prefix + "/" + path + ".png"),
                Identifier.withDefaultNamespace("textures/palettes/trim_base.png"),
                paletteId == null ? null : paletteId.withPath(path -> "textures/palettes/" + path + ".png"),
                step);
    }
}
