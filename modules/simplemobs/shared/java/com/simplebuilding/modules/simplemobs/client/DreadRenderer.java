package com.simplebuilding.modules.simplemobs.client;

import com.simplebuilding.modules.simplemobs.DreadEntity;
import com.simplebuilding.modules.simplemobs.FearLogic;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public class DreadRenderer extends MobRenderer<DreadEntity, DreadRenderState, DreadModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/dread/dread.png");
    private static final Identifier GLOW = Identifier.fromNamespaceAndPath("simplemobs", "textures/entity/dread/dread_glow.png");

    public DreadRenderer(EntityRendererProvider.Context context) {
        super(context, new DreadModel(context.bakeLayer(DreadModel.LAYER)), 0.9F);
        // soul flames behind the visor flicker; in bright light they burn low
        this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> GLOW,
                (state, age) -> (state.weak ? 0.35F : 0.8F) + 0.2F * (float) Math.sin(age * 0.3F) * (float) Math.sin(age * 0.11F),
                new DreadModel(context.bakeLayer(DreadModel.LAYER)), RenderTypes::eyes, false));
    }

    @Override
    public Identifier getTextureLocation(DreadRenderState state) { return TEXTURE; }

    @Override
    public DreadRenderState createRenderState() { return new DreadRenderState(); }

    @Override
    public void extractRenderState(DreadEntity entity, DreadRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        float age = entity.clientActionAge() + partialTicks;
        int action = entity.action();
        state.swing = action == DreadEntity.ACT_SWING ? Math.min(1.0F, age / (FearLogic.SWING_WINDUP - 2)) : 0.0F;
        state.stomp = action == DreadEntity.ACT_STOMP ? Math.min(1.0F, age / (FearLogic.STOMP_WINDUP - 4)) : 0.0F;
        int rise = entity.riseTicks();
        state.rise = rise > 0 ? 1.0F - Math.max(0.0F, rise - partialTicks) / FearLogic.RISE_TICKS : 1.0F;
        state.weak = entity.weakened();
    }
}
