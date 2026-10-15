package com.simplefun.mixin.client;

import com.simplefun.client.PiggyStateExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Same principle as simplemobheads' worn mob heads: while piggy, the pig head replaces the player's head
 * instead of sitting over it. Only the cubes are hidden ({@code skipDraw}); the head pose still places the
 * pig head. The value also includes the mob-head rule, so it is the same whichever mixin runs last.
 */
@Mixin(HumanoidModel.class)
public abstract class WornPigHeadMixin {
  @Shadow @Final public ModelPart head;
  @Shadow @Final public ModelPart hat;

  /** 0 = not measured, 1 = own model, 2 = armor model. */
  @Unique private int simplefun$kind;

  @Inject(
      method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V",
      at = @At("TAIL"))
  private void simplefun$replaceHead(HumanoidRenderState state, CallbackInfo ci) {
    boolean piggy =
        state instanceof PiggyStateExtension p
            && p.simplefun$isPiggy()
            && !state.isInvisible
            && simplefun$isOwnModel();
    boolean modHead = state.wornHeadType != null && !(state.wornHeadType instanceof SkullBlock.Types);
    this.head.skipDraw = piggy || modHead;
    this.hat.skipDraw = piggy || modHead;
  }

  /**
   * Only the wearer's own model: the armor layer runs the same {@code setupAnim} on armor models, whose
   * head must stay so a helmet over the pig head remains visible. The armor head cube is inflated
   * (10 px), the player's is 8 px; measured once per model.
   */
  @Unique
  private boolean simplefun$isOwnModel() {
    if (this.simplefun$kind == 0) {
      float[] width = {0.0F};
      this.head.visit(
          new PoseStack(),
          (pose, path, index, cube) -> {
            if (path.isEmpty()) {
              width[0] = Math.max(width[0], cube.maxX - cube.minX);
            }
          });
      this.simplefun$kind = width[0] > 9.0F ? 2 : 1;
    }
    return this.simplefun$kind == 1;
  }
}
