package com.simplebuilding.tweaks.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import com.simplebuilding.tweaks.client.ModSkullModels;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.level.block.SkullBlock;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Modell und Textur der Mod-Koepfe (docs/MOBKOEPFE.md). Vanillas {@code SkullBlockRenderer#createModel}
 * kennt nur die eigenen Kopf-Typen und liefert fuer alle anderen null; Block-Renderer, Item-Modell
 * ({@code minecraft:head}) und der getragene Kopf ({@code CustomHeadLayer}) holen ihr Modell alle
 * dort und zeichnen ueber {@code submitSkull}. Die Texturen sind die echten Vanilla-Mob-Texturen
 * der Mobs, siehe {@link ModSkullModels}; Koepfe mit Augen, Kleidung oder durchscheinender Huelle bekommen in {@code submitSkull}
 * zusaetzlich ihre Schichten.
 */
@Mixin(SkullBlockRenderer.class)
public abstract class SkullModelMixin {
    @Shadow
    @Final
    private static Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void simplebuilding$modHeadSkins(CallbackInfo ci) {
        for (BlazeHeadType type : BlazeHeadType.values()) {
            SKIN_BY_TYPE.put(type, ModSkullModels.texture(type));
        }
    }

    @Inject(method = "createModel", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$modHeadModel(EntityModelSet modelSet, SkullBlock.Type type, CallbackInfoReturnable<SkullModelBase> cir) {
        if (type instanceof BlazeHeadType modType) {
            cir.setReturnValue(ModSkullModels.createModel(modType));
        }
    }

    @Inject(method = "submitSkull", at = @At("TAIL"))
    private static void simplebuilding$modHeadGlow(float animationValue, PoseStack poseStack, SubmitNodeCollector collector,
                                                   int lightCoords, SkullModelBase model, RenderType renderType, int outlineColor,
                                                   ModelFeatureRenderer.CrumblingOverlay breakProgress, CallbackInfo ci) {
        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = animationValue;
        ModSkullModels.submitLayers(model, state, poseStack, collector, lightCoords, outlineColor);
    }
}
