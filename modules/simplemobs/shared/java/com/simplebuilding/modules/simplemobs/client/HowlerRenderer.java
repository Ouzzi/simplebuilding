package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.HowlerEntity;
import com.simplebuilding.modules.simplemobs.HowlerLogic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class HowlerRenderer extends MobRenderer<HowlerEntity, HowlerRenderState, HowlerModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler_glow.png");
    private static final Identifier ANTENNAE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/hollow_howler/hollow_howler_antennae.png");

    public HowlerRenderer(EntityRendererProvider.Context context) {
        super(context, new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), 0.8F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW, (state, age) -> 0.75F + 0.25F * (float) Math.sin(age * 0.12F),
                new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), RenderTypes::eyes, false));
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> ANTENNAE, (state, age) -> state.tendril,
                new HowlerModel(context.bakeLayer(HowlerModel.LAYER)), RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(HowlerRenderState state) { return TEXTURE; }

    @Override
    public HowlerRenderState createRenderState() { return new HowlerRenderState(); }

    @Override
    public void extractRenderState(HowlerEntity entity, HowlerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        int bite = entity.biteTicks(), shriek = entity.shriekTicks(), emerge = entity.emergeTicks();
        float open = entity.phase() == HowlerLogic.Phase.ONE ? 0.0f : 0.35f;
        if (bite > 0) open = Math.max(open, Math.min(1.0f, (HowlerLogic.BITE_WINDUP_TICKS - bite + partialTicks) / 4.0f));
        state.shriek = shriek > 0 ? Math.min(1.0f, shriek / 6.0f) : 0.0f;
        state.jawOpen = Math.max(open, state.shriek);
        state.tendril = entity.tendrilAnimation(partialTicks);
        state.emerge = emerge > 0 ? 1.0f - Math.max(0.0f, emerge - partialTicks) / HowlerLogic.EMERGE_TICKS : 1.0f;
    }
}
