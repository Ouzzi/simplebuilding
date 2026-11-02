package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.BossLogic;
import com.simplebuilding.modules.simplemobs.VoidRunnerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class VoidRunnerRenderer extends MobRenderer<VoidRunnerEntity, VoidRunnerRenderState, VoidRunnerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/void_runner/void_runner.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/void_runner/void_runner_glow.png");

    public VoidRunnerRenderer(EntityRendererProvider.Context context) {
        super(context, new VoidRunnerModel(context.bakeLayer(VoidRunnerModel.LAYER)), 0.4F);
        // the eye slit glows, brighter in rage
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW, (state, age) -> 0.85F + 0.15F * (float) Math.sin(age * 0.25F),
                new VoidRunnerModel(context.bakeLayer(VoidRunnerModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(VoidRunnerRenderState state) { return TEXTURE; }

    @Override
    public VoidRunnerRenderState createRenderState() { return new VoidRunnerRenderState(); }

    @Override
    public void extractRenderState(VoidRunnerEntity entity, VoidRunnerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.strike = entity.clientStrike() > 0 ? Math.max(0.0F, (entity.clientStrike() - partialTicks) / 8.0F) : 0.0F;
        int act = entity.action();
        state.crouch = act == VoidRunnerEntity.ACT_WINDUP ? Math.min(1.0F, (entity.clientActionAge() + partialTicks) / BossLogic.LUNGE_WINDUP) : 0.0F;
        state.lunge = act == VoidRunnerEntity.ACT_LUNGE ? 1.0F : 0.0F;
        state.stagger = act == VoidRunnerEntity.ACT_STAGGER ? 1.0F : 0.0F;
    }
}
