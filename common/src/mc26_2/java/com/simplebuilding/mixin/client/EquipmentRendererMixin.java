package com.simplebuilding.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.client.render.TrimPulseTextures;
import com.simplebuilding.util.GlowingTrimUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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

/**
 * Licht und Farbe des Ruestungsbesatzes (Lichtregeln des Besitzers 2026-09-29, siehe
 * {@link GlowingTrimUtils#trimLight}): Glowing leuchtet ruhig voll hell (nur eine Stufe), nur Pulsating + Glowing
 * schwankt in der Helligkeit, Pulsating allein laesst das Licht und pulsiert in der Saettigung.
 */
@Mixin(EquipmentLayerRenderer.class)
public class EquipmentRendererMixin {

    @Unique
    private static final Map<Identifier, RenderType> simplebuilding$decalTypes = new HashMap<>();

    @ModifyVariable(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            ),
            argsOnly = true,
            ordinal = 0
    )
    private int makeTrimGlow(int light, @Local(argsOnly = true) ItemStack stack) {
        // Injection-Punkt liegt bereits im "trim != null"-Zweig von renderLayers, daher ist kein eigener
        // ArmorTrim-Check noetig. Die Ruestungs-Layer selbst sind zu diesem Zeitpunkt schon mit dem
        // Original-Licht submitted; ueberschrieben wird nur noch das Licht fuer den Trim-Submit.
        return GlowingTrimUtils.trimLight(light, GlowingTrimUtils.getGlowLevel(stack),
                GlowingTrimUtils.isPulsating(stack), System.currentTimeMillis());
    }

    /**
     * Pulsating ohne Glowing: der Besatz-Submit (der erste nach dem Besatz-RenderType; danach kommt
     * hoechstens noch der Glanz) zeichnet statt des Atlas-Sprites eine entsaettigte Kopie derselben
     * Ebene ({@link TrimPulseTextures}) - Stufe 0 (volle Farbe) und alles, wofuer es keine Kopie gibt,
     * bleibt Vanilla. Mit Glowing bleibt die Farbe, dort pulsiert das Licht ({@link #makeTrimGlow}).
     */
    @WrapOperation(
            method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;armorTrimsSheet(Z)Lnet/minecraft/client/renderer/rendertype/RenderType;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", ordinal = 0)
    )
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void simplebuilding$pulseTrim(OrderedSubmitNodeCollector collector, Model model, Object state, PoseStack poseStack,
                                          RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite,
                                          int outlineColor, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original,
                                          @Local(argsOnly = true) ItemStack stack,
                                          @Local(argsOnly = true) EquipmentClientInfo.LayerType layerType) {
        ArmorTrim trim = stack.get(DataComponents.TRIM);
        Identifier texture = trim == null || sprite == null ? null : simplebuilding$desaturatedTrim(stack, trim, layerType, sprite);
        if (texture == null) {
            original.call(collector, model, state, poseStack, renderType, light, overlay, color, sprite, outlineColor, crumbling);
            return;
        }
        RenderType pulsed = trim.pattern().value().decal()
                ? simplebuilding$decalTypes.computeIfAbsent(texture, RenderTypes::createArmorDecalCutoutNoCull)
                : RenderTypes.armorCutoutNoCull(texture);
        original.call(collector, model, state, poseStack, pulsed, light, overlay, color, null, outlineColor, crumbling);
    }

    /**
     * Die entsaettigte Besatz-Ebene fuer diesen Augenblick, oder {@code null} (kein Pulsating, Glowing
     * dabei, Stufe 0 oder keine Kopie moeglich). Der Sprite heisst {@code <prefix>/<muster>_<palette>}
     * (ArmorTrim#layerAssetId); die Palette liegt wie alle Besatz-Paletten des Atlas unter
     * {@code minecraft:trims/color_palettes/}, das Graustufen-Muster unter {@code <prefix>/<muster>}.
     */
    @Unique
    private static Identifier simplebuilding$desaturatedTrim(ItemStack stack, ArmorTrim trim,
                                                             EquipmentClientInfo.LayerType layerType, TextureAtlasSprite sprite) {
        if (!GlowingTrimUtils.isPulsating(stack) || GlowingTrimUtils.getGlowLevel(stack) > 0) {
            return null;
        }
        int step = GlowingTrimUtils.desaturationStep(System.currentTimeMillis());
        if (step <= 0) {
            return null;
        }
        Identifier pattern = trim.pattern().value().assetId();
        String basePath = layerType.trimAssetPrefix() + "/" + pattern.getPath();
        String spritePath = sprite.contents().name().getPath();
        if (!spritePath.startsWith(basePath + "_")) {
            return null;
        }
        String palette = spritePath.substring(basePath.length() + 1);
        return TrimPulseTextures.texture(
                pattern.withPath("textures/" + basePath + ".png"),
                Identifier.withDefaultNamespace("textures/trims/color_palettes/trim_palette.png"),
                Identifier.withDefaultNamespace("textures/trims/color_palettes/" + palette + ".png"),
                step);
    }
}
