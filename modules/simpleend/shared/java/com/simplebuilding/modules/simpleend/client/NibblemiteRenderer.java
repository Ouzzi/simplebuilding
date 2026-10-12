package com.simplebuilding.modules.simpleend.client;

import com.simplebuilding.modules.simpleend.entity.Nibblemite;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Nibblemite in the colours of its biome (Ash Valley pale grey with lavender, Dawnlands taupe with rose). */
public class NibblemiteRenderer extends MobRenderer<Nibblemite, NibblemiteRenderer.State, NibblemiteModel> {
    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/ash.png"),
            Identifier.fromNamespaceAndPath("simpleend", "textures/entity/nibblemite/dusk.png")};

    public static class State extends LivingEntityRenderState {
        public int variant;
    }

    public NibblemiteRenderer(EntityRendererProvider.Context context) {
        super(context, new NibblemiteModel(context.bakeLayer(NibblemiteModel.LAYER)), 0.25F);
    }

    @Override
    protected float getFlipDegrees() { return 180.0F; }

    @Override
    public Identifier getTextureLocation(State state) { return TEXTURES[state.variant == 1 ? 1 : 0]; }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Nibblemite entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = entity.variant();
    }
}
