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
        /** Per-mob phase offset (radians) from the entity id, so neighbors do not move in step. */
        public float phase;
        /** 0 not chewing, else 0..1 progress of the chew cycle. */
        public float chew;
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
        state.chew = entity.chewProgress(partialTicks);
        state.phase = (entity.getId() * 1.618F) % ((float) Math.PI * 2.0F);
    }
}
