package com.simplemobheads.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import com.simplemobheads.MobHeadType;
import com.simplemobheads.MobHeads;
import com.simplemobheads.client.MobHeadModel;
import com.simplemobheads.client.MobHeadModels;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.blockentity.state.SkullBlockRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Model, texture and render type of the Simple Mobheads looks. Vanilla's {@code createModel} only knows its own skull types;
 * block renderer, item model ({@code minecraft:head}) and worn head ({@code CustomHeadLayer}) all take their model there and
 * draw through {@code submitSkull}, where the look's extra passes follow. A placed head with a variant switches to the
 * variant's skull type in {@code extractRenderState}.
 */
@Mixin(SkullBlockRenderer.class)
public abstract class SkullBlockRendererMixin {
    @Shadow
    @Final
    private static Map<SkullBlock.Type, Identifier> SKIN_BY_TYPE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void simplemobheads$skins(CallbackInfo ci) {
        for (MobHeadType type : MobHeads.types()) {
            SKIN_BY_TYPE.put(type, MobHeadModels.texture(type));
        }
        // the extra head slot of a super mod draws worn heads like the helmet slot does (variants, mob head size)
        com.simplebuilding.framework.api.WornHeads.registerLook(com.simplemobheads.SimpleMobHeads.MOD_ID, new com.simplemobheads.client.MobHeadLook());
    }

    @Inject(method = "createModel", at = @At("HEAD"), cancellable = true)
    private static void simplemobheads$model(EntityModelSet models, SkullBlock.Type type, CallbackInfoReturnable<SkullModelBase> cir) {
        if (type instanceof MobHeadType head) {
            cir.setReturnValue(MobHeadModels.create(models, head));
        }
    }

    @Inject(method = "getSkullRenderType", at = @At("HEAD"), cancellable = true)
    private static void simplemobheads$renderType(SkullBlock.Type type, @Nullable Identifier texture, CallbackInfoReturnable<RenderType> cir) {
        if (type instanceof MobHeadType head) {
            cir.setReturnValue(MobHeadModels.renderType(head, texture));
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/SkullBlockEntity;Lnet/minecraft/client/renderer/blockentity/state/SkullBlockRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At("TAIL"))
    private void simplemobheads$variant(SkullBlockEntity entity, SkullBlockRenderState state, float partialTicks, Vec3 camera,
                                        ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
        if (state.skullType instanceof MobHeadType head) {
            String variant = entity.components().get(MobHeads.VARIANT);
            if (variant != null) {
                MobHeadType shown = MobHeads.type(head.head(), variant);
                state.skullType = shown;
                state.renderType = SkullBlockRenderer.getSkullRenderType(shown, null);
            }
            // EH21 giant heads: on a wall, the back of the big head touches the wall (Vanilla's offset fits 8 px heads only).
            float[] wall = state.skullType instanceof MobHeadType shown ? MobHeadModels.wall(shown) : null;
            BlockState block = entity.getBlockState();
            if (wall != null && block.getBlock() instanceof WallSkullBlock) {
                Direction facing = block.getValue(WallSkullBlock.FACING);
                state.transformation = new Transformation(
                        new Vector3f(0.5F - facing.getStepX() * wall[0], wall[1], 0.5F - facing.getStepZ() * wall[0]),
                        Axis.YP.rotationDegrees(-facing.getOpposite().toYRot()), new Vector3f(-1.0F, -1.0F, 1.0F), null);
            }
        }
    }

    @Inject(method = "submitSkull", at = @At("TAIL"))
    private static void simplemobheads$passes(float animation, PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                                              SkullModelBase model, RenderType renderType, int outlineColor,
                                              ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo ci) {
        if (!(model instanceof MobHeadModel head) || head.passes().isEmpty()) {
            return;
        }
        SkullModelBase.State state = new SkullModelBase.State();
        state.animationPos = animation;
        for (MobHeadModel.Pass pass : head.passes()) {
            collector.order(1).submitModel(pass.model(), state, poseStack, pass.renderType(), lightCoords, OverlayTexture.NO_OVERLAY,
                    pass.tint(), null, outlineColor);
        }
    }
}
