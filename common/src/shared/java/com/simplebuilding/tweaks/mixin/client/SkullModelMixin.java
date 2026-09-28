package com.simplebuilding.tweaks.mixin.client;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.BlazeHeadType;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
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
 * Modell und Textur des Lohenkopfs. Vanillas {@code SkullBlockRenderer#createModel} kennt nur die
 * eigenen Kopf-Typen und liefert fuer alle anderen null; Block-Renderer, Item-Modell
 * ({@code minecraft:head}) und der getragene Kopf ({@code CustomHeadLayer}) holen ihr Modell alle
 * dort. Der Lohenkopf ist Vanillas Mob-Kopf-Wuerfel (8x8x8, Textur 64x32 wie der Creeper-Kopf), die
 * Textur {@code simplebuilding:textures/entity/blaze_head.png} steht in {@code SKIN_BY_TYPE}.
 */
@Mixin(SkullBlockRenderer.class)
public abstract class SkullModelMixin {
    @Shadow
    @Final
    private static Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void simplebuilding$blazeHeadSkin(CallbackInfo ci) {
        SKIN_BY_TYPE.put(BlazeHeadType.BLAZE, SimpleTweaks.id("textures/entity/blaze_head.png"));
        SKIN_BY_TYPE.put(BlazeHeadType.ENDERMAN, SimpleTweaks.id("textures/entity/enderman_head.png"));
    }

    @Inject(method = "createModel", at = @At("HEAD"), cancellable = true)
    private static void simplebuilding$blazeHeadModel(EntityModelSet modelSet, SkullBlock.Type type, CallbackInfoReturnable<SkullModelBase> cir) {
        if (type instanceof BlazeHeadType) {
            cir.setReturnValue(new SkullModel(SkullModel.createMobHeadLayer().bakeRoot()));
        }
    }
}
