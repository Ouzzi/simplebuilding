package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.BossLogic;
import com.simplebuilding.modules.simplemobs.BrimguardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class BrimguardRenderer extends MobRenderer<BrimguardEntity, BrimguardRenderState, BrimguardModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/brimguard/brimguard.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/brimguard/brimguard_glow.png");

    public BrimguardRenderer(EntityRendererProvider.Context context) {
        // the brim throws a wide shadow
        super(context, new BrimguardModel(context.bakeLayer(BrimguardModel.LAYER)), 1.1F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW, (state, age) -> 0.9F,
                new BrimguardModel(context.bakeLayer(BrimguardModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(BrimguardRenderState state) { return TEXTURE; }

    @Override
    public BrimguardRenderState createRenderState() { return new BrimguardRenderState(); }

    @Override
    public void extractRenderState(BrimguardEntity entity, BrimguardRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.duck = entity.ducking() ? 1.0F : 0.0F;
        int vent = entity.ventTicks();
        // ease in over the first 5 ticks and out over the last 5
        state.vent = vent <= 0 ? 0.0F : Math.min(1.0F, Math.min(vent, BossLogic.VENT_TICKS - vent + 1) / 5.0F);
        int act = entity.action();
        state.slam = act == BrimguardEntity.ACT_SLAM ? Math.min(1.0F, (entity.clientActionAge() + partialTicks) / BossLogic.SLAM_WINDUP) : 0.0F;
        state.charge = act == BrimguardEntity.ACT_CHARGE_WINDUP || act == BrimguardEntity.ACT_CHARGE ? 1.0F : 0.0F;
    }
}
